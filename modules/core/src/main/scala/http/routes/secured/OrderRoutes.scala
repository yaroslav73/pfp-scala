package http.routes.secured

import cats.Monad
import http.Vars.OrderIdVar
import http.auth.Users.CommonUser
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import services.Orders

final case class OrderRoutes[F[_]: Monad](orders: Orders[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case GET -> Root as user                       => Ok(orders.findBy(user.userId))
    case GET -> Root / OrderIdVar(orderId) as user => Ok(orders.get(user.userId, orderId))
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
