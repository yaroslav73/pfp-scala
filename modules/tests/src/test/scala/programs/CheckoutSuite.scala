package programs

import cats.effect.unsafe.implicits.global

import scala.concurrent.duration._
import scala.util.control.NoStackTrace
import cats.implicits._
import cats.effect.{ IO, Ref }
import domain.Auth.{ UserId, userIdShow }
import domain.Cart.CartTotal
import domain.Order.{ EmptyCartError, OrderError, OrderId, PaymentError, PaymentId }
import domain.{ Auth, Card, Cart, Item, Order, Payment }
import effects.{ Background, TestBackground }
import eu.timepit.refined.cats.refTypeShow
import fs2.concurrent.SignallingRef
import http.routes.clients.PaymentClient
import org.scalacheck.Gen
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import retries.{ Retry, TestRetry }
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import services.{ Orders, ShoppingCart }
import shop.Generators._
import squants.market.{ Money, USD }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CheckoutSuite extends SimpleIOSuite with Checkers {

  implicit val lg: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(pid: PaymentId): PaymentClient[IO] = new PaymentClient[IO] {
    def process(payment: Payment): IO[PaymentId] = IO.pure(pid)
  }

  def recoveringClient(attemptsSoFar: Ref[IO, Int], paymentId: PaymentId): PaymentClient[IO] = new PaymentClient[IO] {
    def process(payment: Payment): IO[PaymentId] =
      attemptsSoFar.get.flatMap {
        case n if n === 1 => IO.pure(paymentId)
        case _            => attemptsSoFar.update(_ + 1) *> IO.raiseError(PaymentError(""))
      }
  }

  val unreachableClient: PaymentClient[IO] = new PaymentClient[IO] {
    def process(payment: Payment): IO[PaymentId] = IO.raiseError(PaymentError(""))
  }

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit]   = IO.unit
  }

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit]   = IO.raiseError(new NoStackTrace {})
  }

  val emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def successfulOrders(orderId: OrderId): Orders[IO] = new TestOrders {
    override def create(userId: UserId, paymentId: PaymentId, items: List[Cart.CartItem], total: Money): IO[OrderId] =
      IO.pure(orderId)
  }

  val failingOrders: Orders[IO] = new TestOrders {
    override def create(userId: UserId, paymentId: PaymentId, items: List[Cart.CartItem], total: Money): IO[OrderId] =
      IO.raiseError(OrderError(""))
  }

  val gen: Gen[(UserId, PaymentId, OrderId, CartTotal, Card)] = for {
    userId    <- userIdGen
    paymentId <- paymentIdGen
    orderId   <- orderIdGen
    cartTotal <- cartTotalGen
    card      <- cardGen
  } yield (userId, paymentId, orderId, cartTotal, card)

  test("successful checkout") {
    implicit val bg: Background[IO] = TestBackground.NoOp

    forall(gen) {
      case (userId, paymentId, orderId, cartTotal, card) =>
        Checkout[IO](successfulClient(paymentId), successfulCart(cartTotal), successfulOrders(orderId), retryPolicy)
          .process(userId, card)
          .map(expect.same(orderId, _))
    }
  }

  test("empty cart") {
    implicit val bg: Background[IO] = TestBackground.NoOp

    forall(gen) {
      case (userId, paymentId, orderId, _, card) =>
        Checkout[IO](successfulClient(paymentId), emptyCart, successfulOrders(orderId), retryPolicy)
          .process(userId, card)
          .attempt
          .map {
            case Left(EmptyCartError) => success
            case _                    => failure("Cart was not empty as expected")
          }
    }
  }

  test("unreachable payment client") {
    implicit val bg: Background[IO] = TestBackground.NoOp

    forall(gen) {
      case (userId, _, orderId, cartTotal, card) =>
        Ref.of[IO, Option[GivingUp]](None).flatMap { retries =>
          implicit val retryHandler: Retry[IO] = TestRetry.givingUp(retries)

          Checkout[IO](unreachableClient, successfulCart(cartTotal), successfulOrders(orderId), retryPolicy)
            .process(userId, card)
            .attempt
            .flatMap {
              case Left(PaymentError(_)) =>
                retries.get.map {
                  case Some(givingUp) => expect.same(givingUp.totalRetries, MaxRetries)
                  case None           => failure("Expected GivingUp")
                }
              case _ => IO.pure(failure("Expected payment error"))
            }
        }
    }
  }

  test("failing payment client succeeds after one retry") {
    implicit val bg: Background[IO] = TestBackground.NoOp

    forall(gen) {
      case (userId, paymentId, orderId, cartTotal, card) =>
        (Ref.of[IO, Option[WillDelayAndRetry]](None), Ref.of[IO, Int](0)).tupled.flatMap {
          case (retries, clientRef) =>
            implicit val retryHandler: Retry[IO] = TestRetry.recovering(retries)

            Checkout[IO](
              recoveringClient(clientRef, paymentId),
              successfulCart(cartTotal),
              successfulOrders(orderId),
              retryPolicy
            ).process(userId, card).attempt.flatMap {
              case Right(oid) =>
                retries.get.map {
                  case Some(willDelay) => expect.same(oid, orderId) |+| expect.same(0, willDelay.retriesSoFar)
                  case None            => failure("Expected one retry")
                }
              case Left(_) => IO.pure(failure("Expected Order Id"))
            }
        }
    }
  }

  test("cannot create order, run in the background") {
    forall(gen) {
      case (userId, paymentId, _, cartTotal, card) =>
        (Ref.of[IO, (Int, FiniteDuration)](0 -> 0.seconds), Ref.of[IO, Option[GivingUp]](None)).tupled.flatMap {
          case (acc, retries) =>
            implicit val background: Background[IO] = TestBackground.counter(acc)
            implicit val retryHandler: Retry[IO]    = TestRetry.givingUp(retries)

            Checkout[IO](successfulClient(paymentId), successfulCart(cartTotal), failingOrders, retryPolicy)
              .process(userId, card)
              .attempt
              .flatMap {
                case Left(OrderError(_)) =>
                  (acc.get, retries.get).mapN {
                    case (c, Some(g)) => expect.same(c, 1 -> 1.hour) |+| expect.same(g.totalRetries, MaxRetries)
                    case _            => failure(s"Expected $MaxRetries retries and reschedule")
                  }
                case _ => IO.pure(failure("Expected order error"))
              }
        }
    }
  }

  test("failing to delete cart does not affect checkout") {
    implicit val bg: Background[IO] = TestBackground.NoOp

    forall(gen) {
      case (userId, paymentId, orderId, cartTotal, card) =>
        Checkout[IO](successfulClient(paymentId), failingCart(cartTotal), successfulOrders(orderId), retryPolicy)
          .process(userId, card)
          .map(expect.same(orderId, _))
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
