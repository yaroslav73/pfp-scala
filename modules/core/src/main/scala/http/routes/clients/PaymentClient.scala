package http.routes.clients

import cats.effect.kernel.MonadCancelThrow
import cats.implicits.{ catsSyntaxApplicativeErrorId, catsSyntaxEither, toFlatMapOps }
import config.Types.PaymentConfig
import domain.Order.{ PaymentError, PaymentId }
import domain.Payment
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{ JsonDecoder, toMessageSyntax }
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{ Status, Uri }

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

object PaymentClient {
  def make[F[_]: JsonDecoder: MonadCancelThrow](config: PaymentConfig, client: Client[F]): PaymentClient[F] = {
    new PaymentClient[F] with Http4sClientDsl[F] {
      def process(payment: Payment): F[PaymentId] =
        Uri
          .fromString(config.uri.value.toString() + "/payments")
          .liftTo[F]
          .flatMap { uri =>
            client.run(POST(payment, uri)).use { resp =>
              resp.status match {
                case Status.Ok | Status.Conflict =>
                  resp.asJsonDecode[PaymentId]
                case status =>
                  PaymentError(Option(status.reason).getOrElse("unknown")).raiseError[F, PaymentId]
              }
            }
          }
    }
  }
}
