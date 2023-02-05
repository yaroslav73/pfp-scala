package resources

import cats.syntax.all._
import cats.effect.std.Console
import cats.effect.{ Concurrent, Resource }
import config.Types.{ AppConfig, PostgreSQLConfig, RedisConfig }
import dev.profunktor.redis4cats.effect.{ MkRedis => MakeRedis }
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk._
import skunk.codec.text._
import skunk.implicits._
sealed abstract class AppResources[F[_]](
  val client: Client[F],
  val postgres: Resource[F, Session[F]],
  val redis: RedisCommands[F, String, String],
)

object AppResources {
  def make[F[_]: Concurrent: Console: Logger: MakeHttpClient: MakeRedis: Network](
    config: AppConfig
  ): Resource[F, AppResources[F]] = {
    def checkPostgresConnection(postgres: Resource[F, Session[F]]): F[Unit] =
      postgres.use { session =>
        session
          .unique(sql"SELECT version();".query(text))
          .flatMap(v => Logger[F].info(s"Connected to Postgres $v"))
      }

    def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
      redis.info.flatMap { info =>
        info.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to Redis $v")
        }
      }

    def makePostgreSQLResource(c: PostgreSQLConfig): SessionPool[F] =
      Session
        .pooled(
          host     = c.host.value,
          port     = c.port.value,
          user     = c.user.value,
          database = c.database.value,
          password = c.password.value.value.some,
          max      = c.max.value
        )
        .evalTap(checkPostgresConnection)

    def makeRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.redisURI.value.value).evalTap(checkRedisConnection)

    (
      MakeHttpClient[F].newEmber(config.httpClientConfig),
      makePostgreSQLResource(config.postgreSQLConfig),
      makeRedisResource(config.redisConfig),
    ).parMapN { case (client, postgres, redis) => new AppResources[F](client, postgres, redis) {} }
  }
}
