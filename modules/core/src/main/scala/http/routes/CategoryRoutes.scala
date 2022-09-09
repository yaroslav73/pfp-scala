package http.routes

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import services.Categories

final case class CategoryRoutes[F[_]: Monad](categories: Categories[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(categories.findAll)
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
