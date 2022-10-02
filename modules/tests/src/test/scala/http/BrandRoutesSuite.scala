package http

import cats.effect.IO
import cats.implicits.{ catsStdShowForList, catsSyntaxSemigroup }
import domain.Brand.BrandId
import domain.{ Brand, ID }
import http.routes.BrandRoutes
import io.circe.syntax.EncoderOps
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.GET
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import services.Brands
import shop.Generators.{ brandGen, brandIdGen }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.util.control.NoStackTrace

object BrandRoutesSuite extends SimpleIOSuite with Checkers {
  test("GET brands succeeds") {
    forall(Gen.listOf(brandGen)) { brands =>
      val request = GET(uri"/brands")
      val routes  = new BrandRoutes[IO](dataBrands(brands)).routes

      routes.run(request).value.flatMap {
        case Some(response) =>
          response.asJson.map { json =>
            expect.same(response.status, Status.Ok) |+| expect.same(json.dropNullValues, brands.asJson.dropNullValues)
          }
        case None => IO.pure(failure("route not found"))
      }
    }
  }

  test("GET brands fails") {
    forall(Gen.listOf(brandGen)) { brands =>
      val request = GET(uri"/brands")
      val routes  = new BrandRoutes[IO](failingBrands(brands)).routes

      routes.run(request).value.attempt.map {
        case Left(_)  => success
        case Right(_) => failure("expected a failure")
      }
    }
  }

  private def dataBrands(brands: List[Brand]): Brands[IO] = new TestBrands {
    override def findAll: IO[List[Brand]] = IO.pure(brands)
  }

  private def failingBrands(brands: List[Brand]): Brands[IO] = new TestBrands {
    override def findAll: IO[List[Brand]] = IO.raiseError(DummyError) *> IO.pure(brands)
  }

  protected case object DummyError extends NoStackTrace

  protected class TestBrands extends Brands[IO] {
    override def findAll: IO[List[Brand]]                         = IO.pure(List.empty)
    override def create(name: Brand.BrandName): IO[Brand.BrandId] = ID.make[IO, BrandId]
  }
}
