package domain

import domain.Auth.UserId
import domain.Items.{Item, ItemId}
import domain.ShoppingCart.{Cart, CartTotal, Quantity}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json, KeyDecoder, KeyEncoder}
import squants.market.Money

import java.util.UUID
import scala.util.Try

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {
  case class Quantity(value: Int)
  case class Cart(items: Map[ItemId, Quantity])

  case class CartItem(item: Item, quantity: Quantity)
  case class CartTotal(items: List[CartItem], total: Money)

  implicit val quantityEncoder: Encoder[Quantity] = deriveEncoder[Quantity]
  implicit val quantityDecoder: Decoder[Quantity] = deriveDecoder[Quantity]
  implicit val cartEncoder: Encoder[Cart]         = deriveEncoder[Cart]
  implicit val cartDecoder: Decoder[Cart]         = deriveDecoder[Cart]
  implicit val itemIdKeyEncoder: KeyEncoder[ItemId] = new KeyEncoder[ItemId] {
    override def apply(key: ItemId): String = key.value.toString
  }
  implicit val itemIdKeyDecoder: KeyDecoder[ItemId] = new KeyDecoder[ItemId] {
    override def apply(key: String): Option[ItemId] = Try(UUID.fromString(key)).map(ItemId).toOption
  }
  implicit val cartItemDecoder: Encoder[CartItem]   = deriveEncoder[CartItem]
  implicit val cartTotalDecoder: Encoder[CartTotal] = deriveEncoder[CartTotal]
  implicit val moneyEncoder: Encoder[Money] = new Encoder[Money] {
    override def apply(a: Money): Json =
      Json.obj(
        "value" -> Json.fromBigDecimal(a.amount),
        "currency" -> Json.fromString(a.currency.code),
      )
  }
}
