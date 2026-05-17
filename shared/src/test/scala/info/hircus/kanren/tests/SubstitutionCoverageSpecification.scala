package info.hircus.kanren.tests

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Substitution
import info.hircus.kanren.Substitution._
import org.scalacheck.Properties

object SubstitutionCoverageSpecification extends Properties("SubstitutionCoverage") {

  property("empty and simple substitutions behave as linked maps") = {
    val x = make_var(Symbol("x"))
    val y = make_var(Symbol("y"))

    val emptyOk = EmptySubst.lookup(x).isEmpty && EmptySubst.length == 0

    val s1 = EmptySubst.extend(x, 1).get
    val s2 = s1.extend(y, 2).get

    emptyOk &&
      s1.lookup(x).contains(1) &&
      s2.lookup(y).contains(2) &&
      s2.lookup(x).contains(1) &&
      s2.length == 2
  }

  property("constraint substitutions reject forbidden values") = {
    val x = make_var(Symbol("cx"))

    val emptyConstraintsVisible = ConstraintSubst0(Nil).constraints(x).isEmpty && ConstraintSubst0(Nil).length == 0

    val c0 = ConstraintSubst0(Nil).c_extend(x, 5).asInstanceOf[ConstraintSubst0]
    val c0Again = c0.c_extend(x, 5).asInstanceOf[ConstraintSubst0]

    val c0Rejects = c0.extend(x, 5).isEmpty && c0.constraints(x) == List(5)
    val deduped = c0Again.constraints(x) == List(5)

    val base = EmptySubst.extend(x, 1).get.asInstanceOf[SimpleSubst]
    val cn = ConstraintSubstN(base, Nil).c_extend(x, 6).asInstanceOf[ConstraintSubstN]
    val cnRejects = cn.extend(x, 6).isEmpty
    val cnLookupAndLength = cn.lookup(x).contains(1) && cn.length == 1

    emptyConstraintsVisible && c0Rejects && deduped && cnRejects && cnLookupAndLength
  }

  property("constraint substitution unify checks both directions") = {
    val leftVar = make_var(Symbol("leftVar"))
    val rightVar = make_var(Symbol("rightVar"))

    val guarded = new Substitution.ConstraintSubst {
      override def extend(v: MiniKanren.Var, x: Any): Option[MiniKanren.Subst] = Some(this)
      override def lookup(v: MiniKanren.Var): Option[Any] = None
      override def constraints(v: MiniKanren.Var): List[Any] =
        if (v == leftVar) List(99)
        else if (v == rightVar) List(42)
        else Nil
      override def length: Int = 0
    }

    guarded.unify(leftVar, 99).isEmpty &&
      guarded.unify(42, rightVar).isEmpty &&
      guarded.unify(leftVar, 100).nonEmpty
  }

  property("map-backed substitutions work") = {
    val x = make_var(Symbol("mx"))

    val m = MSubst(Map()).extend(x, 3).get
    val c = CljSubst(Map()).extend(x, 4).get

    m.lookup(x).contains(3) && m.length == 1 &&
      c.lookup(x).contains(4) && c.length == 1 &&
      empty_msubst.length == 0 && empty_cljsubst.length == 0
  }
}