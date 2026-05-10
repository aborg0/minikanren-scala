package info.hircus.kanren.dsl3

import info.hircus.kanren.dslir.QueryCompiler
import info.hircus.kanren.dslir.QueryIR
import info.hircus.kanren.dslir.QueryIR.{Expr, Goal, Program}
import scala.collection.compat.immutable.LazyList

object Scala3DSL {

  final case class LogicVar(name: String)

  private def toExpr(value: Any): Expr = value match {
    case e: Expr => e
    case lv: LogicVar => lv.asExpr
    case other => QueryIR.Atom(other)
  }

  extension (v: LogicVar) {
    def asExpr: Expr = QueryIR.Ref(v.name)
    infix def ===(rhs: Expr): Goal = QueryIR.Eq(asExpr, rhs)
    infix def =/=(rhs: Expr): Goal = QueryIR.Neq(asExpr, rhs)
    infix def ===(rhs: Any): Goal = QueryIR.Eq(asExpr, toExpr(rhs))
    infix def =/=(rhs: Any): Goal = QueryIR.Neq(asExpr, toExpr(rhs))
    infix def ===(rhs: LogicVar): Goal = QueryIR.Eq(asExpr, rhs.asExpr)
    infix def =/=(rhs: LogicVar): Goal = QueryIR.Neq(asExpr, rhs.asExpr)
  }

  extension (lhs: Expr) {
    infix def ===(rhs: Expr): Goal = QueryIR.Eq(lhs, rhs)
    infix def =/=(rhs: Expr): Goal = QueryIR.Neq(lhs, rhs)
    infix def ===(rhs: Any): Goal = QueryIR.Eq(lhs, toExpr(rhs))
    infix def =/=(rhs: Any): Goal = QueryIR.Neq(lhs, toExpr(rhs))
  }

  def v(name: String): LogicVar = LogicVar(name)
  def atom(value: Any): Expr = toExpr(value)
  def pair(head: Any, tail: Any): Expr = QueryIR.Pair(toExpr(head), toExpr(tail))
  def list(items: Any*): Expr = QueryIR.ListExpr(items.map(toExpr).toList)

  def all(goals: Goal*): Goal = QueryIR.Conj(goals.toList)
  def any(goals: Goal*): Goal = QueryIR.Disj(goals.toList)
  def rel(name: String, args: Any*): Goal = QueryIR.Rel(name, args.map(toExpr).toList)

  def isEmptyList(x: Any): Goal = rel("null_o", x)
  def isPair(x: Any): Goal = rel("pair_o", x)
  def head(pairExpr: Any, headExpr: Any): Goal = rel("car_o", pairExpr, headExpr)
  def tail(pairExpr: Any, tailExpr: Any): Goal = rel("cdr_o", pairExpr, tailExpr)
  def contains(elem: Any, listExpr: Any): Goal = rel("member_o", elem, listExpr)
  def append(left: Any, right: Any, result: Any): Goal = rel("append_o", left, right, result)

  // Compatibility aliases for existing naming
  def null_o(x: Any): Goal = isEmptyList(x)
  def pair_o(x: Any): Goal = isPair(x)
  def car_o(p: Any, a: Any): Goal = head(p, a)
  def cdr_o(p: Any, d: Any): Goal = tail(p, d)
  def member_o(x: Any, l: Any): Goal = contains(x, l)
  def append_o(l1: Any, l2: Any, l3: Any): Goal = append(l1, l2, l3)

  final case class QueryBuilder(
    focus: LogicVar,
    limit: Int = -1,
    constrained: Boolean = false
  ) {
    def withLimit(n: Int): QueryBuilder = copy(limit = n)
    def withConstraints(enabled: Boolean = true): QueryBuilder = copy(constrained = enabled)
    def where(goals: Goal*): Program = Program(focus.name, goals.toList, limit, constrained)
  }

  def query(focus: LogicVar): QueryBuilder = QueryBuilder(focus)

  private def toIdiomaticList(value: Any): Option[List[Any]] = value match {
    case Nil => Some(Nil)
    case (head, tail) =>
      toIdiomaticList(tail).map(normalizedTail => normalize(head) :: normalizedTail)
    case _ => None
  }

  private def normalize(value: Any): Any = {
    toIdiomaticList(value) match {
      case Some(items) => items
      case None =>
        value match {
          case (head, tail) => (normalize(head), normalize(tail))
          case other => other
        }
    }
  }

  private def normalizeResults(results: LazyList[Any]): LazyList[Any] =
    results.map(normalize)

  def run(program: Program): LazyList[Any] =
    normalizeResults(QueryCompiler.executeStream(program))

  def runList(program: Program): List[Any] = run(program).toList

  def run(focus: LogicVar, limit: Int = -1)(goals: Goal*): LazyList[Any] =
    run(Program(focus.name, goals.toList, limit, constrained = false))

  def runList(focus: LogicVar, limit: Int = -1)(goals: Goal*): List[Any] =
    run(focus, limit)(goals*).toList
}
