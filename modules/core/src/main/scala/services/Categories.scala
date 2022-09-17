package services

import domain.Category
import domain.Category.{ CategoryId, CategoryName }

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]
}
