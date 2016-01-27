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

object DConst {
  def apply(value: Double): UInt = {
    UInt(java.lang.Double.doubleToRawLongBits(value), width=64);
  }
}

object DMult {
  def apply(op1: UInt, op2:UInt): UInt = {
    val mult = Module(new BBFMult())
    mult.io.in1 := op1
    mult.io.in2 := op2
    mult.io.out
  }
}

object DAdd {
  def apply(op1: UInt, op2:UInt): UInt = {
    val mult = Module(new BBFAdder())
    mult.io.in1 := op1
    mult.io.in2 := op2
    mult.io.out
  }
}

class BlackBoxFloatTester extends BasicTester {
  val (cnt, _) = Counter(Bool(true), 10)
  val accum = Reg(init=DConst(0.0))

  val addOut = DAdd(accum, DConst(1.0))
  val mulOut = DMult(addOut, DConst(2.0))

  accum := addOut

  printf("cnt: %x     accum: %x    add: %x    mult: %x\n",
      cnt, accum, addOut, mulOut)

  when (cnt === UInt(0)) {
    assert(addOut === DConst(1))
    assert(mulOut === DConst(2))
  } .elsewhen (cnt === UInt(1)) {
    assert(addOut === DConst(2))
    assert(mulOut === DConst(4))
  } .elsewhen (cnt === UInt(2)) {
    assert(addOut === DConst(3))
    assert(mulOut === DConst(6))
  } .elsewhen (cnt === UInt(3)) {
    assert(addOut === DConst(4))
    assert(mulOut === DConst(8))
  }

  when (cnt >= UInt(3)) {
    // for unknown reasons, stop needs to be invoked multiple times
    stop()
  }
}

class BlackBoxFloatSpec extends ChiselFlatSpec {
  "A BlackBoxed FP block" should "work" in {
    assertTesterPasses({ new BlackBoxFloatTester },
        Seq("/BlackBoxFloat.v"))
  }
}
