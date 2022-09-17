package sql

import domain.Brand.{ BrandId, BrandName }
import domain.Category.{ CategoryId, CategoryName }
import domain.Item.{ ItemDescription, ItemId, ItemName }
import skunk._
import skunk.codec.all._
import squants.market.{ Money, USD }

object Codecs {
  val brandId: Codec[BrandId]     = uuid.imap[BrandId](uuid => BrandId(uuid))(brandId => brandId.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](name => BrandName(name))(brandName => brandName.value)

  val categoryId: Codec[CategoryId]     = uuid.imap[CategoryId](uuid => CategoryId(uuid))(categoryId => categoryId.value)
  val categoryName: Codec[CategoryName] = varchar.imap[CategoryName](CategoryName)(_.value)

  val itemId: Codec[ItemId]            = uuid.imap[ItemId](uuid => ItemId(uuid))(itemId => itemId.value)
  val itemName: Codec[ItemName]        = varchar.imap[ItemName](name => ItemName(name))(itemName => itemName.value)
  val itemDesc: Codec[ItemDescription] = varchar.imap[ItemDescription](ItemDescription)(_.value)

  val money: Codec[Money] = numeric.imap[Money](price => Money(price, USD))(money => money.amount)
}
