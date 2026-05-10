package info.hircus.kanren.dsl3

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import org.scalacheck.Properties

object ParitySpecification extends Properties("Parity") {

  import Scala3DSL.*

  property("member_o parity with core api") = {
    val coreX = make_var(Symbol("x"))
    val coreResult = info.hircus.kanren.MiniKanren.run(-1, coreX)(info.hircus.kanren.Prelude.member_o(coreX, list2pair(List(1, 2, 3))))

    val dslX = v("x")
    val dslResult = Scala3DSL.run(dslX, limit = -1)(Scala3DSL.contains(dslX, list(1, 2, 3))).toList

    coreResult == dslResult
  }

  property("head parity with core api") = {
    val coreX = make_var(Symbol("x"))
    val coreResult = info.hircus.kanren.MiniKanren.run(-1, coreX)(info.hircus.kanren.Prelude.car_o((1, (2, Nil)), coreX))

    val dslX = v("x")
    val dslResult = Scala3DSL.run(dslX, limit = -1)(Scala3DSL.head(pair(1, list(2)), dslX)).toList

    coreResult == dslResult
  }
}
