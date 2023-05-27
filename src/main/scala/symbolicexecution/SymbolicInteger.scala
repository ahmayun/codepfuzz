package symbolicexecution

class SymbolicInteger(override val expr: SymbolicTree, val concrete: Option[Int]) extends SymbolicExpression(expr) {

  def this() = {
    this(SymbolicTree(null, "SymInt", null), None)
  }

  def this(i: Int) = {
    this(SymbolicTree(null, s"SymInt:${i.toString}", null), Some(i))
  }

}
