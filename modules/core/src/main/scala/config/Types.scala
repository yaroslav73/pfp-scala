package config

import cats.Show
import ciris.refined.refTypeConfigDecoder
import ciris.{ ConfigDecoder, ConfigError, ConfigKey, Secret }
import com.comcast.ip4s.{ Host, Port }
import dev.profunktor.redis4cats.connection.RedisURI
import domain.Cart.ShoppingCartExpiration
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

object Types {
  final case class TokenExpiration(value: FiniteDuration)

  case class JwtSecretKeyConfig(secret: NonEmptyString)

  case class JwtClaimConfig(secret: NonEmptyString)

  final case class JwtAccessTokenKeyConfig(secret: NonEmptyString)
  final case class AdminUserTokenConfig(secret: NonEmptyString)

  final case class PasswordSalt(secret: NonEmptyString)

  final case class RedisURI(value: NonEmptyString)
  final case class PaymentURI(value: NonEmptyString)

  final case class PaymentConfig(uri: PaymentURI)

  final case class CheckoutConfig(retriesLimit: PosInt, retriesBackoff: FiniteDuration)

  final case class AdminJwtConfig(
    jwtSecretKey: Secret[JwtSecretKeyConfig],
    jwtClaim: Secret[JwtClaimConfig],
    adminUserToken: Secret[AdminUserTokenConfig]
  )

  final case class HttpClientConfig(timeout: FiniteDuration, idleTimeInPool: FiniteDuration)

  final case class PostgreSQLConfig(
    host: NonEmptyString,
    port: UserPortNumber,
    user: NonEmptyString,
    password: Secret[NonEmptyString],
    database: NonEmptyString,
    max: PosInt,
  )

  final case class RedisConfig(redisURI: RedisURI)

  final case class HttpServerConfig(host: Host, port: Port)

  final case class AppConfig(
    adminJwtConfig: AdminJwtConfig,
    jwtAccessTokenKeyConfig: Secret[JwtAccessTokenKeyConfig],
    passwordSalt: Secret[PasswordSalt],
    tokenExpiration: TokenExpiration,
    shoppingCartExpiration: ShoppingCartExpiration,
    checkoutConfig: CheckoutConfig,
    paymentConfig: PaymentConfig,
    httpClientConfig: HttpClientConfig,
    postgreSQLConfig: PostgreSQLConfig,
    redisConfig: RedisConfig,
    httpServerConfig: HttpServerConfig,
  )

  implicit val jwtSecretKeyConfigShow: Show[JwtSecretKeyConfig] =
    (jwtSecretKeyConfig: JwtSecretKeyConfig) => jwtSecretKeyConfig.toString
  implicit val jwtSecretKeyConfigDecoder: ConfigDecoder[String, JwtSecretKeyConfig] =
    ConfigDecoder[String, NonEmptyString].map(s => JwtSecretKeyConfig(s))

  implicit val jwtClaimConfigShow: Show[JwtClaimConfig] =
    (jwtClaimConfig: JwtClaimConfig) => jwtClaimConfig.toString
  implicit val jwtClaimConfigDecoder: ConfigDecoder[String, JwtClaimConfig] =
    ConfigDecoder[String, NonEmptyString].map(s => JwtClaimConfig(s))

  implicit val adminUserTokenConfigShow: Show[AdminUserTokenConfig] =
    (adminUserTokenConfig: AdminUserTokenConfig) => adminUserTokenConfig.toString
  implicit val adminUserTokenConfigDecoder: ConfigDecoder[String, AdminUserTokenConfig] =
    ConfigDecoder[String, NonEmptyString].map(s => AdminUserTokenConfig(s))

  implicit val jwtAccessTokenKeyConfigShow: Show[JwtAccessTokenKeyConfig] =
    (jwtAccessTokenKeyConfig: JwtAccessTokenKeyConfig) => jwtAccessTokenKeyConfig.toString
  implicit val jwtAccessTokenKeyConfigDecoder: ConfigDecoder[String, JwtAccessTokenKeyConfig] =
    ConfigDecoder[String, NonEmptyString].map(s => JwtAccessTokenKeyConfig(s))

  implicit val passwordSaltShow: Show[PasswordSalt] =
    (passwordSalt: PasswordSalt) => passwordSalt.toString
  implicit val passwordSaltDecoder: ConfigDecoder[String, PasswordSalt] =
    ConfigDecoder[String, NonEmptyString].map(s => PasswordSalt(s))
}
