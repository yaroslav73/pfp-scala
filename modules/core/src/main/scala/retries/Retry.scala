package retries

import cats.effect.Temporal
import org.typelevel.log4cats.Logger
import retry.{ RetryDetails, RetryPolicy, retryingOnAllErrors }

trait Retry[F[_]] {
  def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A]
}

object Retry {
  def apply[F[_]: Retry]: Retry[F] = implicitly

  implicit def forLoggerTemporal[F[_]: Logger: Temporal]: Retry[F] = new Retry[F] {
    def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A] = {
      def onError(e: Throwable, details: RetryDetails): F[Unit] = {
        details match {
          case RetryDetails.WillDelayAndRetry(_, retriesSoFar, _) =>
            Logger[F].error(s"Failed on: $retriable with ${e.getMessage}. Retried $retriesSoFar times.")
          case RetryDetails.GivingUp(totalRetries, _) =>
            Logger[F].error(s"Giving up on: $retriable after $totalRetries retries.")
        }
      }

      retryingOnAllErrors[A](policy, onError)(fa)
    }
  }
}
