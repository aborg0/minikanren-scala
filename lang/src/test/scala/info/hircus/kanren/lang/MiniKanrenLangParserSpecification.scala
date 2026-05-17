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

  property("parse prolog arithmetic operator + into add_o") = {
    val source =
      """
        |?- X = 2 + 3.
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.goals == List(
          QueryIR.Rel("add_o", List(QueryIR.Atom(2), QueryIR.Atom(3), QueryIR.Ref("X")))
        )
      case Left(_) => false
    }
  }

  property("parse prolog arithmetic operator - into sub_o") = {
    val source =
      """
        |?- X = 10 - 3.
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.goals == List(
          QueryIR.Rel("sub_o", List(QueryIR.Atom(10), QueryIR.Atom(3), QueryIR.Ref("X")))
        )
      case Left(_) => false
    }
  }

  property("parse prolog arithmetic operator * into mul_o") = {
    val source =
      """
        |?- X = 4 * 3.
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.goals == List(
          QueryIR.Rel("mul_o", List(QueryIR.Atom(4), QueryIR.Atom(3), QueryIR.Ref("X")))
        )
      case Left(_) => false
    }
  }

  property("parse prolog arithmetic operator / into div_o") = {
    val source =
      """
        |?- X = 10 / 2.
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.goals == List(
          QueryIR.Rel("div_o", List(QueryIR.Atom(10), QueryIR.Atom(2), QueryIR.Ref("X")))
        )
      case Left(_) => false
    }
  }

  property("parse prolog ordering operators into comparison relations") = {
    val ltSource =
      """
        |?- 2 < 3.
        |""".stripMargin
    val gtSource =
      """
        |?- 7 > 5.
        |""".stripMargin
    val leSource =
      """
        |?- 4 =< 4.
        |""".stripMargin
    val geSource =
      """
        |?- 9 >= 8.
        |""".stripMargin

    val ltOk = MiniKanrenLangParser.parse(ltSource) match {
      case Right(program) =>
        program.goals == List(QueryIR.Rel("lt_o", List(QueryIR.Atom(2), QueryIR.Atom(3))))
      case Left(_) => false
    }

    val gtOk = MiniKanrenLangParser.parse(gtSource) match {
      case Right(program) =>
        program.goals == List(QueryIR.Rel("gt_o", List(QueryIR.Atom(7), QueryIR.Atom(5))))
      case Left(_) => false
    }

    val leOk = MiniKanrenLangParser.parse(leSource) match {
      case Right(program) =>
        program.goals == List(QueryIR.Rel("le_o", List(QueryIR.Atom(4), QueryIR.Atom(4))))
      case Left(_) => false
    }

    val geOk = MiniKanrenLangParser.parse(geSource) match {
      case Right(program) =>
        program.goals == List(QueryIR.Rel("ge_o", List(QueryIR.Atom(9), QueryIR.Atom(8))))
      case Left(_) => false
    }

    ltOk && gtOk && leOk && geOk
  }

  property("parse prolog numeric =:= operator into eq_num_o") = {
    val source =
      """
        |?- 5 =:= 5.
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.goals == List(QueryIR.Rel("eq_num_o", List(QueryIR.Atom(5), QueryIR.Atom(5))))
      case Left(_) => false
    }
  }

  property("run prolog arithmetic operator / returns quotient") = {
    val source =
      """
        |?- X = 10 / 2.
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(5))
  }

  property("run prolog ordering relation with numeric values") = {
    val source =
      """
        |?- lt_o(4, 5), eq(X, 1).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(1))
  }

  property("run prolog arithmetic operator + returns number") = {
    val source =
      """
        |?- X = 2 + 3.
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(5))
  }

  property("run prolog numeric =:= operator") = {
    val source =
      """
        |?- X =:= 5.
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(5))
  }

  property("parse Cypher DSL with WHERE scientific numeric literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b <> 1e3 RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Neq(QueryIR.Ref("b"), QueryIR.Atom(1000.0))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE negative scientific numeric literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b = -2.5e-2 RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Eq(QueryIR.Ref("b"), QueryIR.Atom(-0.025))
          )
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
      case Left(err) => err.line > 0 && err.column > 0 && err.message.nonEmpty
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

  property("parse Cypher DSL with decimal node property map value") = {
    val source =
      """
        |MATCH (a {score: 42.5})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("score"), QueryIR.Atom(42.5)))
          )
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

  property("parse and run Flix Fixpoints DSL") = {
    val source =
      """
        |parent(alice, bob).
        |parent(bob, carol).
        |ancestor(X, Y) :- parent(X, Y).
        |query ancestor(alice, Y).
        |""".stripMargin
    MiniKanrenLang.run(source) match {
      case Right(results) => results == List("bob")
      case Left(_) => false
    }
  }

  property("parse and run Flix Fixpoints DSL with numeric fact terms") = {
    val source =
      """
        |score(alice, 42).
        |query score(alice, X).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(42))
  }

  property("parse and run recursive Flix Fixpoints DSL") = {
    val source =
      """
        |parent(alice, bob).
        |parent(bob, carol).
        |ancestor(X, Y) :- parent(X, Y).
        |ancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).
        |query ancestor(alice, Y).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("bob", "carol"))
  }

  property("parse Cypher DSL") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) RETURN a
        |""".stripMargin
    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "a" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with multiple RETURN variables") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) RETURN a, b
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "_cy_return" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Eq(QueryIR.Ref("_cy_return"), QueryIR.ListExpr(List(QueryIR.Ref("a"), QueryIR.Ref("b"))))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = a RETURN b
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "b" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Ref("a"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with multiple MATCH patterns") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b), (b)-[:neq]->(c) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Neq(QueryIR.Ref("b"), QueryIR.Ref("c"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with reverse edge direction") = {
    val source =
      """
        |MATCH (a)<-[:parent]-(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("b"), QueryIR.Ref("a")))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with undirected edge") = {
    val source =
      """
        |MATCH (a)-[:parent]-(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Disj(List(
              QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
              QueryIR.Rel("parent", List(QueryIR.Ref("b"), QueryIR.Ref("a")))
            ))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with relationship variable") = {
    val source =
      """
        |MATCH (a)-[r:parent]->(b) RETURN r
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "r" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Eq(QueryIR.Ref("r"), QueryIR.Atom("parent"))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with relationship variable and reverse edge") = {
    val source =
      """
        |MATCH (a)<-[r:parent]-(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("b"), QueryIR.Ref("a"))),
            QueryIR.Eq(QueryIR.Ref("r"), QueryIR.Atom("parent"))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with relationship variable and undirected edge") = {
    val source =
      """
        |MATCH (a)-[r:parent]-(b) RETURN r
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "r" &&
          program.goals == List(
            QueryIR.Disj(List(
              QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
              QueryIR.Rel("parent", List(QueryIR.Ref("b"), QueryIR.Ref("a")))
            )),
            QueryIR.Eq(QueryIR.Ref("r"), QueryIR.Atom("parent"))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE AND predicates") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = a AND b = b RETURN b
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "b" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Ref("a")),
          QueryIR.Eq(QueryIR.Ref("b"), QueryIR.Ref("b"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE OR predicate") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = a OR b = b RETURN b
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "b" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Disj(List(
              QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Ref("a")),
              QueryIR.Eq(QueryIR.Ref("b"), QueryIR.Ref("b"))
            ))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with parenthesized WHERE groups") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE (a = a OR b = b) AND a <> b RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Disj(List(
              QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Ref("a")),
              QueryIR.Eq(QueryIR.Ref("b"), QueryIR.Ref("b"))
            )),
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b"))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with <> predicate") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a <> b RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with node labels") = {
    val source =
      """
        |MATCH (a:Person)-[:neq]->(b:Person) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("label_o", List(QueryIR.Ref("a"), QueryIR.Atom("Person"))),
          QueryIR.Rel("label_o", List(QueryIR.Ref("b"), QueryIR.Atom("Person")))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with node property maps") = {
    val source =
      """
        |MATCH (a:Person {name: "alice", age: 42})-[:parent]->(b {name: "bob"}) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel("label_o", List(QueryIR.Ref("a"), QueryIR.Atom("Person"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("name"), QueryIR.Atom("alice"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("age"), QueryIR.Atom(42))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("name"), QueryIR.Atom("bob")))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with boolean node property map value") = {
    val source =
      """
        |MATCH (a:Person {active: true})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel("label_o", List(QueryIR.Ref("a"), QueryIR.Atom("Person"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("active"), QueryIR.Atom(true)))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with list node property map value") = {
    val source =
      """
        |MATCH (a {tags: ["scala", "kanren"], scores: [1, 2, 3]})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("tags"), QueryIR.ListExpr(List(QueryIR.Atom("scala"), QueryIR.Atom("kanren"))))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("scores"), QueryIR.ListExpr(List(QueryIR.Atom(1), QueryIR.Atom(2), QueryIR.Atom(3)))))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with nested map node property value") = {
    val source =
      """
        |MATCH (a {meta: {rank: 1, active: true}})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel(
              "prop_o",
              List(
                QueryIR.Ref("a"),
                QueryIR.Atom("meta"),
                QueryIR.Atom(Map("rank" -> 1, "active" -> true))
              )
            )
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with list containing nested maps") = {
    val source =
      """
        |MATCH (a {history: [{year: 2020}, {year: 2021}]})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel(
              "prop_o",
              List(
                QueryIR.Ref("a"),
                QueryIR.Atom("history"),
                QueryIR.ListExpr(List(
                  QueryIR.Atom(Map("year" -> 2020)),
                  QueryIR.Atom(Map("year" -> 2021))
                ))
              )
            )
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with null node property map value") = {
    val source =
      """
        |MATCH (a {middleName: null})-[:parent]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Rel("parent", List(QueryIR.Ref("a"), QueryIR.Ref("b"))),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("middleName"), QueryIR.Atom(null)))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with explicit header") = {
    val source =
      """
        |#!cypher
        |MATCH (a)-[:neq]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "a" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("explicit cypher header does not fall back to other parsers") = {
    val source =
      """
        |#!cypher
        |run 1 x {
        |  eq x 7
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("parse Flix DSL with explicit header") = {
    val source =
      """
        |#!flix
        |parent(alice, bob).
        |query parent(alice, X).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "X" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse legacy DSL with explicit header") = {
    val source =
      """
        |#!legacy
        |run 1 x {
        |  eq x 7
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "x" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("parse declarative DSL with explicit header") = {
    val source =
      """
        |#!declarative
        |const Anakin, Luke, Leia
        |var x, y, z
        |rel/2 infix fatherOf
        |Anakin fatherOf Luke
        |Anakin fatherOf Leia
        |sibling(x, y) = fatherOf(z, x) & fatherOf(z, y) & not x = y
        |ask x sibling(Luke, x)
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "x" && program.declarations.nonEmpty
      case Left(_) => false
    }
  }

  property("parse prolog DSL with explicit header") = {
    val source =
      """
        |#!prolog
        |father_of(anakin, luke).
        |father_of(anakin, leia).
        |?- father_of(anakin, Y).
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "Y" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("explicit prolog header does not fall back to legacy parser") = {
    val source =
      """
        |#!prolog
        |run 1 x {
        |  eq x 7
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("parse Cypher DSL with WHERE string literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = "alice" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Atom("alice"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE numeric literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b <> 42 RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Neq(QueryIR.Ref("b"), QueryIR.Atom(42))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE boolean literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = true RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Atom(true))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE property and boolean literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b.active <> false RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("active"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Neq(QueryIR.Ref("_cy_prop_0"), QueryIR.Atom(false))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE null literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = null RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Eq(QueryIR.Ref("a"), QueryIR.Atom(null))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with WHERE property and null literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b.middleName <> null RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(
            QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
            QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("middleName"), QueryIR.Ref("_cy_prop_0"))),
            QueryIR.Neq(QueryIR.Ref("_cy_prop_0"), QueryIR.Atom(null))
          )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with trailing semicolon") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) RETURN a;
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
          program.goals == List(QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")))
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with property access in WHERE") = {
    val source =
      """
        |MATCH (a:Person)-[:neq]->(b:Person) WHERE a.name = "alice" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("label_o", List(QueryIR.Ref("a"), QueryIR.Atom("Person"))),
          QueryIR.Rel("label_o", List(QueryIR.Ref("b"), QueryIR.Atom("Person"))),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Eq(QueryIR.Ref("_cy_prop_0"), QueryIR.Atom("alice"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with property access and <> in WHERE") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE b.age <> 42 RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("age"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Neq(QueryIR.Ref("_cy_prop_0"), QueryIR.Atom(42))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with property to property equality in WHERE") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a.name = b.name RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_1"))),
          QueryIR.Eq(QueryIR.Ref("_cy_prop_0"), QueryIR.Ref("_cy_prop_1"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL with property to property disequality in WHERE") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a.age <> b.age RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("age"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("age"), QueryIR.Ref("_cy_prop_1"))),
          QueryIR.Neq(QueryIR.Ref("_cy_prop_0"), QueryIR.Ref("_cy_prop_1"))
        )
      case Left(_) => false
    }
  }

  property("Cypher WHERE temp vars are deterministic across AND predicates") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a.name = b.name AND b.age <> 42 RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_1"))),
          QueryIR.Eq(QueryIR.Ref("_cy_prop_0"), QueryIR.Ref("_cy_prop_1")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("age"), QueryIR.Ref("_cy_prop_2"))),
          QueryIR.Neq(QueryIR.Ref("_cy_prop_2"), QueryIR.Atom(42))
        )
      case Left(_) => false
    }
  }

  property("Cypher WHERE temp vars are deterministic for property to property then literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a.height <> b.height AND a.name = "alice" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) =>
        program.focus == "a" &&
        program.goals == List(
          QueryIR.Neq(QueryIR.Ref("a"), QueryIR.Ref("b")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("height"), QueryIR.Ref("_cy_prop_0"))),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("b"), QueryIR.Atom("height"), QueryIR.Ref("_cy_prop_1"))),
          QueryIR.Neq(QueryIR.Ref("_cy_prop_0"), QueryIR.Ref("_cy_prop_1")),
          QueryIR.Rel("prop_o", List(QueryIR.Ref("a"), QueryIR.Atom("name"), QueryIR.Ref("_cy_prop_2"))),
          QueryIR.Eq(QueryIR.Ref("_cy_prop_2"), QueryIR.Atom("alice"))
        )
      case Left(_) => false
    }
  }

  property("parse Cypher DSL rejects unsupported CREATE clause") = {
    val source =
      """
        |#!cypher
        |CREATE (a)
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("parse Cypher DSL rejects malformed property access") = {
    val source =
      """
        |#!cypher
        |MATCH (a)-[:neq]->(b) WHERE a. = "alice" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("explicit header is detected after leading whitespace") = {
    val source =
      """

        |
        |   #!cypher
        |MATCH (a)-[:neq]->(b) RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "a" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("explicit prolog header with leading whitespace remains strict") = {
    val source =
      """

        |   #!prolog
        |run 1 x {
        |  eq x 7
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("explicit declarative header with leading whitespace remains strict") = {
    val source =
      """

        |	#!declarative
        |?- father_of(anakin, Y).
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("legacy DSL supports line and block comments") = {
    val source =
      """
        |run 1 x {
        |  // pick the value
        |  eq /* inline block */ x 7
        |}
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(7))
  }

  property("declarative DSL supports hash comments") = {
    val source =
      """
        |#!declarative
        |# simple declarative query
        |var x
        |ask x x = 7
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List(7))
  }

  property("prolog DSL supports percent comments") = {
    val source =
      """
        |#!prolog
        |% basic fact with query
        |father_of(anakin, luke).
        |?- father_of(anakin, Y).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("luke"))
  }

  property("flix DSL supports slash comments") = {
    val source =
      """
        |#!flix
        |// one fact
        |parent(alice, bob).
        |query parent(alice, X).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("bob"))
  }

  property("cypher DSL supports line and block comments") = {
    val source =
      """
        |#!cypher
        |// cypher query
        |MATCH (a)-[:neq]->(b) /* return one side */ RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source) match {
      case Right(program) => program.focus == "a" && program.goals.nonEmpty
      case Left(_) => false
    }
  }

  property("cypher DSL rejects unterminated block comments") = {
    val source =
      """
        |#!cypher
        |MATCH (a)-[:neq]->(b) /* missing end RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }




  property("legacy DSL rejects unterminated block comments") = {
    val source =
      """
        |run 1 x {
        |  eq x 7 /* missing end
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("prolog DSL executes user-defined rule with conjunction body (grandparent)") = {
    val source =
      """
        |#!prolog
        |parent(pam, bob).
        |parent(tom, bob).
        |parent(bob, ann).
        |
        |grandparent(G, C) :- parent(G, P), parent(P, C).
        |?- grandparent(pam, X).
        |""".stripMargin

    MiniKanrenLang.run(source) == Right(List("ann"))
  }

  property("parse escaped string literal") = {
    val source =
      """
        |run 1 x {
        |  eq x "line1\nline2"
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("parse escaped char literal") = {
    val source =
      """
        |run 1 x {
        |  eq x '\n'
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("parse unicode escaped string literal") = {
    val source =
      """
        |run 1 x {
        |  eq x "smile: \u263A"
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("parse unicode escaped char literal") = {
    val source =
      """
        |run 1 x {
        |  eq x '\u263A'
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("reject malformed unicode escaped string literal") = {
    val source =
      """
        |run 1 x {
        |  eq x "bad: \u12G4"
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("reject short unicode escaped char literal") = {
    val source =
      """
        |run 1 x {
        |  eq x '\u123'
        |}
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }

  property("parse Cypher DSL with escaped WHERE string literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = "ali\"ce" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("parse Cypher DSL with unicode escaped WHERE string literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = "city-\u0042" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isRight
  }

  property("reject Cypher DSL with malformed unicode escaped WHERE string literal") = {
    val source =
      """
        |MATCH (a)-[:neq]->(b) WHERE a = "city-\u12X4" RETURN a
        |""".stripMargin

    MiniKanrenLangParser.parse(source).isLeft
  }
}
