package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Convenient one-stop import for all miniKanren operations.
  * Usage: import info.hircus.kanren.AllOps._
  * Then use: mod_o(10, 3, x), atom_number("42", x), etc.
  */
@JSExportTopLevel("miniKanrenAllOps")
@JSExportAll
object AllOps {
  type Goal = MiniKanren.Goal
  type Subst = MiniKanren.Subst

  val succeed: Goal = MiniKanren.succeed
  val fail: Goal = MiniKanren.fail

  def make_var(sym: Symbol): MiniKanren.Var = MiniKanren.make_var(sym)
  def run(n: Int, v: MiniKanren.Var)(g: => Goal): List[Any] = MiniKanren.run(n, v)(g)
  def all(gs: Goal*): Goal = MiniKanren.all(gs: _*)
  def any_e(g1: Goal, g2: Goal): Goal = MiniKanren.any_e(g1, g2)
  def if_e(g1: Goal, g2: Goal, g3: Goal): Goal = MiniKanren.if_e(g1, g2, g3)
  def both(g1: Goal, g2: Goal): Goal = MiniKanren.both(g1, g2)
  def mkEqual(x: Any, y: Any): Goal = MiniKanren.mkEqual(x, y)

  def once(g: Goal): Goal = Prelude.once(g)
  def list2pair(l: List[Any]): Any = Prelude.list2pair(l)
  def pair2list(p: Any): List[Any] = Prelude.pair2list(p)
  def car_o(p: Any, a: Any): Goal = Prelude.car_o(p, a)
  def cdr_o(p: Any, d: Any): Goal = Prelude.cdr_o(p, d)
  def pair_o(p: Any): Goal = Prelude.pair_o(p)
  def null_o(x: Any): Goal = Prelude.null_o(x)
  def list_o(l: Any): Goal = Prelude.list_o(l)
  def append_o(l1: Any, l2: Any, l3: Any): Goal = Prelude.append_o(l1, l2, l3)
  def member_o(x: Any, l: Any): Goal = Prelude.member_o(x, l)
  def any_o(g: Goal): Goal = Prelude.any_o(g)
  def never_o: Goal = Prelude.never_o
  def always_o: Goal = Prelude.always_o

  def add_o(n: Any, m: Any, k: Any): Goal = MKMath.add_o(n, m, k)
  def sub_o(n: Any, m: Any, k: Any): Goal = MKMath.sub_o(n, m, k)
  def mul_o(n: Any, m: Any, p: Any): Goal = MKMath.mul_o(n, m, p)
  def div_o(n: Any, m: Any, q: Any): Goal = MKMath.div_o(n, m, q)
  def mod_o(n: Any, m: Any, r: Any): Goal = MKMath.mod_o(n, m, r)
  def lt_o(n: Any, m: Any): Goal = MKMath.lt_o(n, m)
  def gt_o(n: Any, m: Any): Goal = MKMath.gt_o(n, m)
  def le_o(n: Any, m: Any): Goal = MKMath.le_o(n, m)
  def ge_o(n: Any, m: Any): Goal = MKMath.ge_o(n, m)
  def eq_num_o(n: Any, m: Any): Goal = MKMath.eq_num_o(n, m)
  def ne_num_o(n: Any, m: Any): Goal = MKMath.ne_num_o(n, m)

  def atom_concat(atom1: Any, atom2: Any, atom3: Any): Goal = StringOps.atom_concat(atom1, atom2, atom3)
  def atom_codes(atom: Any, codes: Any): Goal = StringOps.atom_codes(atom, codes)
  def atom_chars(atom: Any, chars: Any): Goal = StringOps.atom_chars(atom, chars)
  def atom_length(atom: Any, length: Any): Goal = StringOps.atom_length(atom, length)
  def atom_string(atom: Any, str: Any): Goal = StringOps.atom_string(atom, str)
  def sub_atom(atom: Any, before: Any, length: Any, after: Any, subAtom: Any): Goal =
    StringOps.sub_atom(atom, before, length, after, subAtom)
  def upcase_atom(atom: Any, upper: Any): Goal = StringOps.upcase_atom(atom, upper)
  def downcase_atom(atom: Any, lower: Any): Goal = StringOps.downcase_atom(atom, lower)
  def atom_number(atom: Any, number: Any): Goal = StringOps.atom_number(atom, number)
  def number_codes(number: Any, codes: Any): Goal = StringOps.number_codes(number, codes)
  def number_chars(number: Any, chars: Any): Goal = StringOps.number_chars(number, chars)

  def length_o(list: Any, length: Any): Goal = ListOps.length_o(list, length)
  def reverse_o(list: Any, reversed: Any): Goal = ListOps.reverse_o(list, reversed)
  def nth0_o(n: Any, list: Any, elem: Any): Goal = ListOps.nth0_o(n, list, elem)
  def nth1_o(n: Any, list: Any, elem: Any): Goal = ListOps.nth1_o(n, list, elem)
  def last_o(list: Any, last: Any): Goal = ListOps.last_o(list, last)
  def prefix_o(prefix: Any, list: Any): Goal = ListOps.prefix_o(prefix, list)
  def suffix_o(suffix: Any, list: Any): Goal = ListOps.suffix_o(suffix, list)
  def select_o(elem: Any, list: Any, rest: Any): Goal = ListOps.select_o(elem, list, rest)
}
