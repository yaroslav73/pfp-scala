package http

import domain.Items.ItemId
import domain.Orders.OrderId
import java.util.UUID
import cats.syntax.either._

object Vars {
  protected class UUIDVar[A](f: UUID => A) {
    def unapply(s: String): Option[A] = Either.catchNonFatal(f(UUID.fromString(s))).toOption
  }

  object ItemIdVar extends UUIDVar(ItemId.apply)
  object OrderIdVar extends UUIDVar(OrderId.apply)
}
