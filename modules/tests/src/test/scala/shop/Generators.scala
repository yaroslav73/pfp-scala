package shop

import domain.Auth.{ UserId, UserName }
import domain.Brand.{ BrandId, BrandName }
import domain.Card.{ CVV, CardHolder, CardNumber, Expiration }
import domain.Cart.{ CartItem, CartTotal, Quantity }
import domain.Category.{ CategoryId, CategoryName }
import domain.Item.{ ItemDescription, ItemId, ItemName }
import domain.Order.{ OrderId, PaymentId }
import domain.{ Brand, Card, Cart, Category, Item }
import eu.timepit.refined.api.Refined
import http.auth.User
import http.auth.User.CommonUser
import org.scalacheck.Gen
import squants.market.{ Money, USD }

import java.util.UUID

object Generators {
  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 50)
      .flatMap(n => Gen.buildableOfN[String, Char](n, Gen.alphaChar))

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val moneyGen: Gen[Money] =
    Gen.posNum[Long].map { value =>
      USD(BigDecimal(value))
    }

  val paymentIdGen: Gen[PaymentId] =
    idGen(PaymentId.apply)

  val orderIdGen: Gen[OrderId] =
    idGen(OrderId.apply)

  val brandIdGen: Gen[BrandId] =
    idGen(BrandId.apply)

  val brandNameGen: Gen[BrandName] =
    nesGen(BrandName.apply)

  val brandGen: Gen[Brand] =
    for {
      uuid <- brandIdGen
      name <- brandNameGen
    } yield Brand(uuid, name)

  val categoryIdGen: Gen[CategoryId] =
    idGen(CategoryId.apply)

  val categoryNameGen: Gen[CategoryName] =
    nesGen(CategoryName.apply)

  val categoryGen: Gen[Category] =
    for {
      uuid <- categoryIdGen
      name <- categoryNameGen
    } yield Category(uuid, name)

  val itemIdGen: Gen[ItemId] =
    idGen(ItemId.apply)

  val itemName: Gen[ItemName] =
    nesGen(ItemName.apply)

  val itemDescriptionGen: Gen[ItemDescription] =
    nesGen(ItemDescription.apply)

  val itemGen: Gen[Item] =
    for {
      uuid        <- itemIdGen
      name        <- itemName
      description <- itemDescriptionGen
      price       <- moneyGen
      brand       <- brandGen
      category    <- categoryGen
    } yield Item(uuid, name, description, price, brand, category)

  val quantityGen: Gen[Quantity] =
    Gen.posNum[Int].map(Quantity.apply)

  val cartItemGen: Gen[CartItem] =
    for {
      item     <- itemGen
      quantity <- quantityGen
    } yield CartItem(item, quantity)

  val cartTotalGen: Gen[CartTotal] =
    for {
      items <- Gen.nonEmptyListOf(cartItemGen)
      total <- moneyGen
    } yield CartTotal(items, total)

  val itemMapGen: Gen[(ItemId, Quantity)] =
    for {
      itemId   <- itemIdGen
      quantity <- quantityGen
    } yield itemId -> quantity

  val cartGen: Gen[Cart] =
    Gen.nonEmptyMap(itemMapGen).map(Cart.apply)

  val cardHolderGen: Gen[CardHolder] =
    Gen.stringOf(Gen.oneOf(('a' to 'z') ++ ('A' to 'Z'))).map { name =>
      CardHolder(Refined.unsafeApply(name))
    }

  private def sized(size: Int): Gen[Long] = {
    def loop(s: Int, acc: String): Gen[Long] =
      Gen.oneOf(1 to 9).flatMap { n =>
        if (s == size) acc.toLong
        else loop(s + 1, acc + n.toString)
      }

    loop(0, "")
  }

  val cardGen: Gen[Card] =
    for {
      holder <- cardHolderGen
      number <- sized(16).map(n => CardNumber(Refined.unsafeApply(n)))
      expire <- sized(4).map(n => Expiration(Refined.unsafeApply(n.toString)))
      cvv    <- sized(3).map(n => CVV(Refined.unsafeApply(n.toInt)))
    } yield Card(holder, number, expire, cvv)

  val userIdGen: Gen[UserId] =
    idGen(uuid => UserId(uuid))

  val userNameGen: Gen[UserName] =
    nesGen(name => UserName(name))

  val userGen: Gen[User] =
    for {
      id   <- userIdGen
      name <- userNameGen
    } yield User(id, name)

  val commonUserGen: Gen[CommonUser] = userGen.map(CommonUser)
}
