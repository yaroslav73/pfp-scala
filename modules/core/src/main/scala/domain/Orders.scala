package domain

import domain.Auth.UserId
import domain.Items.ItemId
import domain.Orders.{ Order, OrderId, PaymentId }
import domain.ShoppingCart.{ CartItem, Quantity }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import squants.market.Money

import java.util.UUID
import scala.util.control.NoStackTrace

trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId]
}

object Orders {
  final case class OrderId(uuid: UUID)
  final case class PaymentId(uuid: UUID)

  case class Order(
    id: OrderId,
    pid: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
  )
  object Order {
    implicit val orderIdEncoder: Encoder[OrderId]     = deriveEncoder[OrderId]
    implicit val paymentIdEncoder: Encoder[PaymentId] = deriveEncoder[PaymentId]
    implicit val orderEncoder: Encoder[Order]         = deriveEncoder[Order]
  }

  case object EmptyCartError extends NoStackTrace

  sealed trait OrderOrPaymentError extends NoStackTrace {
    def cause: String
  }
  final case class OrderError(cause: String) extends OrderOrPaymentError
  final case class PaymentError(cause: String) extends OrderOrPaymentError
}
