package programs

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApplicativeError, catsSyntaxApply, catsSyntaxMonadError, toFlatMapOps, toFunctorOps }
import domain.Auth.UserId
import domain.Cart.{ CartItem, CartTotal }
import domain.Orders._
import domain.{ Card, Orders, Payment, PaymentClient }
import effects.Background
import org.typelevel.log4cats.Logger
import retries.{ Retriable, Retry }
import retry.RetryPolicy
import services.ShoppingCart
import squants.market.Money

import scala.concurrent.duration.DurationInt

final case class Checkout[F[_]: Background: Logger: MonadThrow: Retry](
  payments: PaymentClient[F],
  cart: ShoppingCart[F],
  orders: Orders[F],
  policy: RetryPolicy[F],
) {
  def process(userId: UserId, card: Card): F[OrderId] = {
    for {
      c   <- cart.get(userId)
      pid <- payments.process(Payment(userId, c.total, card))
      oid <- orders.create(userId, pid, c.items, c.total)
      _   <- cart.delete(userId)
    } yield oid

    cart
      .get(userId)
      .ensure(EmptyCartError)(_.items.nonEmpty)
      .flatMap {
        case CartTotal(items, total) =>
          for {
            pid <- payments.process(Payment(userId, total, card))
            oid <- orders.create(userId, pid, items, total)
            _   <- cart.delete(userId)
          } yield oid
      }
  }

  private def processPayment(in: Payment): F[PaymentId] =
    Retry[F].retry(policy, Retriable.Payments)(payments.process(in)).adaptError {
      case e => PaymentError(Option(e.getMessage).getOrElse("Unknown"))
    }

  private def createOrder(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId] = {
    val action = Retry[F].retry(policy, Retriable.Orders)(orders.create(userId, paymentId, items, total)).adaptError {
      case e => OrderError(e.getMessage)
    }

    def backgroundAction(fa: F[OrderId]): F[OrderId] =
      fa.onError {
        case _ =>
          Logger[F].error(s"Failed to create order for: $paymentId") *>
            Background[F].schedule(backgroundAction(fa), 1.hour)
      }

    backgroundAction(action)
  }
}
