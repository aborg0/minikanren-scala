package info.hircus.kanren.lang

import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.Prelude._
import info.hircus.kanren.dslir.QueryIR
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

  property("parse and run declarative infix syntax") = {
    val source =
      """
        |const Anakin, Luke, Leia
        |var x, y, z
        |rel/2 infix fatherOf
        |Anakin fatherOf Luke
        |Anakin fatherOf Leia
        |sibling(x, y) = fatherOf(z, x) & fatherOf(z, y) & not x = y
        |ask x sibling(Luke, x)
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("Leia"))
  }

  property("parse and run prolog like syntax") = {
    val source =
      """
        |father_of(anakin, luke).
        |father_of(anakin, leia).
        |?- father_of(anakin, Y).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("luke", "leia"))
  }

  property("parse prolog rule declaration") = {
    val source =
      """
        |sibling(X, Y) :- neq(X, Y).
        |?- sibling(a, Y).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.declarations.exists {
          case QueryIR.Rule("sibling", _, _) => true
          case _ => false
        }
      case Left(_) => false
    }
  }

  property("parse and run declarative list head tail deconstruction") = {
    val source =
      """
        |var h, t
        |ask h [h | t] = [1, 2, 3]
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1))
  }

  property("parse and run prolog list head tail deconstruction") = {
    val source =
      """
        |?- eq([H|T], [1,2,3]).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1))
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

  property("parse mod_o predicate") = {
    val source =
      """
        |?- mod_o(10, 3, X).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse atom_number predicate") = {
    val source =
      """
        |?- atom_number('42', X).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse number_codes predicate") = {
    val source =
      """
        |?- number_codes(42, X).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse number_chars predicate") = {
    val source =
      """
        |?- number_chars(42, X).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse atom_number in run block with prefix call syntax") = {
    val source =
      """
        |run 1 x {
        |  atom_number "42" x
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse mod_o in run block with prefix call syntax") = {
    val source =
      """
        |run 1 x {
        |  mod_o 10 3 x
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("run atom_number in run block with prefix call syntax") = {
    val source =
      """
        |run 1 x {
        |  atom_number "42" x
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(42))
  }

  property("run mod_o in run block with prefix call syntax") = {
    val source =
      """
        |run 1 x {
        |  mod_o 10 3 x
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1))
  }
}
