package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren.{Goal => MKGoal, Var}
import info.hircus.kanren.Prelude
import info.hircus.kanren.MKMath
import info.hircus.kanren.dslir.QueryIR._
import scala.collection.compat.immutable.LazyList

object QueryCompiler {

  private def collectExprVars(expr: Expr): Set[String] = expr match {
    case Ref(name) => Set(name)
    case Atom(_) => Set.empty
    case Pair(head, tail) => collectExprVars(head) ++ collectExprVars(tail)
    case ListExpr(items) => items.flatMap(collectExprVars).toSet
  }

  private def collectGoalVars(goal: Goal): Set[String] = goal match {
    case Eq(lhs, rhs) => collectExprVars(lhs) ++ collectExprVars(rhs)
    case Neq(lhs, rhs) => collectExprVars(lhs) ++ collectExprVars(rhs)
    case Rel(_, args) => args.flatMap(collectExprVars).toSet
    case Conj(goals) => goals.flatMap(collectGoalVars).toSet
    case Disj(goals) => goals.flatMap(collectGoalVars).toSet
  }

  private def collectProgramVars(program: Program): List[String] = {
    val goalVars = program.goals.flatMap(collectGoalVars)
    (program.focus :: goalVars).distinct
  }

  private def toPairs(items: List[Any]): Any = items match {
    case Nil => Nil
    case head :: tail => (head, toPairs(tail))
  }

  private def compileExpr(expr: Expr, env: Map[String, Var]): Any = expr match {
    case Ref(name) => env(name)
    case Atom(value) => value
    case Pair(head, tail) => (compileExpr(head, env), compileExpr(tail, env))
    case ListExpr(items) => toPairs(items.map(compileExpr(_, env)))
  }

  private def compileDisj(goals: List[Goal], env: Map[String, Var]): MKGoal = goals match {
    case Nil => MiniKanren.fail
    case head :: Nil => compileGoal(head, env)
    case head :: tail => MiniKanren.any_e(compileGoal(head, env), compileDisj(tail, env))
  }

  private def expectArity(name: String, args: List[Any], arity: Int): Unit = {
    if (args.length != arity) {
      throw new IllegalArgumentException(
        "Relation " + name + " expects " + arity + " arguments but got " + args.length
      )
    }
  }

  private def compileRelation(name: String, args: List[Any]): MKGoal = name match {
    case "null_o" =>
      expectArity(name, args, 1)
      Prelude.null_o(args(0))
    case "pair_o" =>
      expectArity(name, args, 1)
      Prelude.pair_o(args(0))
    case "car_o" =>
      expectArity(name, args, 2)
      Prelude.car_o(args(0), args(1))
    case "cdr_o" =>
      expectArity(name, args, 2)
      Prelude.cdr_o(args(0), args(1))
    case "member_o" =>
      expectArity(name, args, 2)
      Prelude.member_o(args(0), args(1))
    case "append_o" =>
      expectArity(name, args, 3)
      Prelude.append_o(args(0), args(1), args(2))
    case "pos_o" =>
      expectArity(name, args, 1)
      MKMath.pos_o(args(0))
    case "digit_o" =>
      expectArity(name, args, 1)
      MKMath.digit_o(args(0))
    case "gt1_o" =>
      expectArity(name, args, 1)
      MKMath.gt1_o(args(0))
    case "eq_len_o" =>
      expectArity(name, args, 2)
      MKMath.eq_len_o(args(0), args(1))
    case "lt_len_o" =>
      expectArity(name, args, 2)
      MKMath.lt_len_o(args(0), args(1))
    case "lt_o" =>
      expectArity(name, args, 2)
      MKMath.lt_o(args(0), args(1))
    case "bit_xor_o" =>
      expectArity(name, args, 3)
      MKMath.bit_xor_o(args(0), args(1), args(2))
    case "bit_and_o" =>
      expectArity(name, args, 3)
      MKMath.bit_and_o(args(0), args(1), args(2))
    case "add_o" =>
      expectArity(name, args, 3)
      MKMath.add_o(args(0), args(1), args(2))
    case "sub_o" =>
      expectArity(name, args, 3)
      MKMath.sub_o(args(0), args(1), args(2))
    case "mul_o" =>
      expectArity(name, args, 3)
      MKMath.mul_o(args(0), args(1), args(2))
    case _ =>
      throw new IllegalArgumentException("Unknown relation: " + name)
  }

  private def compileGoal(goal: Goal, env: Map[String, Var]): MKGoal = goal match {
    case Eq(lhs, rhs) => MiniKanren.mkEqual(compileExpr(lhs, env), compileExpr(rhs, env))
    case Neq(lhs, rhs) => MiniKanren.neverEqual(compileExpr(lhs, env), compileExpr(rhs, env))
    case Rel(name, args) => compileRelation(name, args.map(compileExpr(_, env)))
    case Conj(goals) =>
      if (goals.isEmpty) MiniKanren.succeed
      else MiniKanren.all(goals.map(compileGoal(_, env)): _*)
    case Disj(goals) => compileDisj(goals, env)
  }

  private def compileProgram(program: Program): (Var, MKGoal) = {
    val env = collectProgramVars(program).map { name =>
      name -> MiniKanren.make_var(Symbol(name))
    }.toMap

    val focus = env(program.focus)
    val compiledGoals = program.goals.map(compileGoal(_, env))
    val combined =
      if (compiledGoals.isEmpty) MiniKanren.succeed
      else MiniKanren.all(compiledGoals: _*)

    (focus, combined)
  }

  def executeStream(program: Program): LazyList[Any] = {
    val (focus, goal) = compileProgram(program)
    val initial = if (program.constrained) MiniKanren.empty_cs else MiniKanren.empty_s
    val allres = goal(initial).map { s =>
      MiniKanren.reify(MiniKanren.walk_*(focus, s))
    }
    if (program.limit < 0) allres else allres.take(program.limit)
  }

  def execute(program: Program): List[Any] = {
    executeStream(program).toList
  }

  def explain(program: Program): MKGoal = {
    val (_, goal) = compileProgram(program)
    goal
  }
}
