package symbolicexecution

case class SymbolicTree(left: SymbolicTree, node: String, right: SymbolicTree) {
  def this() = {
    this(null, null, null)
  }

  def this(node: String) = {
    this(null, node, null)
  }

  def isEmpty = this match {
    case SymbolicTree(null, null, null) => true
    case _ => false
  }

  override def toString: String = {
    this match {
      case SymbolicTree(null, null, null) => ""
      case SymbolicTree(null, node, null) => node
      case _ => s"(${left.toString} $node ${right.toString})"
    }
  }
}

