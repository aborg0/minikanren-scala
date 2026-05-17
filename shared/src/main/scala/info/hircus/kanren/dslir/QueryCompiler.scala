package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren.{Goal => MKGoal, Var}
import info.hircus.kanren.Prelude
import info.hircus.kanren.MKMath
import info.hircus.kanren.StringOps
import info.hircus.kanren.ListOps
import info.hircus.kanren.dslir.QueryIR._
import scala.collection.compat.immutable.LazyList
import scala.collection.immutable.Map

object QueryCompiler {

  private val kanrenNumericBuiltins: Set[String] = Set(
    "pos_o",
    "digit_o",
    "gt1_o",
    "eq_len_o",
    "lt_len_o",
    "lt_o",
    "gt_o",
    "le_o",
    "ge_o",
    "eq_num_o",
    "ne_num_o",
    "add_o",
    "sub_o",
    "mul_o"
  )

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

  private def toKanrenNumberInput(value: Any): Any = value match {
    case n: Int if n >= 0 => MKMath.build_num(n)
    case n: Long if n >= 0L && n <= Int.MaxValue.toLong => MKMath.build_num(n.toInt)
    case other => other
  }

  private def isCanonicalKanrenNumber(value: Any): Boolean = value match {
    case Nil => true
    case (bit: Int, rest) if bit == 0 || bit == 1 => isCanonicalKanrenNumber(rest)
    case _ => false
  }

  private def normalizeNumericOutput(value: Any): Any =
    if (isCanonicalKanrenNumber(value)) MKMath.read_num(value) else value

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

