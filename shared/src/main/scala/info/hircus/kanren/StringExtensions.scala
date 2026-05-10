package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Extension methods for String in miniKanren context.
  * Enables: "42".toNumber(x), "hello".toCharList(x), etc.
  */
@JSExportTopLevel("miniKanrenStringExtensions")
@JSExportAll
object StringExtensions {
  
  extension (atom: String) {
    
    /**
      * Convert string to number: "42".toNumber(x) => x = 42
      * Equivalent to: atom_number("42", x)
      */
    def toNumber(number: Any): MiniKanren.Goal =
      StringOps.atom_number(atom, number)

    /**
      * Convert string to character list: "hi".toCharList(x) => x = ['h', 'i']
      * Equivalent to: atom_chars("hi", x)
      */
    def toCharList(chars: Any): MiniKanren.Goal =
      StringOps.atom_chars(atom, chars)

    /**
      * Convert string to character code list: "hi".toCodes(x) => x = [104, 105]
      * Equivalent to: atom_codes("hi", x)
      */
    def toCodes(codes: Any): MiniKanren.Goal =
      StringOps.atom_codes(atom, codes)

    /**
      * Concatenate with another atom: "hello".concat("world", x) => x = "helloworld"
      * Equivalent to: atom_concat("hello", "world", x)
      */
    def concat(other: Any, result: Any): MiniKanren.Goal =
      StringOps.atom_concat(atom, other, result)

    /**
      * Get string length: "hello".length(x) => x = 5
      * Equivalent to: atom_length("hello", x)
      */
    def atomLength(length: Any): MiniKanren.Goal =
      StringOps.atom_length(atom, length)

    /**
      * Convert to uppercase: "hello".upper(x) => x = "HELLO"
      * Equivalent to: upcase_atom("hello", x)
      */
    def upper(result: Any): MiniKanren.Goal =
      StringOps.upcase_atom(atom, result)

    /**
      * Convert to lowercase: "HELLO".lower(x) => x = "hello"
      * Equivalent to: downcase_atom("HELLO", x)
      */
    def lower(result: Any): MiniKanren.Goal =
      StringOps.downcase_atom(atom, result)

    /**
      * Extract substring: "hello".substring(1, 3, x) => x = "ell"
      * Equivalent to: sub_atom("hello", 1, 3, _, x)
      */
    def substring(before: Any, length: Any, after: Any, subAtom: Any): MiniKanren.Goal =
      StringOps.sub_atom(atom, before, length, after, subAtom)
  }
}
