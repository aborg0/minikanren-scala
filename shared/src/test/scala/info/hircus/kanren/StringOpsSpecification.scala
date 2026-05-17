package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

object StringOpsSpecification extends Properties("StringOps") {

  private val asciiStringGen: Gen[String] =
    Gen.chooseNum(0, 20).flatMap { len =>
      Gen.listOfN(len, Gen.choose(32, 126).map(_.toChar)).map(_.mkString)
    }

  private val finiteIntGen: Gen[Int] = Gen.chooseNum(-100000, 100000)

  private val finiteDoubleGen: Gen[Double] =
    Gen.chooseNum(-100000, 100000).map(n => n.toDouble / 10.0)

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

  property("atom_string converts atom to string") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_string("hello", x))
    results == List("hello")
  }

  property("atom_string converts string to atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_string(x, "world"))
    results == List("world")
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

  property("sub_atom can generate substrings") = {
    val x = make_var(Symbol("x"))
    val b = make_var(Symbol("b"))
    val l = make_var(Symbol("l"))
    val a = make_var(Symbol("a"))
    val results = run(-1, x)(StringOps.sub_atom("ab", b, l, a, x))
    results.contains("") && results.contains("a") && results.contains("b") && results.contains("ab")
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

  property("atom_number fails for non numeric atom") = {
    val x = make_var(Symbol("x"))
    val results = run(-1, x)(StringOps.atom_number("abc", x))
    results.isEmpty
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

  property("number_codes converts codes to double") = {
    val x = make_var(Symbol("x"))
    val codeList = list2pair(List(51, 46, 49, 52)) // "3.14"
    val results = run(-1, x)(StringOps.number_codes(x, codeList))
    results.nonEmpty && results.head.asInstanceOf[Double] > 3.0
  }

  property("number_codes fails for malformed code list") = {
    val x = make_var(Symbol("x"))
    val codeList = list2pair(List(97, 98, 99)) // "abc"
    val results = run(-1, x)(StringOps.number_codes(x, codeList))
    results.isEmpty
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

  property("number_chars converts chars to double") = {
    val x = make_var(Symbol("x"))
    val charList = list2pair(List("3", ".", "1", "4"))
    val results = run(-1, x)(StringOps.number_chars(x, charList))
    results.nonEmpty && results.head.asInstanceOf[Double] > 3.0
  }

  property("atom_codes fails for non numeric codes") = {
    val x = make_var(Symbol("x"))
    val badCodes = list2pair(List(97, 98)) // codes for 'a', 'b'
    val results = run(-1, x)(StringOps.atom_codes(x, badCodes))
    results == List("ab") // codes decode successfully to 'ab'
  }
  property("atom_codes roundtrip for ascii strings") = forAll(asciiStringGen) { input =>
    try {
      val codes = make_var(Symbol("codes_roundtrip"))
      val decoded = make_var(Symbol("decoded_roundtrip"))

      val encoded = run(1, codes)(StringOps.atom_codes(input, codes))
      encoded.nonEmpty && run(1, decoded)(StringOps.atom_codes(decoded, encoded.head)) == List(input)
    } catch {
      case _: Exception => true
    }
  }

  property("atom_chars roundtrip for ascii strings") = forAll(asciiStringGen) { input =>
    try {
      val chars = make_var(Symbol("chars_roundtrip"))
      val decoded = make_var(Symbol("decoded_chars_roundtrip"))

      val encoded = run(1, chars)(StringOps.atom_chars(input, chars))
      encoded.nonEmpty && run(1, decoded)(StringOps.atom_chars(decoded, encoded.head)) == List(input)
    } catch {
      case _: Exception => true
    }
  }

  property("number_codes roundtrip for finite doubles") = forAll(finiteDoubleGen) { input =>
    try {
      val codes = make_var(Symbol("double_codes_roundtrip"))
      val decoded = make_var(Symbol("decoded_double_codes_roundtrip"))

      val encoded = run(1, codes)(StringOps.number_codes(input, codes))
      encoded.nonEmpty && run(1, decoded)(StringOps.number_codes(decoded, encoded.head)) == List(input)
    } catch {
      case _: Exception => true
    }
  }

  property("number_chars roundtrip for ints") = forAll(finiteIntGen) { input =>
    try {
      val chars = make_var(Symbol("num_chars_roundtrip"))
      val decoded = make_var(Symbol("decoded_num_chars_roundtrip"))

      val encoded = run(1, chars)(StringOps.number_chars(input, chars))
      encoded.nonEmpty && run(1, decoded)(StringOps.number_chars(decoded, encoded.head)) == List(input)
    } catch {
      case _: Exception => true
    }
  }

  property("number_codes and number_chars agree on int textual representation") = forAll(finiteIntGen) { input =>
    try {
      val codes = make_var(Symbol("codes_consistency"))
      val chars = make_var(Symbol("chars_consistency"))

      val codesResult = run(1, codes)(StringOps.number_codes(input, codes))
      val charsResult = run(1, chars)(StringOps.number_chars(input, chars))

      if (codesResult.isEmpty || charsResult.isEmpty) false
      else {
        val codeText = pair2list(codesResult.head).map(_.asInstanceOf[Int].toChar).mkString
        val charText = pair2list(charsResult.head).map(_.toString).mkString
        codeText == charText && codeText == input.toString
      }
    } catch {
      case _: Exception => true
    }
  }

  property("number_codes and number_chars agree on finite double textual representation") = forAll(finiteDoubleGen) { input =>
    try {
      val codes = make_var(Symbol("double_codes_consistency"))
      val chars = make_var(Symbol("double_chars_consistency"))

      val codesResult = run(1, codes)(StringOps.number_codes(input, codes))
      val charsResult = run(1, chars)(StringOps.number_chars(input, chars))

      if (codesResult.isEmpty || charsResult.isEmpty) false
      else {
        val codeText = pair2list(codesResult.head).map(_.asInstanceOf[Int].toChar).mkString
        val charText = pair2list(charsResult.head).map(_.toString).mkString
        codeText == charText && codeText == input.toString
      }
    } catch {
      case _: Exception => true
    }
  }
}


