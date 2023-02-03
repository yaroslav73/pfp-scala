package modules

import auth.{ Crypto, JwtExpire, Tokens }
import cats.ApplicativeThrow
import cats.effect._
import cats.syntax.all._
import config.Types.AppConfig
import dev.profunktor.auth.jwt.{ JwtAuth, JwtToken, jwtDecode }
import dev.profunktor.redis4cats.RedisCommands
import domain.Auth.{ ClaimContent, UserId, UserName }
import http.auth.User
import http.auth.User.{ AdminJwtAuth, AdminUser, CommonUser, UserJwtAuth }
import pdi.jwt.JwtAlgorithm
import io.circe.parser.{ decode => jsonDecode }
import services.{ Auth, Users, UsersAuth }
import skunk.Session

sealed abstract class Security[F[_]] private (
  val auth: Auth[F],
  val adminAuth: UsersAuth[F, AdminUser],
  val userAuth: UsersAuth[F, CommonUser],
  val adminJwtAuth: AdminJwtAuth,
  val userJwtAuth: UserJwtAuth,
)

object Security {
  def make[F[_]: Sync](
    config: AppConfig,
    postgres: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {
    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(
        JwtAuth.hmac(
          config.adminJwtConfig.jwtSecretKey.value.secret.value,
          JwtAlgorithm.HS256,
        )
      )

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(
        JwtAuth.hmac(
          config.jwtAccessTokenKeyConfig.value.secret.value,
          JwtAlgorithm.HS256,
        )
      )

    val adminToken = JwtToken(config.adminJwtConfig.adminUserToken.value.secret.value)

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content    <- ApplicativeThrow[F].fromEither(jsonDecode[ClaimContent](adminClaim.content))
      adminUser  = AdminUser(User(UserId(content.uuid), UserName("admin")))
      crypto     <- Crypto.make[F](config.passwordSalt.value)
      tokens <- JwtExpire
        .make[F]
        .map(jwtExpire => Tokens.make[F](jwtExpire, config.jwtAccessTokenKeyConfig.value, config.tokenExpiration))
      users     = Users.make[F](postgres)
      auth      = Auth.make[F](config.tokenExpiration, tokens, users, redis, crypto)
      adminAuth = UsersAuth.admin[F](adminToken, adminUser)
      userAuth  = UsersAuth.common[F](redis)
    } yield new Security[F](auth, adminAuth, userAuth, adminJwtAuth, userJwtAuth) {}
  }
}
