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
