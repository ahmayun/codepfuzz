package symbolicexecution

case class SymbolicExpression (expr: SymbolicTree) {

  def or(symbolicExpression: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "||", symbolicExpression.expr))
  }

  def and(symbolicExpression: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "&&", symbolicExpression.expr))
  }

  def +(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "+", x.expr))
  }

  def +(x: Int): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "+", new SymbolicInteger(x).expr))
  }

  def -(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "-", x.expr))
  }

  def -(x: Int): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "-", new SymbolicInteger(x).expr))
  }

  def *(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "*", x.expr))
  }

  def *(x: Int): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "*", new SymbolicInteger(x).expr))
  }

  def /(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "+", x.expr))
  }

  def /(x: Int): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "+", new SymbolicInteger(x).expr))
  }

  def <(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "<", x.expr))
  }

  def <=(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, "<=", x.expr))
  }

  def >(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, ">", x.expr))
  }

  def >=(x: SymbolicExpression): SymbolicExpression = {
    SymbolicExpression(SymbolicTree(expr, ">=", x.expr))
  }
  override def toString: String = expr.toString

  def isEmpty: Boolean = expr.isEmpty
}
