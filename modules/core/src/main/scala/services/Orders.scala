package services

import cats.effect.kernel.{ Concurrent, Resource }
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import domain.Auth.UserId
import domain.Cart.{ CartItem, Quantity }
import domain.{ ID, Order }
import domain.Item.ItemId
import domain.Order.{ OrderId, PaymentId }
import effects.GenUUID
import skunk._
import skunk.circe.codec.all.jsonb
import skunk.implicits._
import squants.market.Money

trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId]
}

object Orders {
  def make[F[_]: Concurrent: GenUUID](postgres: Resource[F, Session[F]]): Orders[F] =
    new Orders[F] {
      import OrderSQL._

      def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
        postgres.use { session =>
          session.prepare(selectByUserIdAndOrderId).use { preparedQuery =>
            preparedQuery.option(userId ~ orderId)
          }
        }

      def findBy(userId: UserId): F[List[Order]] =
        postgres.use { session =>
          session.prepare(selectByUserId).use { preparedQuery =>
            preparedQuery.stream(userId, 1024).compile.toList
          }
        }

      def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId] =
        postgres.use { session =>
          session.prepare(insertOrder).use { command =>
            ID.make[F, OrderId].flatMap { id =>
              val cartItemToItems = items.map(cartItem => cartItem.item.uuid -> cartItem.quantity).toMap
              val order           = Order(id, paymentId, cartItemToItems, total)
              command.execute(userId ~ order).as(id)
            }
          }
        }
    }

  private object OrderSQL {
    import sql.Codecs._

    val decoder: Decoder[Order] =
      (orderId ~ userId ~ paymentId ~ jsonb[Map[ItemId, Quantity]] ~ money).map {
        case id ~ _ ~ paymentId ~ items ~ total => Order(id, paymentId, items, total)
      }

    val encoder: Encoder[UserId ~ Order] =
      (orderId ~ userId ~ paymentId ~ jsonb[Map[ItemId, Quantity]] ~ money).contramap {
        case id ~ order => order.id ~ id ~ order.pid ~ order.items ~ order.total
      }

    val selectByUserId: Query[UserId, Order] =
      sql"""SELECT * FROM orders WHERE user_id = $userId""".query(decoder)

    val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
      sql"""
           SELECT * FROM orders
           WHERE user_id = $userId AND uuid = $orderId
         """.query(decoder)

    val insertOrder: Command[UserId ~ Order] =
      sql"""INSERT INTO orders VALUES ($encoder)""".command
  }
}
