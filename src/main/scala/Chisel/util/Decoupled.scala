// See LICENSE for license details.

/** Wrappers for ready-valid (Decoupled) interfaces and associated circuit generators using them.
  */

package Chisel

/** An I/O Bundle with simple handshaking using valid and ready signals for data 'bits'*/
class DecoupledIO[+T <: Data](gen: T) extends Bundle
{
  val ready = Input(Bool())
  val valid = Output(Bool())
  val bits  = Output(gen.newType)
  override protected def cloneType: this.type = DecoupledIO(gen).asInstanceOf[this.type]
}

object DecoupledIO {
  /** Adds a ready-valid handshaking protocol to any interface.
    * The standard used is that the consumer uses the flipped interface.
    */
  def apply[T <: Data](gen: T): DecoupledIO[T] = new DecoupledIO(gen)

  implicit class AddMethodsToDecoupled[T<:Data](val target: DecoupledIO[T]) extends AnyVal {
    def firing: Bool = target.ready && target.valid

    /** push dat onto the output bits of this interface to let the consumer know it has happened.
      * @param dat the values to assign to bits.
      * @return    dat.
      */
    def enq(dat: T): T = {
      target.valid := true.asBool
      target.bits := dat
      dat
    }

    /** Indicate no enqueue occurs.  Valid is set to false, and all bits are set to zero.
      */
    def noenq(): Unit = {
      target.valid := false.asBool
      target.bits := target.bits.fromBits(0.asUInt)
    }

    /** Assert ready on this port and return the associated data bits.
      * This is typically used when valid has been asserted by the producer side.
      * @param b ignored
      * @return the data for this device,
      */
    def deq(): T = {
      target.ready := true.asBool
      target.bits
    }

    /** Indicate no dequeue occurs. Ready is set to false
      */
    def nodeq(): Unit = {
      target.ready := false.asBool
    }
  }
}

object EnqIO {
  def apply[T<:Data](gen: T): DecoupledIO[T] = Flipped(DecoupledIO(gen))
}
object DeqIO {
  def apply[T<:Data](gen: T): DecoupledIO[T] = DecoupledIO(gen)
}

/** An I/O Bundle for Queues
  * @param gen The type of data to queue
  * @param entries The max number of entries in the queue */
class QueueIO[T <: Data](gen: T, entries: Int) extends Bundle
{
  /** I/O to enqueue data, is [[Chisel.DecoupledIO]] flipped */
  val enq = EnqIO(gen)
  /** I/O to enqueue data, is [[Chisel.DecoupledIO]]*/
  val deq = DeqIO(gen)
  /** The current amount of data in the queue */
  val count = Output(UInt(log2Up(entries + 1)))
}

/** A hardware module implementing a Queue
  * @param gen The type of data to queue
  * @param entries The max number of entries in the queue
  * @param pipe True if a single entry queue can run at full throughput (like a pipeline). The ''ready'' signals are
  * combinationally coupled.
  * @param flow True if the inputs can be consumed on the same cycle (the inputs "flow" through the queue immediately).
  * The ''valid'' signals are coupled.
  *
  * Example usage:
  *    {{{ val q = new Queue(UInt(), 16)
  *    q.io.enq <> producer.io.out
  *    consumer.io.in <> q.io.deq }}}
  */
class Queue[T <: Data](gen: T, val entries: Int,
                       pipe: Boolean = false,
                       flow: Boolean = false,
                       override_reset: Option[Bool] = None)
extends Module(override_reset=override_reset) {
  def this(gen: T, entries: Int, pipe: Boolean, flow: Boolean, _reset: Bool) =
    this(gen, entries, pipe, flow, Some(_reset))
  
  val io = IO(new QueueIO(gen, entries))

  val ram = Mem(entries, gen)
  val enq_ptr = Counter(entries)
  val deq_ptr = Counter(entries)
  val maybe_full = Reg(init=false.asBool)

  val ptr_match = enq_ptr.value === deq_ptr.value
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full
  val do_enq = Wire(init=io.enq.firing)
  val do_deq = Wire(init=io.deq.firing)

  when (do_enq) {
    ram(enq_ptr.value) := io.enq.bits
    enq_ptr.inc()
  }
  when (do_deq) {
    deq_ptr.inc()
  }
  when (do_enq != do_deq) {
    maybe_full := do_enq
  }

  io.deq.valid := !empty
  io.enq.ready := !full
  io.deq.bits := ram(deq_ptr.value)

  if (flow) {
    when (io.enq.valid) { io.deq.valid := true.asBool }
    when (empty) {
      io.deq.bits := io.enq.bits
      do_deq := false.asBool
      when (io.deq.ready) { do_enq := false.asBool }
    }
  }

  if (pipe) {
    when (io.deq.ready) { io.enq.ready := true.asBool }
  }

  val ptr_diff = enq_ptr.value - deq_ptr.value
  if (isPow2(entries)) {
    io.count := Cat(maybe_full && ptr_match, ptr_diff)
  } else {
    io.count := Mux(ptr_match,
                    Mux(maybe_full,
                      UInt(entries), 0.asUInt),
                    Mux(deq_ptr.value > enq_ptr.value,
                      UInt(entries) + ptr_diff, ptr_diff))
  }
}

/** Generic hardware queue. Required parameter entries controls
  the depth of the queues. The width of the queue is determined
  from the inputs.

  Example usage:
     {{{ val q = Queue(DecoupledIO(UInt()), 16)
     q.io.enq <> producer.io.out
     consumer.io.in <> q.io.deq }}}
  */
object Queue
{
  def apply[T <: Data](enq: DecoupledIO[T], entries: Int = 2, pipe: Boolean = false): DecoupledIO[T]  = {
    val q = Module(new Queue(enq.bits.newType, entries, pipe))
    q.io.enq.valid := enq.valid // not using <> so that override is allowed
    q.io.enq.bits := enq.bits
    enq.ready := q.io.enq.ready
    TransitName(q.io.deq, q)
  }
}
