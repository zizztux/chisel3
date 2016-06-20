// See LICENSE for license details.

package chiselTests

import org.scalatest._
import Chisel._
import Chisel.testers.BasicTester

class LastAssignTester() extends BasicTester {
  val cnt = Counter(2)

  val test = Wire(UInt(width=4))
  assert(test === UInt.Lit(7))  // allow read references before assign references

  test := UInt.Lit(13)
  assert(test === UInt.Lit(7))  // output value should be position-independent

  test := UInt.Lit(7)
  assert(test === UInt.Lit(7))  // this obviously should work

  when(cnt.value === UInt.Lit(1)) {
    stop()
  }
}

class ReassignmentTester() extends BasicTester {
  val test = UInt.Lit(15)
  test := UInt.Lit(7)
}

class MultiAssignSpec extends ChiselFlatSpec {
  "The last assignment" should "be used when multiple assignments happen" in {
    assertTesterPasses{ new LastAssignTester }
  }
  intercept[Chisel.internal.ChiselException] {
//    "Reassignments to non-wire types" should "be disallowed" in {
    assertTesterFails{ new ReassignmentTester }
  }
}
