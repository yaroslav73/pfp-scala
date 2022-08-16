package domain

import domain.Auth.UserId
import domain.Items.ItemId
import domain.Orders.{Order, OrderId, PaymentId}
import domain.ShoppingCart.{CartItem, Quantity}
import io.estatico.newtype.macros.newtype
import squants.market.Money

import java.util.UUID

trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId]
}

object Orders {
  @newtype case class OrderId(uuid: UUID)
  @newtype case class PaymentId(uuid: UUID)

  case class Order(
    id: OrderId,
    pid: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
  )
}
