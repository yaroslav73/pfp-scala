package services

import dev.profunktor.auth.jwt.JwtToken
import domain.Auth.User

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken): F[Option[User]]
}
