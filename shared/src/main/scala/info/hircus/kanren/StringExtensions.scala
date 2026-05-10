package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Extension methods for String in miniKanren context.
  * Enables: "42".toNumber(x), "hello".toCharList(x), etc.
  */
@JSExportTopLevel("miniKanrenStringExtensions")
@JSExportAll
object StringExtensions {
  implicit final class StringOpsSyntax(val atom: String) extends AnyVal {

    def toNumber(number: Any): MiniKanren.Goal = StringOps.atom_number(atom, number)

    def toCharList(chars: Any): MiniKanren.Goal = StringOps.atom_chars(atom, chars)

    def toCodes(codes: Any): MiniKanren.Goal = StringOps.atom_codes(atom, codes)

    def concat(other: Any, result: Any): MiniKanren.Goal = StringOps.atom_concat(atom, other, result)

    def atomLength(length: Any): MiniKanren.Goal = StringOps.atom_length(atom, length)

    def upper(result: Any): MiniKanren.Goal = StringOps.upcase_atom(atom, result)

    def lower(result: Any): MiniKanren.Goal = StringOps.downcase_atom(atom, result)

    def substring(before: Any, length: Any, after: Any, subAtom: Any): MiniKanren.Goal =
      StringOps.sub_atom(atom, before, length, after, subAtom)
  }
}
