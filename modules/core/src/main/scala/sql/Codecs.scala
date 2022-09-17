package sql

import domain.Brand.{ BrandId, BrandName }
import domain.Category.{ CategoryId, CategoryName }
import skunk._
import skunk.codec.all._

object Codecs {
  val brandId: Codec[BrandId]     = uuid.imap[BrandId](uuid => BrandId(uuid))(brandId => brandId.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](name => BrandName(name))(brandName => brandName.value)

  val categoryId: Codec[CategoryId] = uuid.imap[CategoryId](uuid => CategoryId(uuid))(categoryId => categoryId.value)
  val categoryName: Codec[CategoryName] =
    varchar.imap[CategoryName](name => CategoryName(name))(categoryName => categoryName.value)
}
