package resources

import cats.effect.{ Async, Resource }
import config.Types.HttpClientConfig
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait MakeHttpClient[F[_]] {
  def newEmber(config: HttpClientConfig): Resource[F, Client[F]]
}

object MakeHttpClient {
  def apply[F[_]: MakeHttpClient]: MakeHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: MakeHttpClient[F] = new MakeHttpClient[F] {
    def newEmber(config: HttpClientConfig): Resource[F, Client[F]] =
      EmberClientBuilder
        .default[F]
        .withTimeout(config.timeout)
        .withIdleTimeInPool(config.idleTimeInPool)
        .build
  }
}
