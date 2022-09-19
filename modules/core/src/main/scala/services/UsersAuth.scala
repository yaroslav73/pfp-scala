package services

import cats.implicits.{ catsSyntaxAlternativeGuard, catsSyntaxApplicativeId, toFunctorOps }
import cats.{ Applicative, Functor }
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import http.auth.User
import http.auth.User.{ AdminUser, CommonUser }
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser.decode
import pdi.jwt.JwtClaim

import scala.tools.nsc.tasty.SafeEq

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

object UsersAuth {
  def common[F[_]: Functor](redis: RedisCommands[F, String, String]): UsersAuth[F, CommonUser] =
    new UsersAuth[F, CommonUser] {
      def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
        redis
          .get(token.value)
          .map {
            _.flatMap { user =>
              decode[User](user).toOption.map(CommonUser.apply)
            }
          }
    }

  def admin[F[_]: Applicative](adminToken: JwtToken, adminUser: AdminUser): UsersAuth[F, AdminUser] =
    new UsersAuth[F, AdminUser] {

      def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
        (token === adminToken)
          .guard[Option]
          .as(adminUser)
          .pure[F]
    }
}
