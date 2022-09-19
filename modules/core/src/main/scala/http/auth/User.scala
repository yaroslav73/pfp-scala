package http.auth

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import domain.Auth.{ EncryptedPassword, UserId, UserName }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class User(id: UserId, name: UserName)
object User {
  final case class AdminJwtAuth(value: JwtSymmetricAuth)
  final case class UserJwtAuth(value: JwtSymmetricAuth)

  final case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword)

  final case class CommonUser(user: User) {
    def userId: UserId     = user.id
    def userName: UserName = user.name
  }

  final case class AdminUser(user: User)

  implicit val userEncoder: Encoder[User]                         = deriveEncoder[User]
  implicit val userWithPasswordEncoder: Encoder[UserWithPassword] = deriveEncoder[UserWithPassword]
}
