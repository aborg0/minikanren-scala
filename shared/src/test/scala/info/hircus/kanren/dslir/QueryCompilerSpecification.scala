package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.dslir.QueryIR._
import org.scalacheck.Properties

object QueryCompilerSpecification extends Properties("QueryCompiler") {

  property("list and pair expressions are compiled and normalized") = {
    val program = Program(
      focus = "x",
      goals = List(Eq(Ref("x"), Pair(Atom(1), Pair(Atom(2), Atom(Nil)))))
    )

    QueryCompiler.execute(program) == List((1, (2, Nil)))
  }

  property("constrained mode enforces neq before eq") = {
    val constrained = Program(
      focus = "x",
      goals = List(Neq(Ref("x"), Atom(1)), Eq(Ref("x"), Atom(1))),
      constrained = true
    )
    val unconstrained = constrained.copy(constrained = false)

    QueryCompiler.execute(constrained).isEmpty &&
      QueryCompiler.execute(unconstrained) == List(1)
  }

  property("empty conjunction succeeds and empty disjunction fails") = {
    val succeeds = Program(
      focus = "x",
      goals = List(Conj(Nil), Eq(Ref("x"), Atom(7)))
    )
    val fails = Program(
      focus = "x",
      goals = List(Disj(Nil), Eq(Ref("x"), Atom(7)))
    )

    QueryCompiler.execute(succeeds) == List(7) && QueryCompiler.execute(fails).isEmpty
  }

  property("limits apply to streams") = {
    val program = Program(
      focus = "x",
      goals = List(Disj(List(Eq(Ref("x"), Atom(1)), Eq(Ref("x"), Atom(2))))),
      limit = 1
    )

    QueryCompiler.execute(program) == List(1)
  }

  property("const and var declarations are honored") = {
    val constProgram = Program(
      focus = "x",
      goals = List(Rel("is_answer", List(Ref("x")))),
      declarations = List(
        ConstDecl(List("answer")),
        Fact("is_answer", List(Ref("answer")))
      )
    )
    val varProgram = Program(
      focus = "y",
      goals = Nil,
      declarations = List(VarDecl(List("y"), Some(Atom(5))))
    )

    QueryCompiler.execute(constProgram) == List("answer") &&
      QueryCompiler.execute(varProgram) == List(5)
  }

  property("user facts and rules can be queried") = {
    val factsAndRules = Program(
      focus = "child",
      goals = List(Rel("parent", List(Atom("alice"), Ref("child")))),
      declarations = List(
        Fact("mother", List(Atom("alice"), Atom("bob"))),
        Rule("parent", List("x", "y"), Rel("mother", List(Ref("x"), Ref("y"))))
      )
    )

    QueryCompiler.execute(factsAndRules) == List("bob")
  }

  property("multiple clauses for one relation create disjunction") = {
    val program = Program(
      focus = "x",
      goals = List(Rel("coin", List(Ref("x")))),
      declarations = List(
        Fact("coin", List(Atom("heads"))),
        Fact("coin", List(Atom("tails")))
      )
    )

    QueryCompiler.execute(program) == List("heads", "tails")
  }

  property("unknown relation throws") = {
    val program = Program(
      focus = "x",
      goals = List(Rel("missing_relation", List(Ref("x"))))
    )

    try {
      QueryCompiler.execute(program)
      false
    } catch {
      case _: IllegalArgumentException => true
    }
  }

  property("builtin arity mismatch throws") = {
    val program = Program(
      focus = "x",
      goals = List(Rel("null_o", Nil))
    )

    try {
      QueryCompiler.execute(program)
      false
    } catch {
      case _: IllegalArgumentException => true
    }
  }

  property("declaration arity mismatch throws") = {
    val factArityMismatch = Program(
      focus = "x",
      goals = List(Rel("single", List(Ref("x"), Atom(1)))),
      declarations = List(Fact("single", List(Atom(1))))
    )
    val ruleArityMismatch = Program(
      focus = "x",
      goals = List(Rel("pair", List(Ref("x")))),
      declarations = List(Rule("pair", List("a", "b"), Eq(Ref("a"), Ref("b"))))
    )

    val factThrows = try {
      QueryCompiler.execute(factArityMismatch)
      false
    } catch {
      case _: IllegalArgumentException => true
    }

    val ruleThrows = try {
      QueryCompiler.execute(ruleArityMismatch)
      false
    } catch {
      case _: IllegalArgumentException => true
    }

    factThrows && ruleThrows
  }

  property("query ir declarations expose expected defaults") = {
    val relDecl = RelDecl("edge", arity = 2)
    val program = Program(focus = "x", goals = Nil)

    !relDecl.infix && !program.constrained && program.limit == -1 && program.declarations.isEmpty
  }

  property("explain returns runnable goal") = {
    val program = Program(
      focus = "x",
      goals = List(Eq(Ref("x"), Atom(3)))
    )
    val goal = QueryCompiler.explain(program)

    goal(MiniKanren.empty_s).nonEmpty
  }
}