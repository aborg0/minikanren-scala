package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren.{Goal => MKGoal, Var}
import info.hircus.kanren.Prelude
import info.hircus.kanren.MKMath
import info.hircus.kanren.dslir.QueryIR._
import scala.collection.compat.immutable.LazyList
import scala.collection.immutable.Map

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
    if (program.declarations.isEmpty) {
      val goalVars = program.goals.flatMap(collectGoalVars)
      (program.focus :: goalVars).distinct
    } else {
      val goalVars = program.goals.flatMap(collectGoalVars)
      val declaredVars = program.declarations.collect { case VarDecl(names, _) => names }.flatten
      (program.focus :: (declaredVars ++ goalVars)).distinct
    }
  }

  private def toPairs(items: List[Any]): Any = items match {
    case Nil => Nil
    case head :: tail => (head, toPairs(tail))
  }

  private def compileExpr(expr: Expr, env: Map[String, Var], constants: Set[String]): Any = expr match {
    case Ref(name) =>
      if (env.contains(name)) env(name)
      else if (constants.contains(name)) name
      else env(name)
    case Atom(value) => value
    case Pair(head, tail) => (compileExpr(head, env, constants), compileExpr(tail, env, constants))
    case ListExpr(items) => toPairs(items.map(compileExpr(_, env, constants)))
  }

  private def compileDisj(goals: List[Goal], env: Map[String, Var], constants: Set[String], declarations: Map[String, List[Declaration]]): MKGoal = goals match {
    case Nil => MiniKanren.fail
    case head :: Nil => compileGoal(head, env, constants, declarations)
    case head :: tail => MiniKanren.any_e(compileGoal(head, env, constants, declarations), compileDisj(tail, env, constants, declarations))
  }

  private def expectArity(name: String, args: List[Any], arity: Int): Unit = {
    if (args.length != arity) {
      throw new IllegalArgumentException(
        "Relation " + name + " expects " + arity + " arguments but got " + args.length
      )
    }
  }

  private def compileBuiltinRelation(name: String, args: List[Any]): Option[MKGoal] = name match {
    case "null_o" =>
      expectArity(name, args, 1)
      Some(Prelude.null_o(args(0)))
    case "pair_o" =>
      expectArity(name, args, 1)
      Some(Prelude.pair_o(args(0)))
    case "car_o" =>
      expectArity(name, args, 2)
      Some(Prelude.car_o(args(0), args(1)))
    case "cdr_o" =>
      expectArity(name, args, 2)
      Some(Prelude.cdr_o(args(0), args(1)))
    case "member_o" =>
      expectArity(name, args, 2)
      Some(Prelude.member_o(args(0), args(1)))
    case "append_o" =>
      expectArity(name, args, 3)
      Some(Prelude.append_o(args(0), args(1), args(2)))
    case "pos_o" =>
      expectArity(name, args, 1)
      Some(MKMath.pos_o(args(0)))
    case "digit_o" =>
      expectArity(name, args, 1)
      Some(MKMath.digit_o(args(0)))
    case "gt1_o" =>
      expectArity(name, args, 1)
      Some(MKMath.gt1_o(args(0)))
    case "eq_len_o" =>
      expectArity(name, args, 2)
      Some(MKMath.eq_len_o(args(0), args(1)))
    case "lt_len_o" =>
      expectArity(name, args, 2)
      Some(MKMath.lt_len_o(args(0), args(1)))
    case "lt_o" =>
      expectArity(name, args, 2)
      Some(MKMath.lt_o(args(0), args(1)))
    case "bit_xor_o" =>
      expectArity(name, args, 3)
      Some(MKMath.bit_xor_o(args(0), args(1), args(2)))
    case "bit_and_o" =>
      expectArity(name, args, 3)
      Some(MKMath.bit_and_o(args(0), args(1), args(2)))
    case "add_o" =>
      expectArity(name, args, 3)
      Some(MKMath.add_o(args(0), args(1), args(2)))
    case "sub_o" =>
      expectArity(name, args, 3)
      Some(MKMath.sub_o(args(0), args(1), args(2)))
    case "mul_o" =>
      expectArity(name, args, 3)
      Some(MKMath.mul_o(args(0), args(1), args(2)))
    case _ =>
      None
  }

  private def compileGoal(goal: Goal, env: Map[String, Var], constants: Set[String], declarations: Map[String, List[Declaration]]): MKGoal = goal match {
    case Eq(lhs, rhs) => MiniKanren.mkEqual(compileExpr(lhs, env, constants), compileExpr(rhs, env, constants))
    case Neq(lhs, rhs) => MiniKanren.neverEqual(compileExpr(lhs, env, constants), compileExpr(rhs, env, constants))
    case Rel(name, args) =>
      val compiledArgs = args.map(compileExpr(_, env, constants))
      compileBuiltinRelation(name, compiledArgs).getOrElse {
        declarations.get(name) match {
          case Some(defs) => compileUserDefinedRelation(name, compiledArgs, env, constants, defs, declarations)
          case None => throw new IllegalArgumentException("Unknown relation: " + name)
        }
      }
    case Conj(goals) =>
      if (goals.isEmpty) MiniKanren.succeed
      else MiniKanren.all(goals.map(compileGoal(_, env, constants, declarations)): _*)
    case Disj(goals) => compileDisj(goals, env, constants, declarations)
  }

  private def compileUserDefinedRelation(
    name: String,
    args: List[Any],
    env: Map[String, Var],
    constants: Set[String],
    defs: List[Declaration],
    allDeclarations: Map[String, List[Declaration]]
  ): MKGoal = {
    val clauses = defs.map {
      case Fact(_, factArgs) =>
        if (factArgs.length != args.length) {
          throw new IllegalArgumentException("Relation " + name + " expects " + args.length + " arguments but got " + factArgs.length)
        }
        MiniKanren.all(args.zip(factArgs.map(compileExpr(_, env, constants))).map { case (left, right) => MiniKanren.mkEqual(left, right) }: _*)
      case Rule(_, params, body) =>
        if (params.length != args.length) {
          throw new IllegalArgumentException("Relation " + name + " expects " + args.length + " arguments but got " + params.length)
        }
        val bodyVars = collectGoalVars(body).toList
        val localNames = (params ++ bodyVars).distinct
        val localEnv = localNames.zipWithIndex.map { case (param, index) =>
          param -> MiniKanren.make_var(Symbol(name + "_" + param + "_" + index.toString))
        }.toMap
        val mergedEnv = env ++ localEnv
        val headGoals = args.zip(params).map { case (left, param) => MiniKanren.mkEqual(left, mergedEnv(param)) }
        val bodyGoal = compileGoal(body, mergedEnv, constants, allDeclarations)
        MiniKanren.all((headGoals :+ bodyGoal): _*)
      case _ =>
        throw new IllegalArgumentException("Unsupported declaration for relation " + name)
    }

    clauses match {
      case Nil => throw new IllegalArgumentException("Unknown relation: " + name)
      case head :: Nil => head
      case head :: tail => MiniKanren.any_e(head, compileDisjGoalList(tail))
    }
  }

  private def compileDisjGoalList(goals: List[MKGoal]): MKGoal = goals match {
    case Nil => MiniKanren.fail
    case head :: Nil => head
    case head :: tail => MiniKanren.any_e(head, compileDisjGoalList(tail))
  }

  private def compileProgram(program: Program): (Var, MKGoal) = {
    val constants = program.declarations.collect { case ConstDecl(names) => names }.flatten.toSet
    val declarationMap = program.declarations.collect {
      case decl @ Fact(name, _) => name -> decl
      case decl @ Rule(name, _, _) => name -> decl
    }.groupBy(_._1).map { case (name, pairs) => name -> pairs.map(_._2) }
    val env = collectProgramVars(program).map { name =>
      name -> MiniKanren.make_var(Symbol(name))
    }.toMap

    val focus = env(program.focus)
    val declaredVarGoals = program.declarations.collect {
      case VarDecl(names, Some(domain)) =>
        names.map { name => MiniKanren.mkEqual(env(name), compileExpr(domain, env, constants)) }
    }.flatten
    val compiledGoals = program.goals.map(compileGoal(_, env, constants, declarationMap))
    val combined =
      if (compiledGoals.isEmpty && declaredVarGoals.isEmpty) MiniKanren.succeed
      else MiniKanren.all((declaredVarGoals ++ compiledGoals): _*)

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
