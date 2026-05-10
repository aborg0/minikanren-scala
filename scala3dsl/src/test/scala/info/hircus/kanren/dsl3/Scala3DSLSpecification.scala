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
}
