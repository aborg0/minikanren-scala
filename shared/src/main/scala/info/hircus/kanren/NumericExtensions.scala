package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Extension methods for Int and Double in miniKanren context.
  * Enables: 42.toCharList(x), 10.mod(3, x), etc.
  */
@JSExportTopLevel("miniKanrenNumericExtensions")
@JSExportAll
object NumericExtensions {
  implicit final class IntOpsSyntax(val num: Int) extends AnyVal {

    def toCharList(chars: Any): MiniKanren.Goal = StringOps.number_chars(num, chars)

    def toCodes(codes: Any): MiniKanren.Goal = StringOps.number_codes(num, codes)

    def toAtom(atom: Any): MiniKanren.Goal = StringOps.atom_number(atom, num)

    def mod(divisor: Any, remainder: Any): MiniKanren.Goal = MKMath.mod_o(num, divisor, remainder)

    def plus(other: Any, result: Any): MiniKanren.Goal = MKMath.add_o(num, other, result)

    def minus(other: Any, result: Any): MiniKanren.Goal = MKMath.sub_o(num, other, result)

    def times(other: Any, result: Any): MiniKanren.Goal = MKMath.mul_o(num, other, result)

    def dividedBy(divisor: Any, quotient: Any): MiniKanren.Goal = MKMath.div_o(num, divisor, quotient)

    def lessThan(other: Any): MiniKanren.Goal = MKMath.lt_o(num, other)

    def greaterThan(other: Any): MiniKanren.Goal = MKMath.gt_o(num, other)

    def lte(other: Any): MiniKanren.Goal = MKMath.le_o(num, other)

    def gte(other: Any): MiniKanren.Goal = MKMath.ge_o(num, other)
  }

  implicit final class DoubleOpsSyntax(val num: Double) extends AnyVal {

    def toCharList(chars: Any): MiniKanren.Goal = StringOps.number_chars(num, chars)

    def toCodes(codes: Any): MiniKanren.Goal = StringOps.number_codes(num, codes)

    def toAtom(atom: Any): MiniKanren.Goal = StringOps.atom_number(atom, num)
  }
}
