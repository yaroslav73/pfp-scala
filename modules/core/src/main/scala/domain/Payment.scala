package domain

import domain.Auth.UserId
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import squants.Money

final case class Payment(id: UserId, total: Money, card: Card)

object Payment {
  implicit val paymentEncoder: Encoder[Payment] = deriveEncoder[Payment]
}
