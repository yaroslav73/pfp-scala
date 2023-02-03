package modules

import cats.effect.Temporal
import cats.implicits.catsSyntaxSemigroup
import config.Types.CheckoutConfig
import effects.Background
import org.typelevel.log4cats.Logger
import programs.Checkout
import retry.RetryPolicy
import retry.RetryPolicies.{ exponentialBackoff, limitRetries }

sealed abstract class Programs[F[_]: Background: Logger: Temporal] private (
  config: CheckoutConfig,
  services: Services[F],
  clients: HttpClients[F],
) {
  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](config.retriesLimit.value) |+|
      exponentialBackoff[F](config.retriesBackoff)

  val checkout: Checkout[F] = Checkout[F](clients.paymentClient, services.shoppingCart, services.orders, retryPolicy)
}

object Programs {
  def make[F[_]: Background: Logger: Temporal](
    checkoutConfig: CheckoutConfig,
    services: Services[F],
    clients: HttpClients[F],
  ): Programs[F] = new Programs[F](checkoutConfig, services, clients) {}
}
