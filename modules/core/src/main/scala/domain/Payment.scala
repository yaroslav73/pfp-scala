package domain

import domain.Auth.UserId
import domain.Payment.Card
import io.estatico.newtype.macros.newtype
import squants.Money

final case class Payment(
  id: UserId,
  total: Money,
  card: Card
)

object Payment {
  @newtype case class CardHolder(value: String)
  @newtype case class CardNumber(value: String)
  @newtype case class Expiration(value: String)
  @newtype case class CVV(value: String)

  final case class Card(
    name: CardHolder,
    number: CardNumber,
    expiration: Expiration,
    cvv: CVV
  )
}
