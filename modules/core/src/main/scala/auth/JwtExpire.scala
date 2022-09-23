package auth

import cats.effect.kernel.Sync
import cats.implicits.toFunctorOps
import config.Types.TokenExpiration
import effects.JwtClock
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, expiration: TokenExpiration): F[JwtClaim]
}

object JwtExpire {
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc.map { implicit jwtClock =>
      new JwtExpire[F] {
        def expiresIn(claim: JwtClaim, expiration: TokenExpiration): F[JwtClaim] =
          Sync[F].delay(claim.issuedNow.expiresIn(expiration.value.toMillis))
      }
    }
}
