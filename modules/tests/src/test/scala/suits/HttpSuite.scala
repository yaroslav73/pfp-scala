package suits

import cats.effect.IO
import cats.implicits.catsSyntaxSemigroup
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.{ HttpRoutes, Request, Status }
import weaver.scalacheck.Checkers
import weaver.{ Expectations, SimpleIOSuite }

trait HttpSuite extends SimpleIOSuite with Checkers {
  def expectedHttpBodyAndStatus[A: Encoder](
    routes: HttpRoutes[IO],
    request: Request[IO]
  )(expectedBody: A, expectedStatus: Status): IO[Expectations] = {
    routes.run(request).value.flatMap {
      case Some(response) =>
        response.asJson.map { json =>
          expect.same(response.status, expectedStatus) |+| expect
            .same(json.dropNullValues, expectedBody.asJson.dropNullValues)
        }
      case None => IO.pure(failure("route not found"))
    }
  }

  def expectedHttpStatus(routes: HttpRoutes[IO], request: Request[IO])(expectedStatus: Status): IO[Expectations] = {
    routes.run(request).value.map {
      case Some(response) => expect.same(response.status, expectedStatus)
      case None           => failure("route not found")
    }
  }

  def expectedHttpFailure(routes: HttpRoutes[IO], request: Request[IO]): IO[Expectations] = {
    routes.run(request).value.attempt.map {
      case Left(_)  => success
      case Right(_) => failure("expected a failure")
    }
  }
}
