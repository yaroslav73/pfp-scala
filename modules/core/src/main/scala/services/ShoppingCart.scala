package services

import cats.MonadThrow
import cats.implicits.{ catsSyntaxApply, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseFilterOps }
import dev.profunktor.redis4cats.RedisCommands
import domain.Auth.UserId
import domain.{ Cart, ID, monoidMoney }
import domain.Cart.{ CartTotal, Quantity, ShoppingCartExpiration }
import domain.Item.ItemId
import effects.GenUUID

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {
  def make[F[_]: GenUUID: MonadThrow](
    items: Items[F],
    redis: RedisCommands[F, String, String],
    expiration: ShoppingCartExpiration,
  ): ShoppingCart[F] = new ShoppingCart[F] {
    def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
      redis.hSet(userId.value.toString, itemId.value.toString, quantity.value.toString) *>
        redis.expire(userId.value.toString, expiration.value).void

    def get(userId: UserId): F[CartTotal] =
      redis.hGetAll(userId.value.toString).flatMap { all =>
        all.toList
          .traverseFilter {
            case (k, v) =>
              for {
                itemId   <- ID.read[F, ItemId](k)
                quantity <- MonadThrow[F].catchNonFatal(Quantity(v.toInt))
                result   <- items.findById(itemId).map(_.map(item => item.cart(quantity)))
              } yield result
          }
          .map { items =>
            CartTotal(items, items.foldMap(_.subTotal))
          }
      }

    def delete(userId: UserId): F[Unit] =
      redis.del(userId.value.toString).void

    def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
      redis.hDel(userId.value.toString, itemId.value.toString).void

    def update(userId: UserId, cart: Cart): F[Unit] =
      redis.hGetAll(userId.value.toString).flatMap { all =>
        all.toList.traverse_ {
          case (k, _) =>
            ID.read[F, ItemId](k).flatMap { id =>
              cart.items.get(id).traverse_ { quantity =>
                redis.hSet(userId.value.toString, k, quantity.value.toString)
              }
            }
        } *> redis.expire(userId.value.toString, expiration.value).void
      }
  }
}
