package programs

import scala.concurrent.duration._
import scala.util.control.NoStackTrace
import cats.implicits._
import cats.effect.IO
import domain.Auth.UserId
import domain.Cart.CartTotal
import domain.Order.{ OrderId, PaymentId }
import domain.{ Auth, Card, Cart, Item, Order, Payment }
import effects.{ Background, TestBackground }
import eu.timepit.refined.cats.refTypeShow
import http.routes.clients.PaymentClient
import org.scalacheck.Gen
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import services.{ Orders, ShoppingCart }
import shop.Generators._
import squants.market.Money
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CheckoutSuite extends SimpleIOSuite with Checkers {
  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(pid: PaymentId): PaymentClient[IO] = new PaymentClient[IO] {
    def process(payment: Payment): IO[PaymentId] = IO.pure(pid)
  }

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit]   = IO.unit
  }

  def successfulOrders(orderId: OrderId): Orders[IO] = new TestOrders {
    override def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[Cart.CartItem],
      total: Money
    ): IO[OrderId] =
      IO.pure(orderId)
  }

  val gen: Gen[(UserId, PaymentId, OrderId, CartTotal, Card)] = for {
    userId    <- userIdGen
    paymentId <- paymentIdGen
    orderId   <- orderIdGen
    cartTotal <- cartTotalGen
    card      <- cardGen
  } yield (userId, paymentId, orderId, cartTotal, card)

  implicit val bg: Background[IO]                = TestBackground.NoOp
  implicit val lg: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  test("successful checkout") {
    forall(gen) {
      case (userId, paymentId, orderId, cartTotal, card) =>
        Checkout[IO](
          successfulClient(paymentId),
          successfulCart(cartTotal),
          successfulOrders(orderId),
          retryPolicy
        ).process(userId, card).map(expect.same(orderId, _))
    }
  }

  protected class TestOrders extends Orders[IO] {
    def get(userId: UserId, orderId: OrderId): IO[Option[Order]]                                            = ???
    def findBy(userId: UserId): IO[List[Order]]                                                             = ???
    def create(userId: UserId, paymentId: PaymentId, items: List[Cart.CartItem], total: Money): IO[OrderId] = ???
  }

  protected class TestCart extends ShoppingCart[IO] {
    def add(userId: Auth.UserId, itemId: Item.ItemId, quantity: Cart.Quantity): IO[Unit] = ???
    def get(userId: Auth.UserId): IO[Cart.CartTotal]                                     = ???
    def delete(userId: Auth.UserId): IO[Unit]                                            = ???
    def update(userId: Auth.UserId, cart: Cart): IO[Unit]                                = ???
    def removeItem(userId: Auth.UserId, itemId: Item.ItemId): IO[Unit]                   = ???
  }
}
