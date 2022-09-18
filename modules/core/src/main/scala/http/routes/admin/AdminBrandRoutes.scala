package http.routes.admin

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import domain.Brand.brandIdEncoder
import domain.Brand.BrandParam
import http.auth.User.AdminUser
import http.routes.RefinedRequestDecoder
import io.circe.JsonObject
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import services.Brands

final case class AdminBrandRoutes[F[_]: JsonDecoder: MonadThrow](brands: Brands[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decoderR[BrandParam] { brandParam =>
        brands.create(brandParam.toDomain).flatMap { id =>
          Created(JsonObject.singleton("brand_id", id.asJson))
        }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
