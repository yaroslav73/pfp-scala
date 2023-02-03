package modules

import cats.effect.kernel.MonadCancelThrow
import config.Types.PaymentConfig
import http.routes.clients.PaymentClient
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client

trait HttpClients[F[_]] {
  def paymentClient: PaymentClient[F]
}

object HttpClients {
  def make[F[_]: JsonDecoder: MonadCancelThrow](config: PaymentConfig, client: Client[F]): HttpClients[F] =
    new HttpClients[F] {
      def paymentClient: PaymentClient[F] = PaymentClient.make(config, client)
    }
}