  private def compileBuiltinRelation(name: String, args: List[Any]): Option[MKGoal] = {
    PreludeRegistry.resolve(name, args).orElse {
      val normalizedArgs =
        if (kanrenNumericBuiltins.contains(name)) args.map(toKanrenNumberInput)
        else args
      name match {
    case "null_o" =>
      expectArity(name, args, 1)
      Some(Prelude.null_o(normalizedArgs(0)))
    case "pair_o" =>
      expectArity(name, args, 1)
      Some(Prelude.pair_o(normalizedArgs(0)))
    case "car_o" =>
      expectArity(name, args, 2)
      Some(Prelude.car_o(normalizedArgs(0), normalizedArgs(1)))
    case "cdr_o" =>
      expectArity(name, args, 2)
      Some(Prelude.cdr_o(normalizedArgs(0), normalizedArgs(1)))
    case "member_o" =>
      expectArity(name, args, 2)
      Some(Prelude.member_o(normalizedArgs(0), normalizedArgs(1)))
    case "append_o" =>
      expectArity(name, args, 3)
      Some(Prelude.append_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "pos_o" =>
      expectArity(name, args, 1)
      Some(MKMath.pos_o(normalizedArgs(0)))
    case "digit_o" =>
      expectArity(name, args, 1)
      Some(MKMath.digit_o(normalizedArgs(0)))
    case "gt1_o" =>
      expectArity(name, args, 1)
      Some(MKMath.gt1_o(normalizedArgs(0)))
    case "eq_len_o" =>
      expectArity(name, args, 2)
      Some(MKMath.eq_len_o(normalizedArgs(0), normalizedArgs(1)))
    case "lt_len_o" =>
      expectArity(name, args, 2)
      Some(MKMath.lt_len_o(normalizedArgs(0), normalizedArgs(1)))
    case "lt_o" =>
      expectArity(name, args, 2)
      Some(MKMath.lt_o(normalizedArgs(0), normalizedArgs(1)))
    case "gt_o" =>
      expectArity(name, args, 2)
      Some(MKMath.gt_o(normalizedArgs(0), normalizedArgs(1)))
    case "le_o" =>
      expectArity(name, args, 2)
      Some(MKMath.le_o(normalizedArgs(0), normalizedArgs(1)))
    case "ge_o" =>
      expectArity(name, args, 2)
      Some(MKMath.ge_o(normalizedArgs(0), normalizedArgs(1)))
    case "eq_num_o" =>
      expectArity(name, args, 2)
      Some(MKMath.eq_num_o(normalizedArgs(0), normalizedArgs(1)))
    case "ne_num_o" =>
      expectArity(name, args, 2)
      Some(MKMath.ne_num_o(normalizedArgs(0), normalizedArgs(1)))
    case "bit_xor_o" =>
      expectArity(name, args, 3)
      Some(MKMath.bit_xor_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "bit_and_o" =>
      expectArity(name, args, 3)
      Some(MKMath.bit_and_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "add_o" =>
      expectArity(name, args, 3)
      Some(MKMath.add_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "sub_o" =>
      expectArity(name, args, 3)
      Some(MKMath.sub_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "mul_o" =>
      expectArity(name, args, 3)
      Some(MKMath.mul_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "div_o" =>
      expectArity(name, args, 3)
      Some(MKMath.div_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "mod_o" =>
      expectArity(name, args, 3)
      Some(MKMath.mod_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    // String operations
    case "atom_concat" =>
      expectArity(name, args, 3)
      Some(StringOps.atom_concat(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "atom_codes" =>
      expectArity(name, args, 2)
      Some(StringOps.atom_codes(normalizedArgs(0), normalizedArgs(1)))
    case "atom_chars" =>
      expectArity(name, args, 2)
      Some(StringOps.atom_chars(normalizedArgs(0), normalizedArgs(1)))
    case "atom_length" =>
      expectArity(name, args, 2)
      Some(StringOps.atom_length(normalizedArgs(0), normalizedArgs(1)))
    case "atom_string" =>
      expectArity(name, args, 2)
      Some(StringOps.atom_string(normalizedArgs(0), normalizedArgs(1)))
    case "sub_atom" =>
      expectArity(name, args, 5)
      Some(StringOps.sub_atom(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2), normalizedArgs(3), normalizedArgs(4)))
    case "upcase_atom" =>
      expectArity(name, args, 2)
      Some(StringOps.upcase_atom(normalizedArgs(0), normalizedArgs(1)))
    case "downcase_atom" =>
      expectArity(name, args, 2)
      Some(StringOps.downcase_atom(normalizedArgs(0), normalizedArgs(1)))
    case "atom_number" =>
      expectArity(name, args, 2)
      Some(StringOps.atom_number(normalizedArgs(0), normalizedArgs(1)))
    case "number_codes" =>
      expectArity(name, args, 2)
      Some(StringOps.number_codes(normalizedArgs(0), normalizedArgs(1)))
    case "number_chars" =>
      expectArity(name, args, 2)
      Some(StringOps.number_chars(normalizedArgs(0), normalizedArgs(1)))
    // List operations
    case "length_o" =>
      expectArity(name, args, 2)
      Some(ListOps.length_o(normalizedArgs(0), normalizedArgs(1)))
    case "reverse_o" =>
      expectArity(name, args, 2)
      Some(ListOps.reverse_o(normalizedArgs(0), normalizedArgs(1)))
    case "nth0_o" =>
      expectArity(name, args, 3)
      Some(ListOps.nth0_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "nth1_o" =>
      expectArity(name, args, 3)
      Some(ListOps.nth1_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case "last_o" =>
      expectArity(name, args, 2)
      Some(ListOps.last_o(normalizedArgs(0), normalizedArgs(1)))
    case "prefix_o" =>
      expectArity(name, args, 2)
      Some(ListOps.prefix_o(normalizedArgs(0), normalizedArgs(1)))
    case "suffix_o" =>
      expectArity(name, args, 2)
      Some(ListOps.suffix_o(normalizedArgs(0), normalizedArgs(1)))
    case "select_o" =>
      expectArity(name, args, 3)
      Some(ListOps.select_o(normalizedArgs(0), normalizedArgs(1), normalizedArgs(2)))
    case _ =>
      None
      }
    }
  }

  private def collectNumericBuiltinVars(goal: Goal): Set[String] = goal match {
    case Rel(name, args) if kanrenNumericBuiltins.contains(name) => args.flatMap(collectExprVars).toSet
    case Conj(goals) => goals.flatMap(collectNumericBuiltinVars).toSet
    case Disj(goals) => goals.flatMap(collectNumericBuiltinVars).toSet
    case _ => Set.empty
  }

  private def compileGoal(goal: Goal, env: Map[String, Var], constants: Set[String], declarations: Map[String, List[Declaration]]): MKGoal = goal match {
    case Eq(lhs, rhs) => MiniKanren.mkEqual(compileExpr(lhs, env, constants), compileExpr(rhs, env, constants))
    case Neq(lhs, rhs) => MiniKanren.neverEqual(compileExpr(lhs, env, constants), compileExpr(rhs, env, constants))
    case Rel(name, args) =>
      val compiledArgs = args.map(compileExpr(_, env, constants))
      compileBuiltinRelation(name, compiledArgs).getOrElse {
        declarations.get(name) match {
          case Some(defs) =>
            // Defer user-defined relation expansion so recursive rules are not unfolded eagerly at compile time.
            lazy val compiled = compileUserDefinedRelation(name, compiledArgs, env, constants, defs, declarations)
            (s: MiniKanren.Subst) => compiled(s)
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
    val normalizeFocusAsNumber = program.goals.flatMap(collectNumericBuiltinVars).toSet.contains(program.focus)
    val initial = if (program.constrained) MiniKanren.empty_cs else MiniKanren.empty_s
    val allres = goal(initial).map { s =>
      val raw = MiniKanren.reify(MiniKanren.walk_*(focus, s))
      if (normalizeFocusAsNumber) normalizeNumericOutput(raw) else raw
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
