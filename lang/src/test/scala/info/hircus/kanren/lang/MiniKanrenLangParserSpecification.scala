package info.hircus.kanren.lang

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import org.scalacheck.Properties

object MiniKanrenLangParserSpecification extends Properties("MiniKanrenLangParser") {

  property("parse and run equality") = {
    val source =
      """
        |run 1 x {
        |  eq x 7
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(7))
  }

  property("parse and run disjunction") = {
    val source =
      """
        |run -1 x {
        |  any {
        |    eq x 1;
        |    eq x 2
        |  }
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1, 2))
  }

  property("parse and run relation call") = {
    val source =
      """
        |run -1 x {
        |  member_o(x, [1, 2, 3])
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1, 2, 3))
  }

  property("member_o parity with core api") = {
    val source =
      """
        |run -1 x {
        |  member_o(x, [1, 2, 3])
        |}
        |""".stripMargin

    val coreX = make_var(Symbol("x"))
    val core = run(-1, coreX)(member_o(coreX, list2pair(List(1, 2, 3))))

    MiniKanrenLang.run(source) == Right(core)
  }

  property("parser reports location") = {
    val source =
      """
        |run 1 x {
        |  eq x
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) match {
      case Left(err) => err.line >= 2 && err.column > 0 && err.message.nonEmpty
      case Right(_) => false
    }
  }
}
