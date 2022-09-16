package services

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import domain.Brands.{ Brand, BrandId, BrandName }
import domain.ID
import effects.GenUUID
import skunk._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[BrandId]
}

object Brands {
  def make[F[_]: GenUUID: MonadCancelThrow](postgres: Resource[F, Session[F]]): Brands[F] =
    new Brands[F] {
      import BrandSQL._

      def findAll: F[List[Brand]] =
        postgres.use(session => session.execute(selectAll))

      def create(name: BrandName): F[BrandId] =
        postgres.use { session =>
          session.prepare(insertBrand).use { command =>
            ID.make[F, BrandId].flatMap { id =>
              command.execute(Brand(id, name)).as(id)
            }
          }
        }
    }

  private object BrandSQL {
    import sql.Codecs._

    val codec: Codec[Brand] =
      (brandId ~ brandName).imap {
        case i ~ n => Brand(i, n)
      }(brand => brand.uuid ~ brand.name)

    val selectAll: Query[Void, Brand] =
      sql"""SELECT * FROM brands""".query(codec)

    val insertBrand: Command[Brand] =
      sql"""INSERT INTO brands VALUES ($codec)""".command
  }
}
