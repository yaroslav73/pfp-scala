package services

import cats.effect.kernel.{ MonadCancelThrow, Resource }
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import domain.{ Category, ID }
import domain.Category.{ CategoryId, CategoryName }
import effects.GenUUID
import skunk._
import skunk.implicits._

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]
}

object Categories {
  def make[F[_]: GenUUID: MonadCancelThrow](postgres: Resource[F, Session[F]]): Categories[F] =
    new Categories[F] {
      import CategorySQL._

      def findAll: F[List[Category]] =
        postgres.use(session => session.execute(selectAll))

      def create(name: CategoryName): F[CategoryId] =
        postgres.use { session =>
          session.prepare(insertCategory).use { command =>
            ID.make[F, CategoryId].flatMap { id =>
              command.execute(Category(id, name)).as(id)
            }
          }
        }
    }
  private object CategorySQL {
    import sql.Codecs._

    val codec: Codec[Category] =
      (categoryId ~ categoryName).imap {
        case id ~ name => Category(id, name)
      }(category => category.uuid ~ category.name)

    val selectAll: Query[Void, Category] =
      sql"""SELECT * FROM categories""".query(codec)

    val insertCategory: Command[Category] =
      sql"""INSERT INTO categories VALUES ($codec)""".command
  }
}
