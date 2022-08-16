package domain

import domain.Auth.UserId
import domain.Users.{ UserName, UserWithPassword }
import io.estatico.newtype.macros.newtype

import java.util.UUID

trait Users[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName): F[UserId]
}

object Users {
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)
  @newtype case class EncryptedPassword(value: String)

  case class UserWithPassword(
    id: UserId,
    name: UserName,
    password: EncryptedPassword
  )
}
