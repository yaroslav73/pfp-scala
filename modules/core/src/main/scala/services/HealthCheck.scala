package services

import domain.HealthCheck.AppStatus

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}
