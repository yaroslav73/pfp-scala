package http.routes.clients

import domain.Orders.PaymentId
import domain.Payment

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
