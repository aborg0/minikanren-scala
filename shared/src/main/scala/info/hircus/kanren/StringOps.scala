package info.hircus.kanren

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * String and atom operations for miniKanren.
  * These operations work with atoms (symbols) and their string representations.
  */
@JSExportTopLevel("miniKanrenStringOps")
@JSExportAll
object StringOps {

  /**
    * atom_concat(+Atom1, +Atom2, ?Atom3)
    * Concatenates two atoms/strings.
    * atom_concat(hello, world, X) => X = helloworld
    *
    * @param atom1 first atom
    * @param atom2 second atom
    * @param atom3 result atom
    */
  def atom_concat(atom1: Any, atom2: Any, atom3: Any): Goal = {
    val s1 = atom1.toString
    val s2 = atom2.toString
    val result = s1 + s2
    mkEqual(result, atom3)
  }

  /**
    * atom_codes(?Atom, ?CodeList)
    * Converts between an atom and a list of character codes.
    * atom_codes(hello, X) => X = [104, 101, 108, 108, 111]
    *
    * @param atom an atom or variable
    * @param codes a list of character codes or variable
    */
  def atom_codes(atom: Any, codes: Any): Goal = (atom, codes) match {
    case (a: String, _) =>
      val codeList = a.map(_.toInt).toList
      mkEqual(list2pair(codeList), codes)
    case (_, c) =>
      // Decode mode: codes to atom
      val codesPair = pair2list(c)
      try {
        val chars = codesPair.map(_.asInstanceOf[Int].toChar).mkString
        mkEqual(chars, atom)
      } catch {
        case _: Exception => fail
      }
  }

  /**
    * atom_chars(?Atom, ?CharList)
    * Converts between an atom and a list of characters.
    * atom_chars(hello, X) => X = [h, e, l, l, o]
    *
    * @param atom an atom or variable
    * @param chars a list of characters or variable
    */
  def atom_chars(atom: Any, chars: Any): Goal = (atom, chars) match {
    case (a: String, _) =>
      val charList = a.toList.map(c => c.toString)
      mkEqual(list2pair(charList), chars)
    case (_, c) =>
      val charPair = pair2list(c)
      try {
        val str = charPair.map(_.toString).mkString
        mkEqual(str, atom)
      } catch {
        case _: Exception => fail
      }
  }

  /**
    * atom_length(+Atom, ?Length)
    * Unifies Length with the number of characters in Atom.
    * atom_length(hello, X) => X = 5
    *
    * @param atom an atom
    * @param length the length or variable
    */
  def atom_length(atom: Any, length: Any): Goal = {
    val len = atom.toString.length
    mkEqual(len, length)
  }

  /**
    * atom_string(?Atom, ?String)
    * Converts between atoms and strings.
    * In this implementation, atoms and strings are largely interchangeable.
    *
    * @param atom an atom or variable
    * @param str a string or variable
    */
  def atom_string(atom: Any, str: Any): Goal = {
    mkEqual(atom.toString, str.toString)
  }

  /**
    * sub_atom(+Atom, ?Before, ?Length, ?After, ?SubAtom)
    * Extracts a substring from an atom.
    * sub_atom(hello, 1, 3, _, X) => X = ell
    *
    * @param atom the source atom
    * @param before number of chars before substring
    * @param length length of substring
    * @param after number of chars after substring
    * @param subAtom the extracted substring
    */
  def sub_atom(atom: Any, before: Any, length: Any, after: Any, subAtom: Any): Goal = {
    val atomStr = atom.toString
    val atomLen = atomStr.length

    (before, length) match {
      case (b: Int, l: Int) if b >= 0 && l >= 0 && b + l <= atomLen =>
        val sub = atomStr.substring(b, b + l)
        val a = atomLen - b - l
        all(mkEqual(b, before),
          mkEqual(l, length),
          mkEqual(a, after),
          mkEqual(sub, subAtom))
      case _ =>
        // Generate all possible substrings
        { (s: Subst) =>
          var goal: Goal = fail
          for (b <- 0 to atomLen; l <- 0 to (atomLen - b)) {
            val sub = atomStr.substring(b, b + l)
            val a = atomLen - b - l
            goal = any_e(goal, all(mkEqual(b, before),
              mkEqual(l, length),
              mkEqual(a, after),
              mkEqual(sub, subAtom)))
          }
          goal(s)
        }
    }
  }

