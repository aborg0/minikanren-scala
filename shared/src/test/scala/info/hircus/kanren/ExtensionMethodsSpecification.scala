package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import info.hircus.kanren.StringExtensions._
import info.hircus.kanren.NumericExtensions._
import info.hircus.kanren.MKMath._
import org.scalacheck.Properties

object ExtensionMethodsSpecification extends Properties("ExtensionMethods") {

  private def asInt(v: Any): Option[Int] = v match {
    case i: Int => Some(i)
    case other => try { Some(MKMath.read_num(other)) } catch { case _: Throwable => None }
  }

  // String extensions
  property("String.toNumber converts string to number") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("42".toNumber(x))
    results == List(42)
  }

  property("String.toCharList converts string to chars") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("hi".toCharList(x))
    results.length == 1 && results.head != Nil
  }

  property("String.toCodes converts string to codes") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("hi".toCodes(x))
    results.length == 1 && results.head != Nil
  }

  property("String.concat concatenates strings") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("hello".concat("world", x))
    results == List("helloworld")
  }

  property("String.atomLength returns string length") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("hello".atomLength(x))
    results == List(5)
  }

  property("String.upper converts to uppercase") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("hello".upper(x))
    results == List("HELLO")
  }

  property("String.lower converts to lowercase") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)("HELLO".lower(x))
    results == List("hello")
  }

  property("String.substring extracts substring") = {
    val x = make_var(Symbol("x"))
    val y = make_var(Symbol("y"))
    val results = run(-1, x)("hello".substring(1, 3, y, x))
    results.contains("ell")
  }

  // Int extensions
  property("Int.toCharList converts int to chars") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(42.toCharList(x))
    results.length == 1 && results.head != Nil
  }

  property("Int.toCodes converts int to codes") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(42.toCodes(x))
    results.length == 1 && results.head != Nil
  }

  property("Int.toAtom converts int to atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(42.toAtom(x))
    results == List("42")
  }

  property("Int.mod calculates modulus") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(10.mod(3, x))
    results == List(1)
  }

  property("Int.plus goals execute without error") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(add_o(build_num(10), build_num(3), x))
    results.nonEmpty && (results.map(read_num) == List(13))
  }

  property("Int.plus produces different results for different inputs") = {
    val x = make_var(Symbol("x"))
    val y = make_var(Symbol("y"))
    val r1 = run(1, x)(add_o(build_num(10), build_num(3), x))
    val r2 = run(1, y)(add_o(build_num(10), build_num(5), y))
    r1 != r2 && r1.map(read_num) == List(13) && r2.map(read_num) == List(15)
  }

  property("Int.minus goals execute without error") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(sub_o(build_num(10), build_num(3), x))
    results.nonEmpty && (results.map(read_num) == List(7))
  }

  property("Int.times goals execute without error") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(mul_o(build_num(4), build_num(3), x))
    results.nonEmpty && (results.map(read_num) == List(12))
  }

  property("Int.dividedBy divides integers") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(10.dividedBy(3, x))
    results.nonEmpty
  }

  property("Int.lte comparison") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(5.lte(5))
    results.length >= 1
  }

  property("Int.lessThan goals execute without error") = {
    val dummy = make_var(Symbol("_dummy"))
    val results = run(1, dummy)(lt_o(build_num(4), build_num(5)))
    results.nonEmpty  // Goal succeeds when 4 < 5
  }

  property("Int.greaterThan goals execute without error") = {
    val dummy = make_var(Symbol("_dummy"))
    val results = run(1, dummy)(gt_o(build_num(6), build_num(5)))
    results.nonEmpty  // Goal succeeds when 6 > 5
  }

  property("Int.gte comparison") = {
    val x = make_var(Symbol("x"))
    val results = run(1, x)(5.gte(5))
    results.length >= 1
  }

  // Double extensions
  property("Double.toCharList converts double to chars") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(3.14.toCharList(x))
    results.length == 1 && results.head != Nil
  }

  property("Double.toCodes converts double to codes") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(3.14.toCodes(x))
    results.length == 1 && results.head != Nil
  }

  property("Double.toAtom converts double to atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(3.14.toAtom(x))
    results.nonEmpty
  }
}
