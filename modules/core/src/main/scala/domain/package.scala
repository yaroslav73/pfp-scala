import cats.{ Eq, Monoid, Show }
import domain.Item.ItemId
import io.circe.Decoder.Result
import io.circe.{ Decoder, Encoder, HCursor, Json, KeyDecoder, KeyEncoder }
import squants.market.{ Currency, Money, USD }

import java.util.UUID
import scala.util.Try

package object domain {
  implicit val moneyEncoder: Encoder[Money] = new Encoder[Money] {
    def apply(a: Money): Json =
      Json.obj(
        "value" -> Json.fromBigDecimal(a.amount),
        "currency" -> Json.fromString(a.currency.code),
      )
  }

  // TODO: how to supported other currency?
  implicit val moneyDecoder: Decoder[Money] = new Decoder[Money] {
    def apply(c: HCursor): Result[Money] =
      for {
        value <- c.downField("value").as[BigDecimal]
      } yield Money(value, USD)
  }

  implicit val itemIdKeyEncoder: KeyEncoder[ItemId] = new KeyEncoder[ItemId] {
    def apply(key: ItemId): String = key.value.toString
  }

  implicit val itemIdKeyDecoder: KeyDecoder[ItemId] = new KeyDecoder[ItemId] {
    def apply(key: String): Option[ItemId] = Try(UUID.fromString(key)).map(uuid => ItemId(uuid)).toOption
  }

  implicit val showMoney: Show[Money] = (money: Money) => money.toString

  implicit val eqMoney: Eq[Money] = new Eq[Money] {
    override def eqv(x: Money, y: Money): Boolean =
      x.amount.equals(y.amount) && x.currency.name.equals(y.currency.name)
  }

  implicit val monoidMoney: Monoid[Money] = new Monoid[Money] {
    def empty: Money                       = USD(0)
    def combine(x: Money, y: Money): Money = x + y
  }
}
