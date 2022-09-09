package domain

import domain.Brands.{Brand, BrandId}
import domain.Categories.{Category, CategoryId}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
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

  case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brandId: BrandId,
    categoryId: CategoryId
  )

  case class UpdateItem(itemId: ItemId, price: Money)
}
