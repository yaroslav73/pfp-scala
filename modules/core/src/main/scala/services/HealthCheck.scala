package services

import cats.effect.Temporal
import cats.effect.implicits.{ genTemporalOps, parallelForGenSpawn }
import cats.effect.kernel.Resource
import cats.implicits.{ catsSyntaxApplicativeError, catsSyntaxApplicativeId, catsSyntaxTuple2Parallel, toFunctorOps }
import dev.profunktor.redis4cats.RedisCommands
import domain.HealthCheck.{ AppStatus, PostgresStatus, RedisStatus, Status }
import skunk._
import skunk.codec.all.int4
import skunk.implicits._

import scala.concurrent.duration.DurationInt

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object HealthCheck {
  def make[F[_]: Temporal](
    postgres: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
  ): HealthCheck[F] = new HealthCheck[F] {
    val query: Query[Void, Int] = sql"SELECT pid FROM pg_stat_activity".query(int4)

    val redisHealth: F[RedisStatus] =
      redis.ping
        .map(_.nonEmpty)
        .map(Status._bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(RedisStatus.apply)

    val postgresHealth: F[PostgresStatus] =
      postgres
        .use(_.execute(query))
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status._bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(PostgresStatus.apply)

    def status: F[AppStatus] =
      (redisHealth, postgresHealth).parMapN(AppStatus.apply)
  }
}
