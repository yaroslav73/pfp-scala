package retries

import cats.effect.{ IO, Ref }
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.{ RetryDetails, RetryPolicy, retryingOnAllErrors }

import scala.annotation.nowarn
import scala.reflect.ClassTag

object TestRetry {
  def givingUp(ref: Ref[IO, Option[GivingUp]]): Retry[IO] = handleFor[GivingUp](ref)

  def recovering(ref: Ref[IO, Option[WillDelayAndRetry]]): Retry[IO] = handleFor[WillDelayAndRetry](ref)

  private[retries] def handleFor[A <: RetryDetails: ClassTag](ref: Ref[IO, Option[A]]): Retry[IO] = new Retry[IO] {
    def retry[T](policy: RetryPolicy[IO], retriable: Retriable)(fa: IO[T]): IO[T] = {
      @nowarn
      def onError(e: Throwable, details: RetryDetails): IO[Unit] = {
        details match {
          case a: A => ref.set(Option(a))
          case _    => IO.unit
        }
      }

      retryingOnAllErrors[T](policy, onError)(fa)
    }
  }
}