  /**
    * upcase_atom(+Atom, ?UppercaseAtom)
    * Converts an atom to uppercase.
    *
    * @param atom the source atom
    * @param upper the uppercase version
    */
  def upcase_atom(atom: Any, upper: Any): Goal = {
    mkEqual(atom.toString.toUpperCase, upper)
  }

  /**
    * downcase_atom(+Atom, ?LowercaseAtom)
    * Converts an atom to lowercase.
    *
    * @param atom the source atom
    * @param lower the lowercase version
    */
  def downcase_atom(atom: Any, lower: Any): Goal = {
    mkEqual(atom.toString.toLowerCase, lower)
  }

  /**
    * atom_number(?Atom, ?Number)
    * Bidirectional conversion between atoms and numeric values.
    * atom_number('42', X) => X = 42
    * atom_number(X, 42) => X = '42'
    *
    * @param atom an atom representing a number or variable
    * @param number a numeric value or variable
    */
  def atom_number(atom: Any, number: Any): Goal = (atom, number) match {
    case (a: String, _) =>
      try {
        val num = a.toInt
        mkEqual(num, number)
      } catch {
        case _: NumberFormatException =>
          try {
            val num = a.toDouble
            mkEqual(num, number)
          } catch {
            case _: NumberFormatException => fail
          }
      }
    case (_, n: Int) => mkEqual(n.toString, atom)
    case (_, n: Double) => mkEqual(n.toString, atom)
    case _ => fail
  }

  /**
    * number_codes(?Number, ?Codes)
    * Bidirectional conversion between numbers and character code lists.
    * number_codes(42, X) => X = [52, 50]
    * number_codes(X, [52, 50]) => X = 42
    *
    * @param number a numeric value or variable
    * @param codes a list of character codes or variable
    */
  def number_codes(number: Any, codes: Any): Goal = (number, codes) match {
    case (n: Int, _) =>
      val codeList = n.toString.map(_.toInt).toList
      mkEqual(list2pair(codeList), codes)
    case (n: Double, _) =>
      val codeList = n.toString.map(_.toInt).toList
      mkEqual(list2pair(codeList), codes)
    case (_, c) =>
      // Decode mode: codes to number
      val codesPair = pair2list(c)
      try {
        val chars = codesPair.map(_.asInstanceOf[Int].toChar).mkString
        try {
          val num = chars.toInt
          mkEqual(num, number)
        } catch {
          case _: NumberFormatException =>
            val num = chars.toDouble
            mkEqual(num, number)
        }
      } catch {
        case _: Exception => fail
      }
  }

  /**
    * number_chars(?Number, ?Chars)
    * Bidirectional conversion between numbers and character lists.
    * number_chars(42, X) => X = ['4', '2']
    * number_chars(X, ['4', '2']) => X = 42
    *
    * @param number a numeric value or variable
    * @param chars a list of characters or variable
    */
  def number_chars(number: Any, chars: Any): Goal = (number, chars) match {
    case (n: Int, _) =>
      val charList = n.toString.toList.map(c => c.toString)
      mkEqual(list2pair(charList), chars)
    case (n: Double, _) =>
      val charList = n.toString.toList.map(c => c.toString)
      mkEqual(list2pair(charList), chars)
    case (_, c) =>
      // Decode mode: chars to number
      val charPair = pair2list(c)
      try {
        val str = charPair.map(_.toString).mkString
        try {
          val num = str.toInt
          mkEqual(num, number)
        } catch {
          case _: NumberFormatException =>
            val num = str.toDouble
            mkEqual(num, number)
        }
      } catch {
        case _: Exception => fail
      }
  }
}
