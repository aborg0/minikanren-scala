A Scala port of [miniKanren](http://minikanren.org/)
====================================================

[![CI](https://github.com/aborg0/minikanren-scala/actions/workflows/ci.yml/badge.svg?branch=cleanup)](https://github.com/aborg0/minikanren-scala/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/aborg0/minikanren-scala/badge.svg?branch=cleanup)](https://coveralls.io/github/aborg0/minikanren-scala?branch=cleanup)

**[📖 View the live documentation and interactive demos »](https://aborg0.github.io/minikanren-scala/)**

Based on https://github.com/michel-slm/minikanren-scala

The website includes browser-based playgrounds where you can:
- Try the external language DSL with live code execution
- Run compiled Scala 3 DSL demos
- Explore the Solve More Money and palindrome examples
- Browse the generated API documentation

Local documentation is also available in [docs/presentation.rst](docs/presentation).

You can also check the (basic) tutorial of miniKanren with this Scala syntax, using the following command (in the root project's sbt shell):

    > miniKanrenExamplesJVM/mdoc

and check the output markdown file in `examples/jvm/target/mdoc`

Using REPL with SBT:

    > miniKanrenExamplesJVM/console

...

    scala> time(run(1, x)(solve_puzzle(x)))
    res0: (Long, Any) = (10044,List(List(9567, 1085, 10652)))

Interpretation of the result: took `10044` milliseconds to solve the problem, a result is

     SEND   9567
    +MORE  +1085
    -----  -----
    MONEY  10652

Another example (palindromes with six-digit numbers that are the product of two three-digit numbers), this time with `maprun` as it is much faster:

    time(maprun(1, x)(palprod_o(x)))
    100001
    101101
    res1: (Long, Any) = (40837,List((1,(1,(1,(0,(0,(1,(1,(1,(1,(1,(0,(0,(0,(1,List()))))))))))))))))

Scala 3 DSL Notes
-----------------

The Scala 3 DSL module (`miniKanrenScala3DSL`) provides a more idiomatic surface over the core API.

- Idiomatic relation names: `isEmptyList`, `isPair`, `head`, `tail`, `contains`, `append`
- Compatibility aliases are still available: `null_o`, `pair_o`, `car_o`, `cdr_o`, `member_o`, `append_o`
- `run` returns a `LazyList[Any]` for streaming consumption
- Proper pair-lists like `(1, (2, (3, Nil)))` are postprocessed to Scala lists (`List(1, 2, 3)`) in DSL output
- `runList` is available if an eager `List` is preferred

Supported DSLs At A Glance
--------------------------

The project currently supports multiple ways to write miniKanren queries.

| DSL | Best for | Example | Notable features |
| --- | --- | --- | --- |
| Core Scala API (`MiniKanren`, `Prelude`, `AllOps`) | Full engine access and low-level control | `run(-1, x)(member_o(x, list2pair(List(1, 2, 3))))` | Most direct mapping to core relations and substitutions |
| Scala 3 DSL (`Scala3DSL`) | Idiomatic Scala 3 query authoring | `run(v("x"), -1)(contains(v("x"), list(1, 2, 3))).toList` | Extension methods (`===`, `=/=`), readable relation aliases, normalized list output |
| External DSL (`MiniKanrenLang`) | User-facing scriptable syntax and parser experimentation | `run -1 x { member_o(x, [1, 2, 3]) }` | Multiple modes: classic `run {}`, declarative infix, Prolog-like, Flix-like, Cypher-like; numeric relations accept plain numbers and return numeric results |

Useful documentation entry points:

- Scala 3 DSL tour: `scala3dsl/src/main/mdoc/scala3dsl-tour.md`
- External DSL tour: `lang/src/main/mdoc/external-dsl-tour.md`
- Built-ins and ops guide: `lang/src/main/mdoc/prelude-guide.md`

Bottles Puzzle Feasibility Example (3L and 5L to reach 4L)
----------------------------------------------------------

Use miniKanren relations directly, with bottle capacities supplied as a list and a target amount.

For capacities `caps` and target `t`, this query checks:

- at least one bottle can hold `t` (`member_o` + `ge_o`)
- `t` is divisible by `gcd(caps)` (`mod_o`)

MiniKanren-style Scala 3 DSL example:

```scala
def gcdAll(values: List[Int]): Int =
    values.map(math.abs).reduce((a, b) => BigInt(a).gcd(BigInt(b)).toInt)

import info.hircus.kanren.dsl3.Scala3DSL._

def bottleFeasibleWithKanren(capacities: List[Int], target: Int): List[Any] = {
    val verdict = v("verdict")
    val cap = v("cap")
    val gcd = gcdAll(capacities)

    runList(verdict, limit = 1)(
        all(
            contains(cap, list(capacities.map(x => x: Any)*)),
            rel("ge_o", cap, target),
            rel("mod_o", target, gcd, 0),
            verdict === "possible"
        )
    )
}

bottleFeasibleWithKanren(List(3, 5), 4) // List("possible")
```

One concrete sequence for `(3, 5) -> 4` is `(0, 5) -> (3, 2) -> (0, 2) -> (2, 0) -> (2, 5) -> (3, 4)`.
