package auth

import cats.Monad
import config.Types.{ JwtAccessTokenKeyConfig, TokenExpiration }
import dev.profunktor.auth.jwt.{ JwtSecretKey, JwtToken, jwtEncode }
import effects.GenUUID
import pdi.jwt.{ JwtAlgorithm, JwtClaim }
import cats.syntax.all._
import io.circe.syntax._

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

object Tokens {
  def make[F[_]: GenUUID: Monad](
    jwtExpire: JwtExpire[F],
    config: JwtAccessTokenKeyConfig,
    expiration: TokenExpiration
  ): Tokens[F] = new Tokens[F] {
    def create: F[JwtToken] =
      for {
        uuid      <- GenUUID[F].make
        claim     <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), expiration)
        secretKey = JwtSecretKey(config.secret.value)
        token     <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
  }
}
