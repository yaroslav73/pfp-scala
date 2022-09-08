package http.routes.secured

import cats.Monad
import cats.implicits.{ catsSyntaxApply, toFlatMapOps, toTraverseOps }
import domain.Cart
import http.Vars.ItemIdVar
import http.auth.Users.CommonUser
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{ JsonDecoder, toMessageSyntax }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import services.ShoppingCart

final case class CartRoutes[F[_]: JsonDecoder: Monad](shoppingCart: ShoppingCart[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case GET -> Root as user => Ok(shoppingCart.get(user.user.id))
    case ar @ POST -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap {
        _.items
          .map {
            case (id, quantity) => shoppingCart.add(user.user.id, id, quantity)
          }
          .toList
          .sequence *> Created()
      }
    case ar @ PUT -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap {
        shoppingCart.update(user.user.id, _) *> Ok()
      }
    case DELETE -> Root / ItemIdVar(itemId) as user =>
      shoppingCart.removeItem(user.user.id, itemId) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
