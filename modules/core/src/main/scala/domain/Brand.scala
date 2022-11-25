package domain

import cats.{ Eq, Show }
import domain.Brand.{ BrandId, BrandName }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.circe.{ Decoder, Encoder }
import monocle.Iso
import optics.IsUUID

import java.util.UUID

final case class Brand(uuid: BrandId, name: BrandName)
object Brand {
  case class BrandId(value: UUID)
  case class BrandName(value: String)

  case class BrandParam(value: NonEmptyString) {
    def toDomain: BrandName = BrandName(value.value.toLowerCase.capitalize)
  }

  object BrandParam {
    implicit val jsonEncoder: Encoder[BrandParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[BrandParam] =
      Decoder.forProduct1("name")(BrandParam.apply)
  }

  implicit val isBrandId: IsUUID[BrandId] = new IsUUID[BrandId] {
    def _UUID: Iso[UUID, BrandId] = Iso[UUID, BrandId](uuid => BrandId(uuid))(brandId => brandId.value)
  }

  implicit val brandIdEq: Eq[BrandId] = new Eq[BrandId] {
    override def eqv(x: BrandId, y: BrandId): Boolean =
      x.value.equals(y.value)
  }

  implicit val brandShow: Show[Brand]         = (brand: Brand) => brand.toString
  implicit val brandIdShow: Show[BrandId]     = (brandId: BrandId) => brandId.toString
  implicit val brandNameShow: Show[BrandName] = (brandName: BrandName) => brandName.toString

  implicit val brandNameEncoder: Encoder[BrandName] = deriveEncoder[BrandName]
  implicit val brandNameDecoder: Decoder[BrandName] = deriveDecoder[BrandName]
  implicit val brandIdEncoder: Encoder[BrandId]     = deriveEncoder[BrandId]
  implicit val brandIdDecoder: Decoder[BrandId]     = deriveDecoder[BrandId]
  implicit val brandEncoder: Encoder[Brand]         = deriveEncoder[Brand]
  implicit val brandDecoder: Decoder[Brand]         = deriveDecoder[Brand]
}
