// See LICENSE for license details.

package chisel3.testers

import chisel3._
import chisel3.internal.HasId
import chisel3.internal.firrtl._

import scala.collection.mutable.{ArrayBuffer, HashMap}

object getDataNames {
  /** Given a Chisel object and its name, return its elements with their run-time names.
    * @param name the string name (used to generate element names)
    * @param data the object
    * @return Seq[(Data, String)]
    */
  def apply(name: String, data: Data): Seq[(Data, String)] = data match {
    case b: Element => Seq(b -> name)
    case b: Bundle => b.elements.toSeq flatMap {case (n, e) => apply(s"${name}_$n", e)}
    case v: Vec[_] => v.zipWithIndex flatMap {case (e, i) => apply(s"${name}_$i", e)}
  }
  /** Given a Chisel Module return its io elements with their run-time names.
    * @param dut the Chisel Module
    * @return Seq[(Data, String)]
    */
  def apply(dut: Module): Seq[(Data, String)] = apply("io", dut.io)
}

object getPorts {
  /** Given a Chisel Module return its io elements as a partitioned sequence of inputs and outputs.
    * @param dut the Chisel Module
    * @return (Seq[Data], Seq[Data])
    */
  def apply(dut: Module) = getDataNames(dut).unzip._1 partition (_.dir == INPUT)
}

object validName {
  /** Given a potential run-time name, append a '$' to it if it corresponds to a firrtl keyword.
    *
    * @param name run-time name
    * @return keyword-protected name
    */
  def apply(name: String) =
    if (firrtl.Utils.v_keywords contains name) name + "$" else name
}

class CircuitGraph {
  protected val _modParent = HashMap[Module, Module]()
  protected val _nodeParent = HashMap[HasId, Module]()
  protected val _modToName = HashMap[Module, String]()
  protected val _nodeToName = HashMap[HasId, String]()
  protected val _nodes = ArrayBuffer[HasId]()
  protected var module: Option[Module] = None
  def getModule: Module = module.get

  def construct(modN: String, components: Seq[Component]): Module = {
    val component = (components find (_.name == modN)).get
    val mod = component.id
    module = Some(mod)

    getDataNames(mod) foreach {case (port, name) =>
    // _nodes += port
    _nodeParent(port) = mod
    _nodeToName(port) = validName(name)
    }

    component.commands foreach {
      case inst: DefInstance =>
        val child = construct(validName(inst.id.name), components)
        _modParent(child) = mod
        _modToName(child) = inst.name
      case reg: DefReg if reg.name.slice(0, 2) != "T_" =>
        getDataNames(reg.name, reg.id) foreach { case (data, name) =>
          _nodes += data
          _nodeParent(data) = mod
          _nodeToName(data) = validName(name)
        }
      case reg: DefRegInit if reg.name.slice(0, 2) != "T_" =>
        getDataNames(reg.name, reg.id) foreach { case (data, name) =>
          _nodes += data
          _nodeParent(data) = mod
          _nodeToName(data) = validName(name)
        }
      case wire: DefWire if wire.name.slice(0, 2) != "T_" =>
        getDataNames(wire.name, wire.id) foreach { case (data, name) =>
          // _nodes += data
          _nodeParent(data) = mod
          _nodeToName(data) = validName(name)
        }
      case prim: DefPrim[_] if prim.name.slice(0, 2) != "T_" =>
        getDataNames(prim.name, prim.id) foreach { case (data, name) =>
          // _nodes += data
          _nodeParent(data) = mod
          _nodeToName(data) = validName(name)
        }
      case mem: DefMemory if mem.name.slice(0, 2) != "T_" => mem.t match {
        case _: Bits =>
          _nodes += mem.id
          _nodeParent(mem.id) = mod
          _nodeToName(mem.id) = validName(mem.name)
        case _ => // Do not supoort aggregate type memories
      }
      case mem: DefSeqMemory if mem.name.slice(0, 2) != "T_" => mem.t match {
        case _: Bits =>
          _nodes += mem.id
          _nodeParent(mem.id) = mod
          _nodeToName(mem.id) = validName(mem.name)
        case _ => // Do not supoort aggregate type memories
      }
      case _ =>
    }
    mod
  }

  def construct(circuit: Circuit): Module =
    construct(circuit.name, circuit.components)

  def nodes = _nodes.toList

  def getName(node: HasId) = _nodeToName(node)

  def getPathName(mod: Module, seperator: String): String = {
    val modName = _modToName getOrElse (mod, mod.name)
    (_modParent get mod) match {
      case None    => modName
      case Some(p) => s"${getPathName(p, seperator)}$seperator$modName"
    }
  }

  def getPathName(node: HasId, seperator: String): String = {
    (_nodeParent get node) match {
      case None    => getName(node)
      case Some(p) => s"${getPathName(p, seperator)}$seperator${getName(node)}"
    }
  }

  def getParentPathName(node: HasId, seperator: String): String = {
    (_nodeParent get node) match {
      case None    => ""
      case Some(p) => getPathName(p, seperator)
    }
  }

  def clear {
    _modParent.clear
    _nodeParent.clear
    _modToName.clear
    _nodeToName.clear
    _nodes.clear
  }
}

object bigIntToStr {
  def apply(x: BigInt, base: Int) = base match {
    case 2  if x < 0 => s"-0b${(-x).toString(base)}"
    case 16 if x < 0 => s"-0x${(-x).toString(base)}"
    case 2  => s"0b${x.toString(base)}"
    case 16 => s"0x${x.toString(base)}"
    case _ => x.toString(base)
  }
}

case class TestApplicationException(exitVal: Int, lastMessage: String) extends RuntimeException(lastMessage)
