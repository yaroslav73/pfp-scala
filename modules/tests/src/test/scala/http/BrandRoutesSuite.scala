package http

import cats.effect.IO
import cats.implicits.catsStdShowForList
import domain.Brand.BrandId
import domain.{ Brand, ID }
import http.routes.BrandRoutes
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.GET
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import services.Brands
import shop.Generators.brandGen
import suits.HttpSuite

object BrandRoutesSuite extends HttpSuite {
  test("GET brands succeeds") {
    forall(Gen.listOf(brandGen)) { brands =>
      val request = GET(uri"/brands")
      val routes  = new BrandRoutes[IO](dataBrands(brands)).routes

      expectedHttpBodyAndStatus(routes, request)(brands, Status.Ok)
    }
  }

  test("GET brands fails") {
    forall(Gen.listOf(brandGen)) { brands =>
      val request = GET(uri"/brands")
      val routes  = new BrandRoutes[IO](failingBrands(brands)).routes

      expectedHttpFailure(routes, request)
    }
  }

  private def dataBrands(brands: List[Brand]): Brands[IO] = new TestBrands {
    override def findAll: IO[List[Brand]] = IO.pure(brands)
  }

  private def failingBrands(brands: List[Brand]): Brands[IO] = new TestBrands {
    override def findAll: IO[List[Brand]] = IO.raiseError(DummyError) *> IO.pure(brands)
  }

  protected class TestBrands extends Brands[IO] {
    override def findAll: IO[List[Brand]]                         = IO.pure(List.empty)
    override def create(name: Brand.BrandName): IO[Brand.BrandId] = ID.make[IO, BrandId]
  }
}
