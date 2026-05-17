package info.hircus.kanren.dslir

import info.hircus.kanren.dslir.QueryIR._
import org.scalacheck.Properties

object QueryIRSpecification extends Properties("QueryIR") {

  property("expr variants retain constructor values") = {
    val ref = Ref("x")
    val atom = Atom(42)
    val pair = Pair(Ref("h"), Ref("t"))
    val list = ListExpr(List(Atom(1), Atom(2), Atom(3)))

    ref.name == "x" &&
      atom.value == 42 &&
      pair.head == Ref("h") &&
      pair.tail == Ref("t") &&
      list.items == List(Atom(1), Atom(2), Atom(3))
  }

  property("goal variants retain constructor values") = {
    val eq = Eq(Ref("x"), Atom(1))
    val neq = Neq(Ref("x"), Atom(2))
    val rel = Rel("edge", List(Ref("x"), Ref("y")))
    val conj = Conj(List(eq, rel))
    val disj = Disj(List(neq, rel))

    eq.lhs == Ref("x") &&
      eq.rhs == Atom(1) &&
      neq.rhs == Atom(2) &&
      rel.args.length == 2 &&
      conj.goals.length == 2 &&
      disj.goals.length == 2
  }

  property("declaration and program defaults are preserved") = {
    val constDecl = ConstDecl(List("A", "B"))
    val varDecl = VarDecl(List("x", "y"))
    val relDecl = RelDecl("fatherOf", arity = 2)
    val fact = Fact("fatherOf", List(Atom("anakin"), Atom("luke")))
    val rule = Rule("sibling", List("x", "y"), Neq(Ref("x"), Ref("y")))
    val program = Program(focus = "x", goals = List(Rel("fatherOf", List(Ref("x"), Ref("y")))))

    constDecl.names == List("A", "B") &&
      varDecl.domain.isEmpty &&
      !relDecl.infix &&
      fact.args.length == 2 &&
      rule.params == List("x", "y") &&
      program.limit == -1 &&
      !program.constrained &&
      program.declarations.isEmpty
  }
}
