package programs

import cats.Monad
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import domain.Auth.UserId
import domain.Orders.OrderId
import domain.Payment.Card
import domain.{ Orders, Payment, PaymentClient, ShoppingCart }

final case class Checkout[F[_]: Monad](
  payments: PaymentClient[F],
  cart: ShoppingCart[F],
  orders: Orders[F]
) {
  def process(userId: UserId, card: Card): F[OrderId] = {
    for {
      c   <- cart.get(userId)
      pid <- payments.process(Payment(userId, c.total, card))
      oid <- orders.create(userId, pid, c.items, c.total)
      _   <- cart.delete(userId)
    } yield oid
  }
}
