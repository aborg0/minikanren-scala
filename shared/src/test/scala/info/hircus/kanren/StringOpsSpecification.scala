package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import org.scalacheck.Properties

object StringOpsSpecification extends Properties("StringOps") {

  property("atom_concat concatenates atoms") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_concat("hello", "world", x))
    results == List("helloworld")
  }

  property("atom_concat with numeric atoms") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_concat(123, 456, x))
    results.nonEmpty && results.head.toString == "123456"
  }

  property("atom_codes converts atom to codes") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_codes("hi", x))
    results.length == 1 && results.head != Nil
  }

  property("atom_codes converts codes to atom") = {
    val x = make_var(Symbol("x"))
    val codeList = list2pair(List(104, 105)) // "hi"
    val results = run(-1, x)(StringOps.atom_codes(x, codeList))
    results.nonEmpty
  }

  property("atom_chars converts atom to chars") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_chars("hi", x))
    results.length == 1 && results.head != Nil
  }

  property("atom_chars with empty string") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_chars("", x))
    results.length == 1 && results.head == Nil
  }

  property("atom_length returns correct length") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_length("hello", x))
    results == List(5)
  }

  property("atom_length with empty atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_length("", x))
    results == List(0)
  }

  property("upcase_atom converts to uppercase") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.upcase_atom("hello", x))
    results == List("HELLO")
  }

  property("downcase_atom converts to lowercase") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.downcase_atom("HELLO", x))
    results == List("hello")
  }

  property("sub_atom extracts substring") = {
    val x = make_var(Symbol("x"))
    val z = make_var(Symbol("z"))
    val results = run(-1, x)(StringOps.sub_atom("hello", 1, 3, z, x))
    results.contains("ell")
  }

  property("atom_number converts atom to int") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_number("42", x))
    results == List(42)
  }

  property("atom_number converts int to atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_number(x, 42))
    results == List("42")
  }

  property("atom_number converts atom to double") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_number("3.14", x))
    results.nonEmpty && results.head.asInstanceOf[Double] > 3.0
  }

  property("number_codes converts int to codes") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.number_codes(42, x))
    results.length == 1 && results.head != Nil
  }

  property("number_codes converts codes to int") = {
    val x = make_var(Symbol("x"))
    val codeList = list2pair(List(52, 50)) // "42"
    val results = run(-1, x)(StringOps.number_codes(x, codeList))
    results == List(42)
  }

  property("number_chars converts int to chars") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.number_chars(42, x))
    results.length == 1 && results.head != Nil
  }

  property("number_chars converts chars to int") = {
    val x = make_var(Symbol("x"))
    val charList = list2pair(List("4", "2"))
    val results = run(-1, x)(StringOps.number_chars(x, charList))
    results == List(42)
  }
}


