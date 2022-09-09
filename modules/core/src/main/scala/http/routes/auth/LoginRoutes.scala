package http.routes.auth

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import domain.Auth.{ InvalidPassword, LoginUser, UserNotFound }
import http.routes.RefinedRequestDecoder
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import services.Auth

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](auth: Auth[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/login"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decoderR[LoginUser] { user =>
        auth
          .login(user.username.toDomain, user.password.toDomain)
          .flatMap(Ok(_))
          .recoverWith {
            case UserNotFound(_) | InvalidPassword(_) => Forbidden()
          }
      }
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
