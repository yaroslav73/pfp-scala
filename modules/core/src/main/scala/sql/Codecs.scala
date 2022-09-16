package sql

import domain.Brands.{ BrandId, BrandName }
import skunk._
import skunk.codec.all._

object Codecs {
  val brandId: Codec[BrandId]     = uuid.imap[BrandId](uuid => BrandId(uuid))(brandId => brandId.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](name => BrandName(name))(brandName => brandName.value)
}
