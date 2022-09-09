package services

import domain.Categories.{ Category, CategoryId, CategoryName }

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]
}
