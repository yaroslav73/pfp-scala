package sql

import domain.Auth.{ EncryptedPassword, UserId, UserName }
import domain.Brand.{ BrandId, BrandName }
import domain.Category.{ CategoryId, CategoryName }
import domain.Item.{ ItemDescription, ItemId, ItemName }
import domain.Order.{ OrderId, PaymentId }
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

  val orderId: Codec[OrderId] = uuid.imap[OrderId](uuid => OrderId(uuid))(orderId => orderId.uuid)

  val userId: Codec[UserId]     = uuid.imap[UserId](uuid => UserId(uuid))(userId => userId.value)
  val userName: Codec[UserName] = varchar.imap[UserName](name => UserName(name))(userName => userName.value)
  val encryptedPassword: Codec[EncryptedPassword] =
    varchar
      .imap[EncryptedPassword](password => EncryptedPassword(password))(encryptedPassword => encryptedPassword.value)

  val paymentId: Codec[PaymentId] = uuid.imap[PaymentId](uuid => PaymentId(uuid))(paymentId => paymentId.uuid)

  val money: Codec[Money] = numeric.imap[Money](price => Money(price, USD))(money => money.amount)
}
