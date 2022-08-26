package domain

import domain.Categories.{ Category, CategoryId, CategoryName }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.util.UUID

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]
}

object Categories {
  case class CategoryId(value: UUID)
  case class CategoryName(value: String)

  final case class Category(uuid: CategoryId, name: CategoryName)
  object Category {
    implicit val categoryNameEncoder: Encoder[CategoryName] = deriveEncoder[CategoryName]
    implicit val categoryIdEncoder: Encoder[CategoryId]     = deriveEncoder[CategoryId]
    implicit val categoryEncoder: Encoder[Category]         = deriveEncoder[Category]
  }
}
