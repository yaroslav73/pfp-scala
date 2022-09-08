package domain

import domain.Card.{ CVV, CardHolder, CardNumber, Expiration }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import ext.Refined._
import io.circe.Decoder
import io.circe.refined._
import io.circe.generic.semiauto.deriveDecoder

final case class Card(
  name: CardHolder,
  number: CardNumber,
  expiration: Expiration,
  cvv: CVV
)

object Card {
  type Rgx                     = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"
  type CardHolderPredicate     = String Refined MatchesRegex[Rgx]
  type CardNumberPredicate     = Long Refined Size[16]
  type CardExpirationPredicate = String Refined (Size[4] And ValidInt)
  type CardCVVPredicate        = Int Refined Size[3]

  final case class CardHolder(value: CardHolderPredicate)

  final case class CardNumber(value: CardNumberPredicate)

  final case class Expiration(value: CardExpirationPredicate)

  final case class CVV(value: CardCVVPredicate)

  implicit val cardHolderDecoder: Decoder[CardHolder]     = deriveDecoder[CardHolder]
  implicit val cardNumberDecoder: Decoder[CardNumber]     = decoderOf[Long, Size[16]].map(CardNumber)
  implicit val cardExpirationDecoder: Decoder[Expiration] = decoderOf[String, Size[4] And ValidInt].map(Expiration)
  implicit val cvvDecoder: Decoder[CVV]                   = decoderOf[Int, Size[3]].map(CVV)
  implicit val cardDecoder: Decoder[Card]                 = deriveDecoder[Card]
}
