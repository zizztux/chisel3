// See LICENSE for license details.

package chiselTests

import org.scalatest._
import Chisel._
import Chisel.testers.BasicTester

class OptionBundle(hasIn: Boolean) extends Bundle {
  val in = if (hasIn) {
    Some(Input(Bool()))
  } else {
    None
  }
  val out = Output(Bool())
}

class OptionBundleModule(hasIn: Boolean) extends Module {
  val io = IO(new OptionBundle(hasIn))
  if (hasIn) {
    io.out := io.in.get
  } else {
    io.out := false.asBool
  }
}

class SomeOptionBundleTester(expected: Boolean) extends BasicTester {
  val mod = Module(new OptionBundleModule(true))
  mod.io.in.get := expected.asBool
  assert(mod.io.out === expected.asBool)
  stop()
}

class NoneOptionBundleTester() extends BasicTester {
  val mod = Module(new OptionBundleModule(false))
  assert(mod.io.out === false.asBool)
  stop()
}

class InvalidOptionBundleTester() extends BasicTester {
  val mod = Module(new OptionBundleModule(false))
  mod.io.in.get := true.asBool
  assert(false.asBool)
  stop()
}

class OptionBundleSpec extends ChiselFlatSpec {
  "A Bundle with an Option field" should "work properly if the Option field is not None" in {
    assertTesterPasses { new SomeOptionBundleTester(true) }
    assertTesterPasses { new SomeOptionBundleTester(false) }
  }

  "A Bundle with an Option field" should "compile if the Option field is None" in {
    assertTesterPasses { new NoneOptionBundleTester() }
  }

  "A Bundle with an Option field" should "assert out accessing a None Option field" in {
    a [Exception] should be thrownBy {
      elaborate { new InvalidOptionBundleTester() }
    }
  }
}
