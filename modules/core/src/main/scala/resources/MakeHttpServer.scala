package resources

import cats.effect.{ Async, Resource }
import config.Types.HttpServerConfig
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

trait MakeHttpServer[F[_]] {
  def newEmber(config: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server]
}

object MakeHttpServer {
  def apply[F[_]: MakeHttpServer]: MakeHttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  implicit def forAsyncLogger[F[_]: Async: Logger]: MakeHttpServer[F] = new MakeHttpServer[F] {
    def newEmber(config: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server] =
      EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(httpApp)
        .build
        .evalTap(showEmberBanner)
  }
}
