package info.hircus.kanren.dsl3

import org.scalacheck.Properties

object Scala3DSLSpecification extends Properties("Scala3DSL") {

  import Scala3DSL.*

  property("simple equality") = {
    val x = v("x")
    run(x, limit = 1)(x === 5).toList == List(5)
  }

  property("disjunction") = {
    val x = v("x")
    val result = run(x, limit = -1)(any(x === 1, x === 2)).toList
    result == List(1, 2)
  }

  property("list literal") = {
    val x = v("x")
    val result = run(x, limit = 1)(x === list(1, 2, 3)).toList
    result == List(List(1, 2, 3))
  }

  property("compatibility runList") = {
    val x = v("x")
    runList(x, limit = 1)(x === 5) == List(5)
  }

  property("query builder supports limit and constraints") = {
    val x = v("x")
    val constrainedProgram = query(x)
      .withLimit(5)
      .withConstraints()
      .where(x =/= 1, x === 1)

    constrainedProgram.limit == 5 && constrainedProgram.constrained && run(constrainedProgram).isEmpty
  }

  property("expr and logic variable overloads compile") = {
    val x = v("x")
    val y = v("y")
    val result = run(x, limit = 1)(all(x === y, y === atom(9))).toList
    result == List(9)
  }

  property("pair and list normalize into idiomatic lists") = {
    val x = v("x")
    val normalized = run(x, limit = 1)(x === pair(1, list(2, 3))).toList
    normalized == List(List(1, 2, 3))
  }

  property("compat aliases map to builtins") = {
    val x = v("x")
    val y = v("y")
    val z = v("z")

    val result = run(x, limit = 1)(
      all(
        null_o(y),
        pair_o(z),
        car_o(z, x),
        cdr_o(z, y),
        x === 4,
        member_o(4, list(4, 5)),
        append_o(list(1), list(2), list(1, 2))
      )
    ).toList

    result == List(4)
  }

  property("string and list ops can be called through rel") = {
    val len = v("len")
    val reversed = v("reversed")

    val result = run(len, limit = 1)(
      all(
        rel("atom_length", atom("hello"), len),
        rel("reverse_o", list(1, 2, 3), reversed),
        reversed === list(3, 2, 1)
      )
    ).toList

    result == List(5)
  }
}
