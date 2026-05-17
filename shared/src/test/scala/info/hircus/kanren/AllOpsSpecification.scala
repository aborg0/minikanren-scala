package info.hircus.kanren

import info.hircus.kanren.AllOps._
import org.scalacheck.Properties

object AllOpsSpecification extends Properties("AllOps") {

  property("mod_o available from AllOps") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(mod_o(10, 3, x))
    results == List(1)
  }

  property("atom_number available from AllOps") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(atom_number("42", x))
    results == List(42)
  }

  property("number_codes available from AllOps") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(number_codes(42, x))
    results.length == 1 && results.head != Nil
  }

  property("number_chars available from AllOps") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(number_chars(42, x))
    results.length == 1 && results.head != Nil
  }

  property("member_o available from AllOps") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(member_o(2, list))
    results.length == 1
  }

  property("append_o available from AllOps") = {
    val x = make_var(Symbol("x"))
    val list1 = list2pair(List(1, 2))
    val list2 = list2pair(List(3, 4))
    val results = run(-1, x)(append_o(list1, list2, x))
    results.length == 1
  }

  property("length_o (from ListOps) available from AllOps") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(length_o(list, x))
    results == List(3)
  }

  property("reverse_o available from AllOps") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(reverse_o(list, x))
    results.length == 1
  }

  property("atom_concat available from AllOps") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(atom_concat("hello", "world", x))
    results == List("helloworld")
  }

  property("make_var available from AllOps") = {
    val x = make_var(Symbol("x"))
    x != null
  }

  property("make_var increments counts per symbol") = {
    val symbol = Symbol("counter_test")
    val first = make_var(symbol)
    val second = make_var(symbol)
    val other = make_var(Symbol("counter_test_other"))

    first.name == symbol &&
    first.count + 1 == second.count &&
    other.count == 0
  }

  property("list2pair and pair2list available from AllOps") = {
    val list = List(1, 2, 3)
    val pair = list2pair(list)
    val reconstructed = pair2list(pair)
    reconstructed == list
  }

  property("core goal combinators available from AllOps") = {
    val x = make_var(Symbol("x"))
    val succeedResult = run(1, x)(all(succeed, mkEqual(x, 7)))
    val failResult = run(1, x)(all(fail, mkEqual(x, 7)))
    val anyResult = run(-1, x)(any_e(mkEqual(x, 1), mkEqual(x, 2)))
    val bothResult = run(1, x)(both(mkEqual(x, 5), mkEqual(x, 5)))
    val ifResult = run(1, x)(if_e(mkEqual(1, 1), mkEqual(x, 9), mkEqual(x, 0)))

    succeedResult == List(7) &&
      failResult.isEmpty &&
      anyResult == List(1, 2) &&
      bothResult == List(5) &&
      ifResult == List(9)
  }

  property("prelude and list wrappers available from AllOps") = {
    val pair = list2pair(List(1, 2, 3))

    val h = make_var(Symbol("h"))
    val t = make_var(Symbol("t"))
    val c = make_var(Symbol("c"))
    val n0 = make_var(Symbol("n0"))
    val n1 = make_var(Symbol("n1"))
    val last = make_var(Symbol("last"))
    val selectedRest = make_var(Symbol("selectedRest"))

    val carResult = run(1, h)(car_o(pair, h))
    val cdrResult = run(1, t)(cdr_o(pair, t))
    val pairResult = run(1, c)(all(pair_o(pair), mkEqual(c, "ok")))
    val nullResult = run(1, c)(all(null_o(Nil), mkEqual(c, "ok")))
    val listResult = run(1, c)(all(list_o(pair), mkEqual(c, "ok")))
    val nth0Result = run(1, n0)(nth0_o(0, pair, n0))
    val nth1Result = run(1, n1)(nth1_o(2, pair, n1))
    val lastResult = run(1, last)(last_o(pair, last))
    val prefixResult = run(1, c)(all(prefix_o(list2pair(List(1, 2)), pair), mkEqual(c, "ok")))
    val suffixResult = run(1, c)(all(suffix_o(list2pair(List(2, 3)), pair), mkEqual(c, "ok")))
    val selectResult = run(1, selectedRest)(select_o(2, pair, selectedRest))

    carResult == List(1) &&
      cdrResult == List(list2pair(List(2, 3))) &&
      pairResult == List("ok") &&
      nullResult == List("ok") &&
      listResult == List("ok") &&
      nth0Result == List(1) &&
      nth1Result == List(2) &&
      lastResult == List(3) &&
      prefixResult == List("ok") &&
      suffixResult == List("ok") &&
      selectResult == List(list2pair(List(1, 3)))
  }

  property("string wrappers available from AllOps") = {
    val x = make_var(Symbol("x"))

    val codesResult = run(1, x)(atom_codes("ab", x))
    val charsResult = run(1, x)(atom_chars("ab", x))
    val lengthResult = run(1, x)(atom_length("hello", x))
    val stringResult = run(1, x)(atom_string("hello", x))
    val subAtomResult = run(1, x)(sub_atom("banana", 1, 3, 2, x))
    val upcaseResult = run(1, x)(upcase_atom("hello", x))
    val downcaseResult = run(1, x)(downcase_atom("HELLO", x))

    codesResult.nonEmpty &&
      charsResult.nonEmpty &&
      lengthResult == List(5) &&
      stringResult == List("hello") &&
      subAtomResult == List("ana") &&
      upcaseResult == List("HELLO") &&
      downcaseResult == List("hello")
  }
}
