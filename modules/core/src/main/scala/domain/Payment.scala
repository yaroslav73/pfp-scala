package domain

import cats.Show
import domain.Auth.UserId
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import squants.Money

final case class Payment(id: UserId, total: Money, card: Card)

object Payment {
  implicit val showPayment: Show[Payment]       = (payment: Payment) => payment.toString
  implicit val paymentEncoder: Encoder[Payment] = deriveEncoder[Payment]
}
