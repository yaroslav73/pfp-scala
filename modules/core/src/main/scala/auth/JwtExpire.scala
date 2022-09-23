package auth

import config.Types.TokenExpiration
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, expiration: TokenExpiration): F[JwtClaim]
}
