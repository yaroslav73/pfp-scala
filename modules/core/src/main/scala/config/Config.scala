package config

import ciris.{ConfigValue, Secret, env}
import config.Types.{AdminJwtConfig, AdminUserTokenConfig, AppConfig, CheckoutConfig, HttpClientConfig, HttpServerConfig, JwtAccessTokenKeyConfig, JwtClaimConfig, JwtSecretKeyConfig, PasswordSalt, PaymentConfig, PaymentURI, PostgreSQLConfig, RedisConfig, TokenExpiration}
import dev.profunktor.redis4cats.connection.RedisURI
import cats.syntax.all._
import ciris._
import ciris.refined._
import com.comcast.ip4s._
import domain.Cart.ShoppingCartExpiration
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.DurationInt

object Config {
  private def default[F[_]](redisURI: RedisURI, paymentURI: PaymentURI): ConfigValue[F, AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[JwtSecretKeyConfig].secret,
      env("SC_JWT_CLAIM").as[JwtClaimConfig].secret,
      env("SC_ACCESS_TOKEN_SECRET_KEY").as[JwtAccessTokenKeyConfig].secret,
      env("SC_ADMIN_USER_TOKEN").as[AdminUserTokenConfig].secret,
      env("SC_PASSWORD_SALT").as[PasswordSalt].secret,
      env("SC_POSTGRES_PASSWORD").as[NonEmptyString].secret,
    ).parMapN { (jwtSecretKey, jwtClaim, accessTokenSecretKey, adminUserToken, passwordSalt, postgresPassword) =>
      AppConfig(
        AdminJwtConfig(jwtSecretKey, jwtClaim, adminUserToken),
        accessTokenSecretKey,
        passwordSalt,
        TokenExpiration(30.minutes),
        ShoppingCartExpiration(30.minutes),
        CheckoutConfig(retriesLimit = 3, retriesBackoff = 10.milliseconds),
        PaymentConfig(paymentURI),
        HttpClientConfig(timeout = 60.seconds, idleTimeInPool = 30.seconds),
        PostgreSQLConfig(
          host     = "localhost",
          port     = 5432,
          user     = "postgres",
          password = postgresPassword,
          database = "store",
          max      = 10,
        ),
        RedisConfig(redisURI),
        HttpServerConfig(host = host"0.0.0.0", port = port"8080")
      )
    }
}
