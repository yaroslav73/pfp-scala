package config

import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

object Types {
  final case class TokenExpiration(value: FiniteDuration)

  case class JwtSecretKeyConfig(secret: NonEmptyString)
  final case class JwtAccessTokenKeyConfig(secret: NonEmptyString)
  final case class PasswordSalt(secret: NonEmptyString)

  final case class PaymentURI(value: NonEmptyString)
  final case class PaymentConfig(uri: PaymentURI)
}
