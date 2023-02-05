import cats.effect._
import cats.effect.std.Supervisor
import config.Config
import dev.profunktor.redis4cats.log4cats._
import modules.{ HttpApi, HttpClients, Programs, Security, Services }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import resources.{ AppResources, MakeHttpServer }

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  def run: IO[Unit] =
    Config[IO].flatMap { config =>
      Logger[IO].info(s"Loaded config $config") >>
        Supervisor[IO].use { implicit sp =>
          AppResources
            .make[IO](config)
            .evalMap { resources =>
              Security
                .make[IO](config, resources.postgres, resources.redis)
                .map { security =>
                  val clients  = HttpClients.make[IO](config.paymentConfig, resources.client)
                  val services = Services.make[IO](resources.redis, resources.postgres, config.shoppingCartExpiration)
                  val programs = Programs.make[IO](config.checkoutConfig, services, clients)
                  val api      = HttpApi.make[IO](services, programs, security)
                  config.httpServerConfig -> api.httpApp
                }
            }
            .flatMap {
              case (config, httpApp) => MakeHttpServer[IO].newEmber(config, httpApp)
            }
            .useForever
        }
    }
}
