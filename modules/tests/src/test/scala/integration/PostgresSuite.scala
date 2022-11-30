package integration

import cats.data.NonEmptyList
import cats.effect.{ IO, Resource }
import cats.implicits.toFoldableOps
import domain.Brand.BrandId
import domain.Category.CategoryId
import domain.Item.CreateItem
import domain.showMoney
import skunk._
import skunk.implicits._
import natchez.Trace.Implicits.noop
import org.scalacheck.Gen
import services.{ Brands, Categories, Items, Orders, Users }
import shop.Generators.{
  brandGen,
  cartItemGen,
  categoryGen,
  categoryIdGen,
  encryptedPasswordGen,
  itemGen,
  moneyGen,
  orderIdGen,
  paymentIdGen,
  userNameGen
}
import skunk.implicits.toStringOps
import skunk.{ Command, Session }
import suits.ResourceSuite

// TODO: what about using testcontainers?
object PostgresSuite extends ResourceSuite {
  type Res = Resource[IO, Session[IO]]

  val flushTable: List[Command[Void]] =
    List("items", "brands", "categories", "orders", "users")
      .map { table =>
        sql"DELETE FROM #$table".command
      }

  override def sharedResource: Resource[IO, Res] =
    Session
      .pooled[IO](
        host     = "localhost",
        port     = 5432,
        user     = "postgres",
        password = Some("password"),
        database = "store",
        max      = 10
      )
      .beforeAll { res =>
        res.use { session =>
          flushTable.traverse_(session.execute)
        }
      }

  test("Brands") { postgres =>
    forall(brandGen) { brand =>
      val brands = Brands.make[IO](postgres)

      for {
        a <- brands.findAll
        _ <- brands.create(brand.name)
        b <- brands.findAll
        c <- brands.create(brand.name).attempt
      } yield expect.all(a.isEmpty, b.count(_.name == brand.name) == 1, c.isLeft)
    }
  }

  test("Categories") { postgres =>
    forall(categoryGen) { category =>
      val categories = Categories.make[IO](postgres)

      for {
        a <- categories.findAll
        _ <- categories.create(category.name)
        b <- categories.findAll
        c <- categories.create(category.name).attempt
      } yield expect.all(a.isEmpty, b.count(_.name == category.name) == 1, c.isLeft)
    }
  }

  test("Items") { postgres =>
    forall(itemGen) { item =>
      def newItem(brandId: Option[BrandId], categoryId: Option[CategoryId]): CreateItem =
        CreateItem(
          name        = item.name,
          price       = item.price,
          description = item.description,
          brandId     = brandId.getOrElse(item.brand.uuid),
          categoryId  = categoryId.getOrElse(item.category.uuid)
        )

      val brands     = Brands.make[IO](postgres)
      val categories = Categories.make[IO](postgres)
      val items      = Items.make[IO](postgres)

      for {
        a   <- items.findAll
        _   <- brands.create(item.brand.name)
        bid <- brands.findAll.map(_.headOption.map(_.uuid))
        _   <- categories.create(item.category.name)
        cid <- categories.findAll.map(_.headOption.map(_.uuid))
        _   <- items.create(newItem(bid, cid))
        d   <- items.findAll
      } yield expect.all(a.isEmpty, d.count(_.name == item.name) == 1)
    }
  }

  test("Users") { postgres =>
    val gen =
      for {
        u <- userNameGen
        p <- encryptedPasswordGen
      } yield u -> p

    forall(gen) {
      case (username, password) =>
        val users = Users.make[IO](postgres)

        for {
          a <- users.create(username, password)
          b <- users.find(username)
          c <- users.create(username, password).attempt
        } yield expect.all(b.count(_.id == a) == 1, c.isLeft)
    }
  }

  test("Orders") { postgres =>
    val gen =
      for {
        orderId   <- orderIdGen
        paymentId <- paymentIdGen
        username  <- userNameGen
        password  <- encryptedPasswordGen
        items     <- Gen.nonEmptyListOf(cartItemGen).map(NonEmptyList.fromListUnsafe)
        price     <- moneyGen
      } yield (orderId, paymentId, username, password, items, price)

    forall(gen) {
      case (orderId, paymentId, username, password, items, price) =>
        val orders = Orders.make[IO](postgres)
        val users  = Users.make[IO](postgres)

        for {
          a <- users.create(username, password)
          b <- orders.findBy(a)
          c <- orders.get(a, orderId)
          d <- orders.create(a, paymentId, items.toList, price)
        } yield expect.all(b.isEmpty, c.isEmpty, d.uuid.version == 4)
    }
  }
}
