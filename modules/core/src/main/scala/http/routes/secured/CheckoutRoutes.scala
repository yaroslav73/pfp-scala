package http.routes.secured

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import domain.Card
import domain.Cart.CartNotFound
import domain.Order.orderIdEncoder
import domain.Order.{ EmptyCartError, OrderOrPaymentError }
import http.auth.Users.CommonUser
import http.routes.RefinedRequestDecoder
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import programs.Checkout

final case class CheckoutRoutes[F[_]: JsonDecoder: MonadThrow](checkout: Checkout[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user =>
      ar.req.decoderR[Card] { card =>
        checkout
          .process(user.userId, card)
          .flatMap(Created(_))
          .recoverWith {
            case CartNotFound(userId)   => NotFound(s"Cart not found for user: ${userId.value}")
            case EmptyCartError         => BadRequest("Shopping cart is empty.")
            case e: OrderOrPaymentError => BadRequest(e.cause)
          }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
