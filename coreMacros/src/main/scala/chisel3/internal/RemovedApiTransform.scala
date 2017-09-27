// See LICENSE for license details.

package chisel3.internal

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

class RemovedApiMacroTransform(val c: Context) {
  import c.universe._
  
  def apply(dir: c.Tree, width: c.Tree): c.Tree = {
    c.abort(c.enclosingPosition, s"uh oh")
  }
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class removedApi(val msg: String) extends StaticAnnotation {
  def removedApiTransform(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    
    val msg: String = c.prefix.tree match {
      case q"new removedApi($str)" => c.eval[String](c.Expr(str))
      case other => c.abort(c.enclosingPosition, s"@removedApi annotion takes only error message, got ${show(c.prefix.tree)}")
    }
    
    val transformed = annottees match {
      // Only allowed on methods
      case q"$mods def $tname[..$tparams](...$paramss): $tpt = $expr" :: Nil => {
        expr match {
          case q"???" => annottees
          case other => Seq(c.abort(c.enclosingPosition, s"@removedApi annotated method must be implemented with ???, got ${show(expr)}"))
        }
      }
      case others =>
        val combined = others.map({ tree => show(tree) }).mkString("\r\n")
        Seq(c.abort(c.enclosingPosition, s"@removedApi annotation may only be used on methods, got ${combined}"))
    }
    q"..$transformed"
  }
  
  def macroTransform(annottees: Any*): Any = macro removedApiTransform
}