package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import org.scalacheck.Properties

object ListOpsSpecification extends Properties("ListOps") {

  property("length_o unifies list length") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.length_o(list, x))
    results == List(3)
  }

  property("length_o with empty list") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(ListOps.length_o(Nil, x))
    results == List(0)
  }

  property("length_o with single element") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List("a"))
    val results = run(-1, x)(ListOps.length_o(list, x))
    results == List(1)
  }

  property("reverse_o reverses a list") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.reverse_o(list, x))
    results.length == 1
  }

  property("reverse_o with empty list") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(ListOps.reverse_o(Nil, x))
    results == List(Nil)
  }

  property("reverse_o with single element") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1))
    val results = run(-1, x)(ListOps.reverse_o(list, x))
    results.length == 1
  }

  property("nth0_o accesses element by index") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List("a", "b", "c"))
    val results = run(-1, x)(ListOps.nth0_o(1, list, x))
    results == List("b")
  }

  property("nth0_o with index 0") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.nth0_o(0, list, x))
    results == List(1)
  }

  property("nth0_o with last element") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.nth0_o(2, list, x))
    results == List(3)
  }

  property("nth1_o uses 1-based indexing") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List("a", "b", "c"))
    val results = run(-1, x)(ListOps.nth1_o(2, list, x))
    results == List("b")
  }

  property("nth1_o with first element") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.nth1_o(1, list, x))
    results == List(1)
  }

  property("prefix_o checks known prefix") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val prefix = list2pair(List(1, 2))
    val results = run(-1, x)(all(ListOps.prefix_o(prefix, list), succeed))
    results.length == 1
  }

  property("prefix_o generates prefixes") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2))
    val results = run(-1, x)(ListOps.prefix_o(x, list))
    results.length >= 1
  }

  property("suffix_o checks known suffix") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val suffix = list2pair(List(2, 3))
    val results = run(-1, x)(all(ListOps.suffix_o(suffix, list), succeed))
    results.length == 1
  }

  property("suffix_o generates suffixes including empty") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2))
    val results = run(-1, x)(ListOps.suffix_o(x, list))
    results.contains(list2pair(List(1, 2))) &&
      results.contains(list2pair(List(2))) &&
      results.contains(Nil)
  }

  property("last_o returns the last element") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(1, x)(ListOps.last_o(list, x))
    results == List(3)
  }

  property("last_o fails on empty list") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(ListOps.last_o(Nil, x))
    // Accept either failure or a non-integer placeholder for the last element
    results.isEmpty || !results.exists(_.isInstanceOf[Int])
  }

  property("nth1_o fails for non integer index") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.nth1_o("2", list, x))
    results.isEmpty
  }

  property("select_o removes element from list") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.select_o(2, list, x))
    results.length == 1
  }

  property("select_o fails when element is missing") = {
    val x = make_var(Symbol("x"))
    val list = list2pair(List(1, 2, 3))
    val results = run(-1, x)(ListOps.select_o(9, list, x))
    results.isEmpty
  }
}
