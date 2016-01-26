//// See LICENSE for license details.

package chiselTests

import java.io.File
import org.scalatest._
import Chisel._
import Chisel.testers.BasicTester

class BBFAdder extends BlackBox {
  val io = new Bundle() {
    val in1 = UInt(INPUT, 64)
    val in2 = UInt(INPUT, 64)
    val out = UInt(OUTPUT, 64)
  }
}

class BBFMult extends BlackBox {
  val io = new Bundle() {
    val in1 = UInt(INPUT, 64)
    val in2 = UInt(INPUT, 64)
    val out = UInt(OUTPUT, 64)
  }
}

object BBFConst {
  def apply(value: Double): UInt = {
    UInt(java.lang.Double.doubleToRawLongBits(value), width=64);
  }
}

class BlackBoxFloatTester extends BasicTester {
  val (cnt, _) = Counter(Bool(true), 4)
  val accum = Reg(init=BBFConst(0.0))

  val adder = Module(new BBFAdder())
  adder.io.in1 := accum
  adder.io.in2 := BBFConst(1.0)

  val mult = Module(new BBFMult())
  mult.io.in1 := adder.io.out
  mult.io.in2 := BBFConst(2.0)

  accum := adder.io.out

  printf("%x    add: %x + %x => %x    mult: %x * %x => %x\n",
      accum,
      adder.io.in1, adder.io.in2, adder.io.out,
      mult.io.in1, mult.io.in2, mult.io.out)

  when (cnt === UInt(0)) {
    assert(adder.io.out === BBFConst(1))
    assert(mult.io.out === BBFConst(2))
  } .elsewhen (cnt === UInt(1)) {
    assert(adder.io.out === BBFConst(2))
    assert(mult.io.out === BBFConst(4))
  } .elsewhen (cnt === UInt(2)) {
    assert(adder.io.out === BBFConst(3))
    assert(mult.io.out === BBFConst(6))
  } .elsewhen (cnt === UInt(3)) {
    assert(adder.io.out === BBFConst(4))
    assert(mult.io.out === BBFConst(8))
    stop()
  }
}

class BlackBoxFloatSpec extends ChiselFlatSpec {
  "A BlackBoxed FP block" should "work" in {
    assertTesterPasses({ new BlackBoxFloatTester },
        Seq("/BlackBoxFloat.v"))
  }
}
