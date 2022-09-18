package http.routes.admin

import cats.MonadThrow
import cats.implicits.{ catsSyntaxFlatMapOps, toFlatMapOps }
import domain.Item.{ CreateItemParam, UpdateItemParam }
import http.auth.User.AdminUser
import http.routes.RefinedRequestDecoder
import io.circe.JsonObject
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax.EncoderOps
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import services.Items

final case class AdminItemRoutes[F[_]: JsonDecoder: MonadThrow](items: Items[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decoderR[CreateItemParam] { item =>
        items.create(item.toDomain).flatMap { id =>
          Created(JsonObject.singleton("item_id", id.asJson))
        }
      }

    case ar @ PUT -> Root as _ =>
      ar.req.decoderR[UpdateItemParam] { item =>
        items.update(item.toDomain) >> Ok()
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
