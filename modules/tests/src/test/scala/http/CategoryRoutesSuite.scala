package http

import cats.effect.IO
import domain.{ Category, ID }
import domain.Category.CategoryId
import http.routes.CategoryRoutes
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import services.Categories
import suits.HttpSuite
import shop.Generators._

object CategoryRoutesSuite extends HttpSuite {
  test("GET category succeed") {
    forall(Gen.listOf(categoryGen)) { categories =>
      val request = GET(uri"/categories")
      val routes  = new CategoryRoutes[IO](succeedCategories(categories)).routes

      expectedHttpBodyAndStatus(routes, request)(categories, Status.Ok)
    }
  }

  test("GET category failed") {
    forall(Gen.listOf(categoryGen)) { categories =>
      val request = GET(uri"/categories")
      val routes  = new CategoryRoutes[IO](failingCategories(categories)).routes

      expectedHttpFailure(routes, request)
    }
  }

  private def succeedCategories(categories: List[Category]): Categories[IO] = new TestCategories {
    override def findAll: IO[List[Category]] = IO.pure(categories)
  }

  private def failingCategories(categories: List[Category]): Categories[IO] = new TestCategories {
    override def findAll: IO[List[Category]] = IO.raiseError(DummyError) *> IO.pure(categories)
  }

  private class TestCategories extends Categories[IO] {
    def findAll: IO[List[Category]]                         = IO.pure(List.empty)
    def create(name: Category.CategoryName): IO[CategoryId] = ID.make[IO, CategoryId]
  }
}
