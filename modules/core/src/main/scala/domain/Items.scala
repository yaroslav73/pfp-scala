package domain

import domain.Brands.{ Brand, BrandId, BrandName }
import domain.Categories.{ Category, CategoryId }
import domain.Items.{ CreateItem, Item, ItemId, UpdateItem }
import io.estatico.newtype.macros.newtype
import squants.market.Money

import java.util.UUID

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}

object Items {
  @newtype case class ItemId(value: UUID)
  @newtype case class ItemName(value: String)
  @newtype case class ItemDescription(value: String)

  case class Item(
    uuid: ItemId,
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brand: Brand,
    category: Category
  )

  case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brandId: BrandId,
    categoryId: CategoryId
  )

  case class UpdateItem(itemId: ItemId, price: Money)
}
