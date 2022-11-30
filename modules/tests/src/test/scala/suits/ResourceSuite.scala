package suits

import cats.effect.IO
import cats.syntax.flatMap._
import cats.effect.kernel.Resource
import weaver.{ Expectations, IOSuite }
import weaver.scalacheck.{ CheckConfig, Checkers }

abstract class ResourceSuite extends IOSuite with Checkers {
  // For it:tests, one test is enough
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1)

  implicit class SharedResourceOps(res: Resource[IO, Res]) {
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))
  }

  def testBeforeAfterEach(
    before: Res => IO[Unit],
    after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    name =>
      fa =>
        test(name) { res =>
          before(res) >> fa(res).guarantee(after(res))
        }

  def testBeforeEach(before: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(before, _ => IO.unit)

  def testAfterEach(after: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(_ => IO.unit, after)
}
