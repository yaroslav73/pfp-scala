package domain

import cats.Show
import domain.Category.{ CategoryId, CategoryName }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.circe.{ Decoder, Encoder }
import monocle.Iso
import optics.IsUUID

import java.util.UUID

final case class Category(uuid: CategoryId, name: CategoryName)
object Category {
  case class CategoryId(value: UUID)

  case class CategoryName(value: String)

  case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(value.value.toLowerCase.capitalize)
  }

  object CategoryParam {
    implicit val jsonEncoder: Encoder[CategoryParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[CategoryParam] =
      Decoder.forProduct1("name")(CategoryParam.apply)
  }

  implicit val isCategoryId: IsUUID[CategoryId] = new IsUUID[CategoryId] {
    def _UUID: Iso[UUID, CategoryId] = Iso[UUID, CategoryId](uuid => CategoryId(uuid))(categoryId => categoryId.value)
  }

  implicit val categoryShow: Show[Category] = (category: Category) => category.toString

  implicit val categoryNameEncoder: Encoder[CategoryName] = deriveEncoder[CategoryName]
  implicit val categoryNameDecoder: Decoder[CategoryName] = deriveDecoder[CategoryName]
  implicit val categoryIdEncoder: Encoder[CategoryId]     = deriveEncoder[CategoryId]
  implicit val categoryIdDecoder: Decoder[CategoryId]     = deriveDecoder[CategoryId]
  implicit val categoryEncoder: Encoder[Category]         = deriveEncoder[Category]
  implicit val categoryDecoder: Decoder[Category]         = deriveDecoder[Category]
}
