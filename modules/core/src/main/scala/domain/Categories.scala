package domain

import domain.Categories.{ Category, CategoryId, CategoryName }
import io.estatico.newtype.macros.newtype

import java.util.UUID

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]
}

object Categories {
  @newtype case class CategoryId(value: UUID)
  @newtype case class CategoryName(value: String)

  final case class Category(uuid: CategoryId, name: CategoryName)
}
