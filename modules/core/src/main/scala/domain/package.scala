import domain.Item.ItemId
import io.circe.{ Encoder, Json, KeyDecoder, KeyEncoder }
import squants.market.Money

import java.util.UUID
import scala.util.Try

package object domain {
  implicit val moneyEncoder: Encoder[Money] = new Encoder[Money] {
    override def apply(a: Money): Json =
      Json.obj(
        "value" -> Json.fromBigDecimal(a.amount),
        "currency" -> Json.fromString(a.currency.code),
      )
  }
  implicit val itemIdKeyEncoder: KeyEncoder[ItemId] = new KeyEncoder[ItemId] {
    override def apply(key: ItemId): String = key.value.toString
  }
  implicit val itemIdKeyDecoder: KeyDecoder[ItemId] = new KeyDecoder[ItemId] {
    override def apply(key: String): Option[ItemId] = Try(UUID.fromString(key)).map(uuid => ItemId(uuid)).toOption
  }
}
