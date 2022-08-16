package domain

import domain.Orders.PaymentId

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
