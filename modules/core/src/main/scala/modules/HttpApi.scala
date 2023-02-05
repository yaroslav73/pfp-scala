package modules

import cats.effect.kernel.Async
import cats.implicits.toSemigroupKOps
import dev.profunktor.auth.JwtAuthMiddleware
import http.auth.User.{ AdminUser, CommonUser }
import http.routes.admin.{ AdminBrandRoutes, AdminCategoryRoutes, AdminItemRoutes }
import http.routes.{ BrandRoutes, CategoryRoutes, HealthRoutes, ItemRoutes, Version }
import http.routes.auth.{ LoginRoutes, LogoutRoutes, UserRoutes }
import http.routes.secured.{ CartRoutes, CheckoutRoutes, OrderRoutes }
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._

import scala.concurrent.duration.DurationInt

sealed abstract class HttpApi[F[_]: Async] private (
  services: Services[F],
  programs: Programs[F],
  security: Security[F],
) {
  private val adminMiddleware =
    JwtAuthMiddleware[F, AdminUser](security.adminJwtAuth.value, security.adminAuth.findUser)

  private val userMiddleware =
    JwtAuthMiddleware[F, CommonUser](security.userJwtAuth.value, security.userAuth.findUser)

  // Auth routes
  private val loginRoutes  = LoginRoutes[F](security.auth).routes
  private val logoutRoutes = LogoutRoutes[F](security.auth).routes(userMiddleware)
  private val userRoutes   = UserRoutes[F](security.auth).routes

  // Open routes
  private val healthRoutes   = HealthRoutes[F](services.healthCheck).routes
  private val brandRoutes    = BrandRoutes[F](services.brands).routes
  private val categoryRoutes = CategoryRoutes[F](services.categories).routes
  private val itemRoutes     = ItemRoutes[F](services.items).routes

  // Secure routes
  private val cartRoutes     = CartRoutes[F](services.shoppingCart).routes(userMiddleware)
  private val checkoutRoutes = CheckoutRoutes[F](programs.checkout).routes(userMiddleware)
  private val orderRoutes    = OrderRoutes[F](services.orders).routes(userMiddleware)

  // Admin routes
  private val adminItemRoutes     = AdminItemRoutes[F](services.items).routes(adminMiddleware)
  private val adminBrandRoutes    = AdminBrandRoutes[F](services.brands).routes(adminMiddleware)
  private val adminCategoryRoutes = AdminCategoryRoutes[F](services.categories).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    loginRoutes <+> logoutRoutes <+> userRoutes <+>
      healthRoutes <+> brandRoutes <+> categoryRoutes <+>
      itemRoutes <+> cartRoutes <+> checkoutRoutes <+> orderRoutes

  private val adminRoutes: HttpRoutes[F] = adminItemRoutes <+> adminBrandRoutes <+> adminCategoryRoutes

  private val routes: HttpRoutes[F] = Router(
    Version.v1 -> openRoutes,
    Version.v1 + "/admin" -> adminRoutes,
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}

object HttpApi {
  def make[F[_]: Async](
    services: Services[F],
    programs: Programs[F],
    security: Security[F],
  ): HttpApi[F] = new HttpApi[F](services, programs, security) {}
}
