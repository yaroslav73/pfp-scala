package auth

import dev.profunktor.auth.jwt.JwtToken

trait Tokens[F[_]] {
  def create: F[JwtToken]
}
