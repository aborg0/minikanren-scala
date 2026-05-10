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

  property("list2pair and pair2list available from AllOps") = {
    val list = List(1, 2, 3)
    val pair = list2pair(list)
    val reconstructed = pair2list(pair)
    reconstructed == list
  }
}
