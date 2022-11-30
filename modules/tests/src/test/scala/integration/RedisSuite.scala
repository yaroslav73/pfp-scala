package integration

import auth.{ Crypto, JwtExpire, Tokens }
import cats.effect.{ IO, Ref, Resource }
import cats.implicits.{ catsSyntaxAlternativeGuard, toFunctorOps }
import config.Types.{ JwtAccessTokenKeyConfig, JwtSecretKeyConfig, PasswordSalt, TokenExpiration }
import dev.profunktor.auth.jwt.{ JwtAuth, JwtToken, jwtDecode }
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import domain.Auth.{ EncryptedPassword, InvalidPassword, UserId, UserName, UserNotFound }
import domain.Brand.BrandName
import domain.Cart.ShoppingCartExpiration
import domain.Category.CategoryName
import domain.{ Brand, Cart, Category, ID, Item }
import domain.Item.{ CreateItem, ItemId, UpdateItem }
import eu.timepit.refined.types.string.NonEmptyString
import http.auth.User
import http.auth.User.{ UserJwtAuth, UserWithPassword }
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import pdi.jwt.{ JwtAlgorithm, JwtClaim }
import services.{ Auth, Items, ShoppingCart, Users, UsersAuth }
import shop.Generators.{ itemGen, passwordGen, quantityGen, userIdGen, userNameGen }
import suits.ResourceSuite

import java.util.UUID
import scala.concurrent.duration.DurationInt

object RedisSuite extends ResourceSuite {
  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  private val expiration: ShoppingCartExpiration = ShoppingCartExpiration(30.seconds)

  private val tokenConfig     = JwtAccessTokenKeyConfig(NonEmptyString.unsafeFrom("bar"))
  private val tokenExpiration = TokenExpiration(30.seconds)
  private val jwtClaim        = JwtClaim("test")
  private val userJwtAuth     = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  test("Shopping Cart") { redis =>
    val gen = for {
      userId    <- userIdGen
      item1     <- itemGen
      item2     <- itemGen
      quantity1 <- quantityGen
      quantity2 <- quantityGen
    } yield (userId, item1, item2, quantity1, quantity2)

    forall(gen) {
      case (userId, item1, item2, quantity1, quantity2) =>
        Ref
          .of[IO, Map[ItemId, Item]](Map(item1.uuid -> item1, item2.uuid -> item2))
          .flatMap { ref =>
            val items        = new TestItems(ref)
            val shoppingCart = ShoppingCart.make[IO](items, redis, expiration)

            for {
              a <- shoppingCart.get(userId)
              _ <- shoppingCart.add(userId, item1.uuid, quantity1)
              _ <- shoppingCart.add(userId, item2.uuid, quantity2)
              b <- shoppingCart.get(userId)
              _ <- shoppingCart.removeItem(userId, item1.uuid)
              c <- shoppingCart.get(userId)
              _ <- shoppingCart.update(userId, Cart(Map(item2.uuid -> quantity2)))
              d <- shoppingCart.get(userId)
              _ <- shoppingCart.delete(userId)
              e <- shoppingCart.get(userId)
            } yield expect.all(
              a.items.isEmpty,
              b.items.size == 2,
              c.items.size == 1,
              d.items.headOption.fold(false)(_.quantity == quantity2),
              e.items.isEmpty
            )
          }
    }
  }

  test("Authentication") { redis =>
    val gen = for {
      username1 <- userNameGen
      username2 <- userNameGen
      password  <- passwordGen
    } yield (username1, username2, password)

    forall(gen) {
      case (username1, username2, password) =>
        for {
          tokens <- JwtExpire.make[IO].map(Tokens.make[IO](_, tokenConfig, tokenExpiration))
          crypto <- Crypto.make[IO](PasswordSalt(NonEmptyString.unsafeFrom("test")))
          auth   = Auth.make[IO](tokenExpiration, tokens, new TestUsers(username2), redis, crypto)
          users  = UsersAuth.common[IO](redis)
          a      <- users.findUser(JwtToken("invalid"))(jwtClaim)
          b      <- auth.login(username1, password).attempt
          c      <- auth.newUser(username1, password)
          d      <- jwtDecode[IO](c, userJwtAuth.value).attempt
          e      <- auth.login(username2, password).attempt
          f      <- users.findUser(c)(jwtClaim)
          _      <- auth.logout(c, username1)
          g      <- redis.get(c.value)
          _      <- auth.logout(c, username1)
          h      <- redis.get(c.value)
        } yield expect.all(
          a.isEmpty,
          b == Left(UserNotFound(username1)),
          d.isRight,
          e == Left(InvalidPassword(username2)),
          f.fold(false)(_.user.name == username1),
          g.nonEmpty,
          h.isEmpty
        )
    }
  }

  protected class TestItems(ref: Ref[IO, Map[ItemId, Item]]) extends Items[IO] {
    def findAll: IO[List[Item]] =
      ref.get.map(_.values.toList)

    def findBy(brand: BrandName): IO[List[Item]] =
      ref.get.map(_.values.filter(_.brand.name == brand).toList)

    def findById(itemId: ItemId): IO[Option[Item]] =
      ref.get.map(_.get(itemId))

    def create(item: CreateItem): IO[ItemId] =
      ID.make[IO, ItemId].flatTap { itemId =>
        val brand    = Brand(item.brandId, BrandName("foo"))
        val category = Category(item.categoryId, CategoryName("foo"))
        val newItem  = Item(itemId, item.name, item.description, item.price, brand, category)

        ref.update(_.updated(itemId, newItem))
      }

    def update(item: UpdateItem): IO[Unit] =
      ref.update { items =>
        items.get(item.itemId).fold(items) { i =>
          items.updated(item.itemId, i.copy(price = item.price))
        }
      }
  }

  private class TestUsers(un: UserName) extends Users[IO] {
    def find(username: UserName): IO[Option[UserWithPassword]] = IO.pure {
      (username == un).guard[Option].as {
        UserWithPassword(UserId(UUID.randomUUID), un, EncryptedPassword("foo"))
      }
    }

    def create(username: UserName, password: EncryptedPassword): IO[UserId] =
      ID.make[IO, UserId]
  }
}
