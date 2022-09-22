package services

import auth.{ Crypto, Tokens }
import cats.MonadThrow
import cats.implicits.{
  catsSyntaxApplicativeErrorId,
  catsSyntaxApplicativeId,
  catsSyntaxApply,
  toFlatMapOps,
  toFunctorOps
}
import config.Types.TokenExpiration
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import domain.Auth.{ InvalidPassword, Password, UserName, UserNameExist, UserNotFound }
import http.auth.User
import io.circe.syntax.EncoderOps

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

object Auth {
  def make[F[_]: MonadThrow](
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String],
    crypto: Crypto
  ): Auth[F] = new Auth[F] {
    private val TokenExpiration = tokenExpiration.value

    def newUser(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap {
        case Some(_) => UserNameExist(username).raiseError[F, JwtToken]
        case None =>
          for {
            id    <- users.create(username, crypto.encrypt(password))
            token <- tokens.create
            user  = User(id, username).asJson.noSpaces
            _     <- redis.setEx(token.value, user, TokenExpiration)
            _     <- redis.setEx(username.value, token.value, TokenExpiration)
          } yield token
      }

    def login(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap {
        case None => UserNotFound(username).raiseError[F, JwtToken]
        case Some(user) if user.password != crypto.encrypt(password) =>
          InvalidPassword(username).raiseError[F, JwtToken]
        case Some(user) =>
          redis.get(username.value).flatMap {
            case Some(token) => JwtToken(token).pure[F]
            case None =>
              tokens.create.flatTap { token =>
                redis.setEx(token.value, user.asJson.noSpaces, TokenExpiration) *>
                  redis.setEx(username.value, token.value, TokenExpiration)
              }
          }
      }

    def logout(token: JwtToken, username: UserName): F[Unit] =
      redis.del(token.value) *> redis.del(username.value).void
  }
}
