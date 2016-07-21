// See LICENSE for license details.

package chiselTests

import org.scalatest._

import chisel3._
import chisel3.testers.BasicTester
import chisel3.util._

class WhenTester() extends BasicTester {
  val cnt = Counter(4)
  when(Bool(true)) { cnt.inc() }

  val out = Wire(UInt.width(3))
  when(cnt.value === UInt.Lit(0)) {
    out := UInt.Lit(1)
  } .elsewhen (cnt.value === UInt.Lit(1)) {
    out := UInt.Lit(2)
  } .elsewhen (cnt.value === UInt.Lit(2)) {
    out := UInt.Lit(3)
  } .otherwise {
    out := UInt.Lit(0)
  }

  assert(out === cnt.value + UInt.Lit(1))

  when(cnt.value === UInt.Lit(3)) {
    stop()
  }
}

class OverlappedWhenTester() extends BasicTester {
  val cnt = Counter(4)
  when(Bool(true)) { cnt.inc() }

  val out = Wire(UInt.width(3))
  when(cnt.value <= UInt.Lit(0)) {
    out := UInt.Lit(1)
  } .elsewhen (cnt.value <= UInt.Lit(1)) {
    out := UInt.Lit(2)
  } .elsewhen (cnt.value <= UInt.Lit(2)) {
    out := UInt.Lit(3)
  } .otherwise {
    out := UInt.Lit(0)
  }

  assert(out === cnt.value + UInt.Lit(1))

  when(cnt.value === UInt.Lit(3)) {
    stop()
  }
}

class WhenSpec extends ChiselFlatSpec {
  "When, elsewhen, and otherwise with orthogonal conditions" should "work" in {
    assertTesterPasses{ new WhenTester }
  }
  "When, elsewhen, and otherwise with overlapped conditions" should "work" in {
    assertTesterPasses{ new OverlappedWhenTester }
  }
}
