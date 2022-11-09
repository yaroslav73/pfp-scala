package http

import cats.effect.IO
import domain.Brand.{ BrandId, BrandName }
import domain.Item.{ CreateItem, ItemId, UpdateItem }
import domain.{ Brand, ID, Item }
import http.routes.ItemsRoutes
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import services.Items
import shop.Generators.itemGen
import suits.HttpSuite

import java.util.UUID

object ItemsRoutesSuite extends HttpSuite {
  test("GET items succeeds") {
    forall(Gen.listOf(itemGen)) { items =>
      val request = GET(uri"/items")
      val routes  = new ItemsRoutes[IO](dataItems(items)).routes

      expectedHttpBodyAndStatus(routes, request)(items, Status.Ok)
    }
  }

  test("GET items by brand succeeds") {
    val testBrand = Brand(BrandId(UUID.randomUUID()), BrandName("Test"))

    val gen = for {
      items  <- Gen.listOf(itemGen)
      brands <- Gen.someOf(items.map(_.brand))
      index  <- Gen.chooseNum(0, brands.size)
      brand  = if (brands.isEmpty || index > brands.size - 1) testBrand else brands(index)
    } yield items -> brand

    forall(gen) {
      case (items, brand) =>
        val request = GET(uri"/items".withQueryParam("brand", brand.name.value))
        val routes  = new ItemsRoutes[IO](dataItems(items)).routes

        val expected = items.find(_.brand.name == brand.name).toList
        expectedHttpBodyAndStatus(routes, request)(expected, Status.Ok)
    }
  }

  test("GET items fails") {
    forall(Gen.listOf(itemGen)) { items =>
      val request = GET(uri"/items")
      val routes  = new ItemsRoutes[IO](failedItems(items)).routes

      expectedHttpFailure(routes, request)
    }
  }

  private def dataItems(items: List[Item]): Items[IO] = new TestItems {
    override def findAll: IO[List[Item]] = IO.pure(items)
    override def findBy(brandName: BrandName): IO[List[Item]] =
      IO.pure(items.find(_.brand.name.value.toLowerCase equals brandName.value.toLowerCase).toList)
  }

  private def failedItems(items: List[Item]): Items[IO] = new TestItems {
    override def findAll: IO[List[Item]] = IO.raiseError(DummyError) *> IO.pure(items)
  }
  protected class TestItems extends Items[IO] {
    def findAll: IO[List[Item]]                    = IO.pure(List.empty)
    def findBy(brand: BrandName): IO[List[Item]]   = IO.pure(List.empty)
    def findById(itemId: ItemId): IO[Option[Item]] = IO.pure(None)
    def create(item: CreateItem): IO[ItemId]       = ID.make[IO, ItemId]
    def update(item: UpdateItem): IO[Unit]         = IO.unit
  }
}
