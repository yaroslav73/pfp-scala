package domain

import cats.Functor
import cats.implicits.toFunctorOps
import effects.GenUUID
import optics.IsUUID

object ID {
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].make.map(uuid => IsUUID[A]._UUID.get(uuid))

  def read[F[_]: Functor: GenUUID, A: IsUUID](s: String): F[A] =
    GenUUID[F].read(s).map(uuid => IsUUID[A]._UUID.get(uuid))
}
