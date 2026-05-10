package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Extended list operations for miniKanren.
  * These relations provide common list utilities beyond the core Prelude.
  */
@JSExportTopLevel("miniKanrenListOps")
@JSExportAll
object ListOps {

  /**
    * length_o(?List, ?Length)
    * Unifies Length with the length of List.
    * length_o([1,2,3], N) => N = 3
    * If List is fresh, can generate lists of a given length.
    *
    * @param list a Kanren list or variable
    * @param length the length or variable
    */
  def length_o(list: Any, length: Any): Goal = {
    def len_acc(l: Any, acc: Int): Goal = {
      if_e(null_o(l), mkEqual(acc, length),
        { (s: Subst) =>
          val d = make_var(Symbol("d"))
          all(cdr_o(l, d),
            len_acc(d, acc + 1))(s)
        })
    }
    len_acc(list, 0)
  }

  /**
    * reverse_o(+List, ?ReversedList)
    * Unifies ReversedList with the reverse of List.
    * reverse_o([1,2,3], X) => X = [3,2,1]
    *
    * @param list the original list
    * @param reversed the reversed list
    */
  def reverse_o(list: Any, reversed: Any): Goal = {
    def rev_acc(l: Any, acc: Any): Goal = {
      if_e(null_o(l), mkEqual(acc, reversed),
        { (s: Subst) =>
          val h = make_var(Symbol("h"))
          val t = make_var(Symbol("t"))
          all(mkEqual((h, t), l),
            { (s2: Subst) =>
              rev_acc(t, (h, acc))(s2)
            })(s)
        })
    }
    rev_acc(list, Nil)
  }

  /**
    * nth0_o(?N, ?List, ?Elem)
    * Relates the 0-indexed position N in List to element Elem.
    * nth0_o(1, [a,b,c], X) => X = b
    *
    * @param n the 0-based index or variable
    * @param list the list
    * @param elem the element at position n
    */
  def nth0_o(n: Any, list: Any, elem: Any): Goal = {
    { (s: Subst) =>
      var goal: MiniKanren.Goal = fail
      var idx = 0
      var current = list
      while (idx < 1000 && !current.equals(Nil)) {
        current match {
          case (h, t) =>
            goal = any_e(goal, all(mkEqual(idx, n), mkEqual(h, elem)))
            current = t
            idx += 1
          case _ => current = Nil
        }
      }
      goal(s)
    }
  }

  /**
    * nth1_o(?N, ?List, ?Elem)
    * Relates the 1-indexed position N in List to element Elem.
    * nth1_o(2, [a,b,c], X) => X = b
    *
    * @param n the 1-based index or variable
    * @param list the list
    * @param elem the element at position n
    */
  def nth1_o(n: Any, list: Any, elem: Any): Goal = {
    // Convert 1-based index to 0-based
    (n) match {
      case i: Int => nth0_o(i - 1, list, elem)
      case _ =>
        // For symbolic computation, would need more sophisticated handling
        fail
    }
  }

  /**
    * last_o(?List, ?LastElem)
    * Unifies LastElem with the last element of List.
    * last_o([1,2,3], X) => X = 3
    *
    * @param list the list
    * @param last the last element
    */
  def last_o(list: Any, last: Any): Goal = {
    if_e(null_o(list), fail,
      if_e({ (s: Subst) =>
        val t = make_var(Symbol("t"))
        cdr_o(list, t)(s)
      }, { (s: Subst) =>
        val h = make_var(Symbol("h"))
        val t = make_var(Symbol("t"))
        all(mkEqual((h, t), list),
          { (s2: Subst) =>
            if_e(null_o(t), mkEqual(h, last), last_o(t, last))(s2)
          })(s)
      }, { (s: Subst) =>
        val h = make_var(Symbol("h"))
        car_o(list, h)(s) match {
          case _ => mkEqual(h, last)(s)
        }
      }))
  }

  /**
    * prefix_o(?Prefix, +List)
    * Succeeds if Prefix is a prefix of List.
    * prefix_o(X, [1,2,3]) generates [], [1], [1,2], [1,2,3]
    *
    * @param prefix the potential prefix or variable
    * @param list the list
    */
  def prefix_o(prefix: Any, list: Any): Goal = {
    if_e(null_o(prefix), succeed,
      if_e(null_o(list), fail,
        { (s: Subst) =>
          val ph = make_var(Symbol("ph"))
          val pt = make_var(Symbol("pt"))
          val lh = make_var(Symbol("lh"))
          val lt = make_var(Symbol("lt"))
          all(mkEqual((ph, pt), prefix),
            mkEqual((lh, lt), list),
            mkEqual(ph, lh),
            prefix_o(pt, lt))(s)
        }))
  }

  /**
    * suffix_o(?Suffix, +List)
    * Succeeds if Suffix is a suffix of List.
    * suffix_o(X, [1,2,3]) generates [1,2,3], [2,3], [3], []
    *
    * @param suffix the potential suffix or variable
    * @param list the list
    */
  def suffix_o(suffix: Any, list: Any): Goal = {
    if_e(null_o(list), null_o(suffix),
      if_e(mkEqual(list, suffix), succeed,
        { (s: Subst) =>
          val t = make_var(Symbol("t"))
          all(cdr_o(list, t),
            suffix_o(suffix, t))(s)
        }))
  }

  /**
    * select_o(?Elem, ?List, ?Rest)
    * True if List = [... Elem ...] and Rest = List with Elem removed once.
    * select_o(2, [1,2,3], X) => X = [1,3]
    *
    * @param elem the element to select
    * @param list the original list
    * @param rest the list with elem removed
    */
  def select_o(elem: Any, list: Any, rest: Any): Goal = {
    if_e(null_o(list), fail,
      if_e({ (s: Subst) =>
        val h = make_var(Symbol("h"))
        all(car_o(list, h),
          mkEqual(h, elem))(s)
      }, { (s: Subst) =>
        cdr_o(list, rest)(s)
      }, { (s: Subst) =>
        val h = make_var(Symbol("h"))
        val t = make_var(Symbol("t"))
        val r = make_var(Symbol("r"))
        all(mkEqual((h, t), list),
          select_o(elem, t, r),
          mkEqual((h, r), rest))(s)
      }))
  }
}
