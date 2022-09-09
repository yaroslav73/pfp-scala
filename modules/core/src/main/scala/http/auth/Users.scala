package http.auth

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import domain.Auth.{EncryptedPassword, UserId, UserName}

object Users {
  final case class AdminJwtAuth(value: JwtSymmetricAuth)
  final case class UserJwtAuth(value: JwtSymmetricAuth)

  final case class User(id: UserId, name: UserName)
  final case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword)

  final case class CommonUser(user: User) {
    def userId: UserId = user.id
  }
  final case class AdminUser(user: User)
}
