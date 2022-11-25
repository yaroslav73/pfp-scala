package domain

import cats.kernel.laws.discipline.MonoidTests
import org.scalacheck.Arbitrary
import shop.Generators.moneyGen
import squants.Money
import weaver.FunSuite
import weaver.discipline.Discipline

object OrphanSuite extends FunSuite with Discipline {
  implicit val arbitraryMoney: Arbitrary[Money] = Arbitrary(moneyGen)

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)
}
