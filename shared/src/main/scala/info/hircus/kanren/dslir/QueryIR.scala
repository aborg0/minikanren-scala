package info.hircus.kanren.dslir

object QueryIR {

  sealed trait Expr
  final case class Ref(name: String) extends Expr
  final case class Atom(value: Any) extends Expr
  final case class Pair(head: Expr, tail: Expr) extends Expr
  final case class ListExpr(items: List[Expr]) extends Expr

  sealed trait Goal
  final case class Eq(lhs: Expr, rhs: Expr) extends Goal
  final case class Neq(lhs: Expr, rhs: Expr) extends Goal
  final case class Rel(name: String, args: List[Expr]) extends Goal
  final case class Conj(goals: List[Goal]) extends Goal
  final case class Disj(goals: List[Goal]) extends Goal

  final case class Program(
    focus: String,
    goals: List[Goal],
    limit: Int = -1,
    constrained: Boolean = false
  )
}
