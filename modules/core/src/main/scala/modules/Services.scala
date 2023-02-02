package modules

import cats.effect.Temporal
import cats.effect.kernel.Resource
import dev.profunktor.redis4cats.RedisCommands
import domain.Cart.ShoppingCartExpiration
import effects.GenUUID
import services.{ Brands, Categories, HealthCheck, Items, Orders, ShoppingCart }
import skunk.Session

sealed abstract class Services[F[_]] private (
  val shoppingCart: ShoppingCart[F],
  val brands: Brands[F],
  val categories: Categories[F],
  val items: Items[F],
  val orders: Orders[F],
  val healthCheck: HealthCheck[F],
)

object Services {
  def make[F[_]: GenUUID: Temporal](
    redis: RedisCommands[F, String, String],
    postgres: Resource[F, Session[F]],
    cartExpiration: ShoppingCartExpiration,
  ): Services[F] = {
    val _items = Items.make(postgres)

    new Services[F](
      shoppingCart = ShoppingCart.make(_items, redis, cartExpiration),
      brands       = Brands.make(postgres),
      categories   = Categories.make(postgres),
      items        = _items,
      orders       = Orders.make(postgres),
      healthCheck  = HealthCheck.make(postgres, redis)
    ) {}
  }
}
