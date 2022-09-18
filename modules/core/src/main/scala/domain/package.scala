import cats.Monoid
import domain.Item.ItemId
import io.circe.{ Encoder, Json, KeyDecoder, KeyEncoder }
import squants.market.{ Money, USD }

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
  implicit val itemIdKeyEncoder: KeyEncoder[ItemId] = new KeyEncoder[ItemId] {
    def apply(key: ItemId): String = key.value.toString
  }
  implicit val itemIdKeyDecoder: KeyDecoder[ItemId] = new KeyDecoder[ItemId] {
    def apply(key: String): Option[ItemId] = Try(UUID.fromString(key)).map(uuid => ItemId(uuid)).toOption
  }

  // TODO: Add law tests for monoid instance.
  implicit val monoidMoney: Monoid[Money] = new Monoid[Money] {
    def empty: Money = USD(0)

    def combine(x: Money, y: Money): Money = x + y
  }
}
