package http

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import io.circe.Decoder
import org.http4s.{ Request, Response }
import org.http4s.circe.{ JsonDecoder, toMessageSyntax }
import org.http4s.dsl.Http4sDsl

package object routes {
  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow](request: Request[F]) extends Http4sDsl[F] {
    def decoderR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      request.asJsonDecode[A].attempt.flatMap {
        case Left(error) =>
          Option(error.getCause) match {
            case Some(cause) if cause.getMessage.startsWith("Predicate") => BadRequest(cause.getMessage)
            case _                                                       => UnprocessableEntity()
          }
        case Right(a) => f(a)
      }
  }
}
