package services

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import domain.Brand.BrandName
import domain.{ Brand, Category, ID }
import domain.Items.{ CreateItem, Item, ItemId, UpdateItem }
import effects.GenUUID
import skunk._
import skunk.implicits._

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}

object Items {
  def make[F[_]: Concurrent: GenUUID](postgres: Resource[F, Session[F]]): Items[F] =
    new Items[F] {
      import ItemSQL._

      def findAll: F[List[Item]] =
        postgres.use(session => session.execute(selectAll))

      def findBy(brand: BrandName): F[List[Item]] =
        postgres.use { session =>
          session.prepare(selectByBrand).use { preparedQuery =>
            preparedQuery.stream(brand, 1024).compile.toList
          }
        }

      def findById(itemId: ItemId): F[Option[Item]] =
        postgres.use { session =>
          session.prepare(selectById).use { preparedQuery =>
            preparedQuery.option(itemId)
          }
        }

      def create(item: CreateItem): F[ItemId] =
        postgres.use { session =>
          session.prepare(insertItem).use { command =>
            ID.make[F, ItemId].flatMap { id =>
              command.execute(id ~ item).as(id)
            }
          }
        }

      def update(item: UpdateItem): F[Unit] =
        postgres.use { session =>
          session.prepare(updateItem).use { command =>
            command.execute(item).void
          }
        }
    }

  private object ItemSQL {
    import sql.Codecs._

    val decoder: Decoder[Item] =
      (itemId ~ itemName ~ itemDesc ~ money ~ brandId ~ brandName ~ categoryId ~ categoryName).map {
        case id ~ name ~ desc ~ price ~ brandId ~ brandName ~ categoryId ~ categoryName =>
          Item(id, name, desc, price, Brand(brandId, brandName), Category(categoryId, categoryName))
      }

    val selectAll: Query[Void, Item] =
      sql"""
           SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
           FROM items AS i
           INNER JOIN brands AS b ON i.brand_id = b.uuid
           INNER JOIN categories AS c ON i.category_id = c.uuid
         """.query(decoder)

    val selectByBrand: Query[BrandName, Item] =
      sql"""
           SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
           FROM items AS i
           INNER JOIN brands AS b ON i.brand_id = b.uuid
           INNER JOIN categories AS c ON i.category_id = c.uuid
           WHERE b.name LIKE $brandName
         """.query(decoder)

    val selectById: Query[ItemId, Item] =
      sql"""
           SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
           FROM items AS i
           INNER JOIN brands AS b ON i.brand_id = b.uuid
           INNER JOIN categories AS c ON i.category_id = c.uuid
           WHERE i.uuid = $itemId
         """.query(decoder)

    val insertItem: Command[ItemId ~ CreateItem] =
      sql"""
           INSERT INTO items
           VALUES ($itemId, $itemName, $itemDesc, $money, $brandId, $categoryId)
         """.command.contramap {
        case id ~ i => id ~ i.name ~ i.description ~ i.price ~ i.brandId ~ i.categoryId
      }

    val updateItem: Command[UpdateItem] =
      sql"""
           UPDATE items
           SET price = $money
           WHERE uuid = $itemId
         """.command.contramap { i =>
        i.price ~ i.itemId
      }
  }
}
