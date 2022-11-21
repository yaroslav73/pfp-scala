package domain

import cats.Show
import domain.Brand.BrandId
import domain.Cart.{ CartItem, Quantity }
import domain.Category.CategoryId
import domain.Item.{ ItemDescription, ItemId, ItemName }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined.{ refinedDecoder, refinedEncoder }
import io.circe.{ Decoder, Encoder }
import monocle.Iso
import optics.IsUUID
import squants.market._

import java.util.UUID

case class Item(
  uuid: ItemId,
  name: ItemName,
  description: ItemDescription,
  price: Money,
  brand: Brand,
  category: Category
) {
  def cart(quantity: Quantity): CartItem =
    CartItem(this, quantity)
}
object Item {
  final case class ItemId(value: UUID)

  object ItemId {
    implicit val showItemId: Show[ItemId] = (itemId: ItemId) => itemId.toString
    implicit val isItemId: IsUUID[ItemId] = new IsUUID[ItemId] {
      def _UUID: Iso[UUID, ItemId] = Iso[UUID, ItemId](uuid => ItemId(uuid))(itemId => itemId.value)
    }
  }

  case class ItemName(value: String)

  case class ItemDescription(value: String)

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

  implicit val itemShow: Show[Item] = (item: Item) => item.toString

  implicit val itemDescriptionEncoder: Encoder[ItemDescription] = deriveEncoder[ItemDescription]
  implicit val itemDescriptionDecoder: Decoder[ItemDescription] = deriveDecoder[ItemDescription]
  implicit val itemNameEncoder: Encoder[ItemName]               = deriveEncoder[ItemName]
  implicit val itemNameDecoder: Decoder[ItemName]               = deriveDecoder[ItemName]
  implicit val itemIdEncoder: Encoder[ItemId]                   = deriveEncoder[ItemId]
  implicit val itemIdDecoder: Decoder[ItemId]                   = deriveDecoder[ItemId]
  implicit val itemEncoder: Encoder[Item]                       = deriveEncoder[Item]
  implicit val itemDecoder: Decoder[Item]                       = deriveDecoder[Item]
}
