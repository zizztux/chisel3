// See LICENSE for license details.

package chiselTests

import chisel3._
import chisel3.testers.BasicTester
import scala.collection.immutable.ListMap

trait RecordSpecUtils {
  final class CustomBundle(elts: ListMap[String, Data]) extends Record {
    val elements = for ((field, elt) <- elts) yield field -> elt.chiselCloneType
    def apply(elt: String): Data = elements(elt)
    override def cloneType = (new CustomBundle(elements)).asInstanceOf[this.type]
  }
  class MyBundle extends Bundle {
    val foo = UInt(32.W)
    val bar = UInt(32.W)
    override def cloneType = (new MyBundle).asInstanceOf[this.type]
  }
  val listMap = ListMap("foo" -> UInt(32.W), "bar" -> UInt(32.W))

  class MyModule(output: => Record, input: => Record) extends Module {
    val io = IO(new Bundle {
      val in = Input(input)
      val out = Output(output)
    })
    io.out <> io.in
  }

  class RecordSerializationTest extends BasicTester {
    val recordType = new CustomBundle(ListMap("fizz" -> UInt(16.W), "buzz" -> UInt(16.W)))
    val record = Wire(recordType)
    // Note that "buzz" was added later than "fizz" and is therefore higher order
    record("fizz") := "hdead".U
    record("buzz") := "hbeef".U
    // To UInt
    val uint = record.asUInt
    assert(uint.getWidth == 32) // elaboration time
    assert(uint === "hbeefdead".U)
    // Back to Record
    val record2 = recordType.fromBits(uint)
    assert("hdead".U === record2("fizz").asInstanceOf[UInt])
    assert("hbeef".U === record2("buzz").asInstanceOf[UInt])
    stop()
  }
}

class RecordSpec extends ChiselFlatSpec with RecordSpecUtils {
  behavior of "Record"

  they should "bulk connect similarly to Bundles" in {
    elaborate { new MyModule(new CustomBundle(listMap), new CustomBundle(listMap)) }
  }

  they should "bulk connect to Bundles" in {
    elaborate { new MyModule(new MyBundle, new CustomBundle(listMap)) }
  }

  they should "follow UInt serialization/deserialization API" in {
    assertTesterPasses { new RecordSerializationTest }
  }

  "Bulk connect on Record" should "check that the fields match" in {
    (the [ChiselException] thrownBy {
      elaborate { new MyModule(new CustomBundle(listMap), new CustomBundle(listMap - "foo")) }
    }).getMessage should include ("Right Record missing field")

    (the [ChiselException] thrownBy {
      elaborate { new MyModule(new CustomBundle(listMap - "bar"), new CustomBundle(listMap)) }
    }).getMessage should include ("Left Record missing field")
  }
}
