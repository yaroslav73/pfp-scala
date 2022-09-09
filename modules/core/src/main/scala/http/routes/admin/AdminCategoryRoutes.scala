package http.routes.admin

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import domain.Categories
import domain.Categories.Category.categoryIdEncoder
import domain.Categories.CategoryParam
import http.auth.Users.AdminUser
import http.routes.RefinedRequestDecoder
import io.circe.JsonObject
import io.circe.syntax.EncoderOps
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }

final case class AdminCategoryRoutes[F[_]: JsonDecoder: MonadThrow](categories: Categories[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/categories"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decoderR[CategoryParam] { categoryParam =>
        categories.create(categoryParam.toDomain).flatMap { id =>
          Created(JsonObject.singleton("category_id", id.asJson))
        }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
