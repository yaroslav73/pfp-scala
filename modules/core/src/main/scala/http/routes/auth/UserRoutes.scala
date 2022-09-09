package http.routes.auth

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import domain.Auth.{ CreateUser, UserNameExist }
import http.routes.RefinedRequestDecoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import services.Auth
import services.Auth.jwtTokenEncoder

final case class UserRoutes[F[_]: JsonDecoder: MonadThrow](auth: Auth[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.decoderR[CreateUser] { user =>
        auth
          .newUser(user.username.toDomain, user.password.toDomain)
          .flatMap(token => Created(token))
          .recoverWith {
            case UserNameExist(u) => Conflict(u.value)
          }
      }
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
