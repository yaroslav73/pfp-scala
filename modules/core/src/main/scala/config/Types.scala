package config

import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

object Types {
  final case class TokenExpiration(value: FiniteDuration)

  final case class JwtAccessTokenKeyConfig(secret: NonEmptyString)
  final case class PasswordSalt(secret: NonEmptyString)
}
