package services

import domain.Auth.{UserId, UserName}
import http.auth.Users.UserWithPassword

trait Users[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName): F[UserId]
}
