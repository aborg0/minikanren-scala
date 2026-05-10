package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Extension methods for Int and Double in miniKanren context.
  * Enables: 42.toCharList(x), 10.mod(3, x), etc.
  */
@JSExportTopLevel("miniKanrenNumericExtensions")
@JSExportAll
object NumericExtensions {
  
  extension (num: Int) {
    
    /**
      * Convert number to character list: 42.toCharList(x) => x = ['4', '2']
      * Equivalent to: number_chars(42, x)
      */
    def toCharList(chars: Any): MiniKanren.Goal =
      StringOps.number_chars(num, chars)

    /**
      * Convert number to character code list: 42.toCodes(x) => x = [52, 50]
      * Equivalent to: number_codes(42, x)
      */
    def toCodes(codes: Any): MiniKanren.Goal =
      StringOps.number_codes(num, codes)

    /**
      * Convert number to atom string: 42.toAtom(x) => x = "42"
      * Equivalent to: atom_number(x, 42)
      */
    def toAtom(atom: Any): MiniKanren.Goal =
      StringOps.atom_number(atom, num)

    /**
      * Modulus operation: 10.mod(3, x) => x = 1
      * Equivalent to: mod_o(10, 3, x)
      */
    def mod(divisor: Any, remainder: Any): MiniKanren.Goal =
      MKMath.mod_o(num, divisor, remainder)

    /**
      * Addition: 5.plus(3, x) => x = 8
      * Equivalent to: add_o(5, 3, x)
      */
    def plus(other: Any, result: Any): MiniKanren.Goal =
      MKMath.add_o(num, other, result)

    /**
      * Subtraction: 10.minus(3, x) => x = 7
      * Equivalent to: sub_o(10, 3, x)
      */
    def minus(other: Any, result: Any): MiniKanren.Goal =
      MKMath.sub_o(num, other, result)

    /**
      * Multiplication: 5.times(3, x) => x = 15
      * Equivalent to: mul_o(5, 3, x)
      */
    def times(other: Any, result: Any): MiniKanren.Goal =
      MKMath.mul_o(num, other, result)

    /**
      * Division: 10.dividedBy(3, x) => x = 3
      * Equivalent to: div_o(10, 3, x)
      */
    def dividedBy(divisor: Any, quotient: Any): MiniKanren.Goal =
      MKMath.div_o(num, divisor, quotient)

    /**
      * Less than: 5.lessThan(10) => succeeds
      * Equivalent to: lt_o(5, 10)
      */
    def lessThan(other: Any): MiniKanren.Goal =
      MKMath.lt_o(num, other)

    /**
      * Greater than: 10.greaterThan(5) => succeeds
      * Equivalent to: gt_o(10, 5)
      */
    def greaterThan(other: Any): MiniKanren.Goal =
      MKMath.gt_o(num, other)

    /**
      * Less than or equal: 5.lte(5) => succeeds
      * Equivalent to: le_o(5, 5)
      */
    def lte(other: Any): MiniKanren.Goal =
      MKMath.le_o(num, other)

    /**
      * Greater than or equal: 5.gte(5) => succeeds
      * Equivalent to: ge_o(5, 5)
      */
    def gte(other: Any): MiniKanren.Goal =
      MKMath.ge_o(num, other)
  }

  extension (num: Double) {
    
    /**
      * Convert double to character list: 3.14.toCharList(x) => x = ['3', '.', '1', '4']
      * Equivalent to: number_chars(3.14, x)
      */
    def toCharList(chars: Any): MiniKanren.Goal =
      StringOps.number_chars(num, chars)

    /**
      * Convert double to character code list: 3.14.toCodes(x)
      * Equivalent to: number_codes(3.14, x)
      */
    def toCodes(codes: Any): MiniKanren.Goal =
      StringOps.number_codes(num, codes)

    /**
      * Convert double to atom string: 3.14.toAtom(x) => x = "3.14"
      * Equivalent to: atom_number(x, 3.14)
      */
    def toAtom(atom: Any): MiniKanren.Goal =
      StringOps.atom_number(atom, num)
  }
}
