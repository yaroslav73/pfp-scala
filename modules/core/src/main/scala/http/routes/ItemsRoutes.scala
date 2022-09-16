package http.routes

import cats.Monad
import cats.implicits.toBifunctorOps
import domain.Brand.BrandParam
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, ParseFailure, QueryParamDecoder }
import services.Items

final case class ItemsRoutes[F[_]: Monad](items: Items[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/items"

  implicit val brandParamQueryParamDecoder: QueryParamDecoder[BrandParam] =
    QueryParamDecoder[String]
      .emap(
        value =>
          NonEmptyString
            .from(value)
            .bimap(error => ParseFailure(error, error), success => BrandParam(success))
      )

  object BrandQueryParam extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? BrandQueryParam(brand) =>
      Ok(brand.fold(items.findAll)(b => items.findBy(b.toDomain)))
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
