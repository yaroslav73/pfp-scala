package domain

import domain.Brands.{ Brand, BrandId, BrandName }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.util.UUID

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[BrandId]
}

object Brands {
  case class BrandId(value: UUID)
  case class BrandName(value: String)

  final case class Brand(uuid: BrandId, name: BrandName)
  object Brand {
    implicit val brandNameEncoder: Encoder[BrandName] = deriveEncoder[BrandName]
    implicit val brandIdEncoder: Encoder[BrandId]     = deriveEncoder[BrandId]
    implicit val brandEncoder: Encoder[Brand]         = deriveEncoder[Brand]
  }
}
