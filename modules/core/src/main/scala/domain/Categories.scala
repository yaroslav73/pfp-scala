package domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.circe.{ Decoder, Encoder }

import java.util.UUID

object Categories {
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

  final case class Category(uuid: CategoryId, name: CategoryName)
  object Category {
    implicit val categoryNameEncoder: Encoder[CategoryName] = deriveEncoder[CategoryName]
    implicit val categoryIdEncoder: Encoder[CategoryId]     = deriveEncoder[CategoryId]
    implicit val categoryEncoder: Encoder[Category]         = deriveEncoder[Category]
  }
}
