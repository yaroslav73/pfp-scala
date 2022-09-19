package config

import scala.concurrent.duration.FiniteDuration

object Types {
  final case class TokenExpiration(value: FiniteDuration)
}
