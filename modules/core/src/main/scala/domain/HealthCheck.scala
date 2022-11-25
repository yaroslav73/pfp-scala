package domain

import cats.Eq
import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import monocle.Iso

object HealthCheck {
  @derive(encoder)
  @newtype
  case class RedisStatus(value: Status)

  @derive(encoder)
  @newtype
  case class PostgresStatus(value: Status)

  @derive(encoder)
  case class AppStatus(redis: RedisStatus, postgres: PostgresStatus)

  sealed trait Status
  object Status {
    case object Okay extends Status
    case object Unreachable extends Status

    val _bool: Iso[Status, Boolean] = Iso[Status, Boolean] {
      case Okay        => true
      case Unreachable => false
    }(status => if (status) Okay else Unreachable)

    implicit val eqStatus: Eq[Status] = new Eq[Status] {
      override def eqv(x: Status, y: Status): Boolean = x == y
    }

    implicit val jsonEncoder: Encoder[Status] =
      Encoder.forProduct1("status")(_.toString)
  }
}
