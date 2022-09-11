package domain

import domain.Brands.{ Brand, BrandId }
import domain.Categories.{ Category, CategoryId }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import squants.market._

import java.util.UUID

object Items {
  case class ItemId(value: UUID)
  case class ItemName(value: String)
  case class ItemDescription(value: String)

  case class Item(
    uuid: ItemId,
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brand: Brand,
    category: Category
  )
  object Item {
    implicit val itemDescriptionEncoder: Encoder[ItemDescription] = deriveEncoder[ItemDescription]
    implicit val itemNameEncoder: Encoder[ItemName]               = deriveEncoder[ItemName]
    implicit val itemIdEncoder: Encoder[ItemId]                   = deriveEncoder[ItemId]
    implicit val itemEncoder: Encoder[Item]                       = deriveEncoder[Item]
  }

  final case class ItemNameParam(value: NonEmptyString)
  object ItemNameParam {
    implicit val jsonEncoder: Encoder[ItemNameParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[ItemNameParam] =
      Decoder.forProduct1("name")(ItemNameParam.apply)
  }

  final case class ItemDescriptionPram(value: NonEmptyString)
  object ItemDescriptionPram {
    implicit val jsonEncoder: Encoder[ItemDescriptionPram] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[ItemDescriptionPram] =
      Decoder.forProduct1("name")(ItemDescriptionPram.apply)
  }

  final case class PriceParam(value: String Refined ValidBigDecimal)
  object PriceParam {
    implicit val jsonEncoder: Encoder[PriceParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[PriceParam] =
      Decoder.forProduct1("name")(PriceParam.apply)
  }

  final case class CreateItemParam(
    name: ItemNameParam,
    description: ItemDescriptionPram,
    price: PriceParam,
    brandId: BrandId,
    categoryId: CategoryId,
  ) {
    def toDomain: CreateItem =
      CreateItem(
        ItemName(name.value.value),
        ItemDescription(description.value.value),
        USD(BigDecimal(price.value.value)),
        brandId,
        categoryId
      )
  }

  final case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brandId: BrandId,
    categoryId: CategoryId
  )

  final case class ItemIdParam(value: String Refined Uuid)
  object ItemIdParam {
    implicit val jsonEncoder: Encoder[ItemIdParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[ItemIdParam] =
      Decoder.forProduct1("name")(ItemIdParam.apply)
  }

  final case class UpdateItemParam(id: ItemIdParam, price: PriceParam) {
    def toDomain: UpdateItem = UpdateItem(ItemId(UUID.fromString(id.value.value)), USD(BigDecimal(price.value.value)))
  }

  final case class UpdateItem(itemId: ItemId, price: Money)
}
