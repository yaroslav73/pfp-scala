package optics

import monocle.Iso

import java.util.UUID

trait IsUUID[A] {
  def _UUID: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit def identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    def _UUID: Iso[UUID, UUID] = Iso[UUID, UUID](identity)(identity)
  }
}
