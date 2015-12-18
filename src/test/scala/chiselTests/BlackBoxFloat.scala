//// See LICENSE for license details.

package chiselTests

import java.io.File
import org.scalatest._
import Chisel._
import Chisel.testers.BasicTester

class BBFX extends BlackBox {
  val io = new Bundle() {
    val out = UInt(OUTPUT, 64)
  }
}

class BBFZero extends BBFX
class BBFOne extends BBFX
class BBFTwo extends BBFX
class BBFThree extends BBFX
class BBFFour extends BBFX
class BBFSix extends BBFX

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

class BlackBoxFloatTester extends BasicTester {
  val mzero = Module(new BBFZero())
  val zero = mzero.io.out
  val mone = Module(new BBFOne())
  val one = mone.io.out
  val mtwo = Module(new BBFTwo())
  val two = mtwo.io.out
  val mthree = Module(new BBFThree())
  val three = mthree.io.out
  val mfour = Module(new BBFFour())
  val four = mfour.io.out
  val msix = Module(new BBFSix())
  val six = msix.io.out

  val (cnt, done) = Counter(Bool(true), 4)
  val accum = Reg(init=UInt(0, width=64))

  val adder = Module(new BBFAdder())
  adder.io.in1 := accum
  adder.io.in2 := one

  val mult = Module(new BBFMult())
  mult.io.in1 := adder.io.out
  mult.io.in2 := two

  accum := adder.io.out

  printf("%x    a: %x + %x => %x    m: %x * %x => %x",
      accum,
      adder.io.in1, adder.io.in2, adder.io.out,
      mult.io.in1, mult.io.in2, mult.io.out)

  when (cnt === UInt(0)) {
    assert(adder.io.out === one)
    assert(mult.io.out === two)
  } .elsewhen (cnt === UInt(1)) {
    assert(adder.io.out === two)
    assert(mult.io.out === four)
  }
  when (done) {
    stop()
  }
}

class BlackBoxFloatSpec extends ChiselFlatSpec {
  "A BlackBoxed FP block" should "work" in {
    assertTesterPasses({ new BlackBoxFloatTester },
        Seq("/BlackBoxFloat.v"))
  }
}
