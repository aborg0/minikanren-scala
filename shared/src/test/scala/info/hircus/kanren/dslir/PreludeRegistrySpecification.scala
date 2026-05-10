package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.dslir.QueryIR._
import org.scalacheck.Properties

object PreludeRegistrySpecification extends Properties("PreludeRegistry") {

  property("register custom relation") = {
    PreludeRegistry.clear()
    try {
      PreludeRegistry.register("always_42", { args =>
        if (args.length != 1) throw new IllegalArgumentException("always_42 expects 1 argument")
        MiniKanren.mkEqual(42, args.head)
      })

      val program = Program(
        focus = "x",
        goals = List(Rel("always_42", List(Ref("x"))))
      )

      QueryCompiler.execute(program) == List(42)
    } finally {
      PreludeRegistry.clear()
    }
  }

  property("custom relation can override builtin") = {
    PreludeRegistry.clear()
    try {
      PreludeRegistry.register("atom_length", { args =>
        if (args.length != 2) throw new IllegalArgumentException("atom_length expects 2 arguments")
        MiniKanren.mkEqual(99, args(1))
      })

      val overridden = Program(
        focus = "x",
        goals = List(Rel("atom_length", List(Atom("hello"), Ref("x"))))
      )

      QueryCompiler.execute(overridden) == List(99)
    } finally {
      PreludeRegistry.clear()
    }
  }

  property("unregister restores builtin behavior") = {
    PreludeRegistry.clear()
    PreludeRegistry.register("atom_length", { args =>
      if (args.length != 2) throw new IllegalArgumentException("atom_length expects 2 arguments")
      MiniKanren.mkEqual(99, args(1))
    })
    PreludeRegistry.unregister("atom_length")

    val program = Program(
      focus = "x",
      goals = List(Rel("atom_length", List(Atom("hello"), Ref("x"))))
    )

    val result = QueryCompiler.execute(program)
    PreludeRegistry.clear()
    result == List(5)
  }
}
