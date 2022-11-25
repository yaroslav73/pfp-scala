package domain

import domain.Brand.BrandId
import domain.HealthCheck.Status
import monocle.law.discipline.IsoTests
import optics.IsUUID
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import shop.Generators.brandIdGen
import weaver.FunSuite
import weaver.discipline.Discipline

import java.util.UUID

object OpticsSuite extends FunSuite with Discipline {
  implicit val arbitraryStatus: Arbitrary[Status] = Arbitrary(Gen.oneOf(Status.Okay, Status.Unreachable))

  implicit val arbitraryBrandId: Arbitrary[BrandId] = Arbitrary(brandIdGen)

  implicit val cogenBrandId: Cogen[BrandId] = Cogen[UUID].contramap[BrandId](_.value)

  checkAll("Iso[Status._bool]", IsoTests(Status._bool))

  // Bonus check
  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]._UUID))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]._UUID))
}
