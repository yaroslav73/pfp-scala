package domain

import domain.Auth.UserId
import domain.Cart.Quantity
import domain.Item.ItemId
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import squants.market.{ Money, USD }

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

final case class Cart(items: Map[ItemId, Quantity])
object Cart {
  final case class Quantity(value: Int)

  final case class CartItem(item: Item, quantity: Quantity) {
    def subTotal: Money = USD(item.price.amount * quantity.value)
  }

  final case class CartTotal(items: List[CartItem], total: Money)

  final case class ShoppingCartExpiration(value: FiniteDuration)

  final case class CartNotFound(userId: UserId) extends NoStackTrace

  implicit val quantityEncoder: Encoder[Quantity]   = deriveEncoder[Quantity]
  implicit val quantityDecoder: Decoder[Quantity]   = deriveDecoder[Quantity]
  implicit val cartEncoder: Encoder[Cart]           = deriveEncoder[Cart]
  implicit val cartDecoder: Decoder[Cart]           = deriveDecoder[Cart]
  implicit val cartItemDecoder: Encoder[CartItem]   = deriveEncoder[CartItem]
  implicit val cartTotalDecoder: Encoder[CartTotal] = deriveEncoder[CartTotal]
}
