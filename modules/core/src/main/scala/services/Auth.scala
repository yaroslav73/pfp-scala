package services

import dev.profunktor.auth.jwt.JwtToken
import domain.Auth.{ Password, User, UserName }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

object Auth {}
