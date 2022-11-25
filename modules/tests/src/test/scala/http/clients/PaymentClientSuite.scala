package http.clients

import cats.data.Kleisli
import cats.effect.IO
import config.Types.{ PaymentConfig, PaymentURI }
import domain.Order.PaymentError
import eu.timepit.refined.types.string.NonEmptyString
import http.routes.clients.PaymentClient
import org.http4s.{ HttpRoutes, Response }
import suits.HttpSuite
import org.http4s.Method.{ POST, _ }
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.client.dsl.io._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import shop.Generators._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object PaymentClientSuite extends SimpleIOSuite with Checkers {

  private val gen =
    for {
      paymentId <- paymentIdGen
      payment   <- paymentGen
    } yield paymentId -> payment

  test("Response Ok (200)") {
    forall(gen) {
      case (paymentId, payment) =>
        val client = Client.fromHttpApp(routes(Ok(paymentId)))

        PaymentClient
          .make[IO](config, client)
          .process(payment)
          .map(expect.same(paymentId, _))
    }
  }

  test("Response Conflict (409)") {
    forall(gen) {
      case (paymentId, payment) =>
        val client = Client.fromHttpApp(routes(Conflict(paymentId)))

        PaymentClient
          .make[IO](config, client)
          .process(payment)
          .map(expect.same(paymentId, _))
    }
  }

  test("Response Internal Server Error (500)") {
    forall(gen) {
      case (_, payment) =>
        val client = Client.fromHttpApp(routes(InternalServerError()))

        PaymentClient
          .make[IO](config, client)
          .process(payment)
          .attempt
          .map {
            case Left(error) => expect.same(PaymentError("Internal Server Error"), error)
            case Right(_)    => failure("expected payment error")
          }
    }
  }

  private val config = PaymentConfig(PaymentURI(NonEmptyString("http://localhost")))

  private def routes(mkResponse: IO[Response[IO]]): Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] {
        case POST -> Root / "payments" => mkResponse
      }
      .orNotFound
}
