package domain

import domain.Auth.UserId
import squants.Money

final case class Payment(
  id: UserId,
  total: Money,
  card: Card
)
