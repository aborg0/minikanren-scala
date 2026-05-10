# External DSL Tour

The external DSL is a beginner-friendly language that compiles into the existing miniKanren engine.

```scala mdoc:silent
import info.hircus.kanren.lang.MiniKanrenLang
```

## Basic equality

```scala mdoc
val eqSource =
  """
    |run 1 x {
    |  eq x 7
    |}
    |""".stripMargin

MiniKanrenLang.run(eqSource)
```

## Disjunction

```scala mdoc
val anySource =
  """
    |run -1 x {
    |  any {
    |    eq x 1;
    |    eq x 2
    |  }
    |}
    |""".stripMargin

MiniKanrenLang.run(anySource)
```

## Relation calls

The external DSL supports relation calls such as `member_o(x, [1, 2, 3])`.

```scala mdoc
val memberSource =
  """
    |run -1 x {
    |  member_o(x, [1, 2, 3])
    |}
    |""".stripMargin

MiniKanrenLang.run(memberSource)
```

## Parse and validation errors

```scala mdoc
val brokenSource =
  """
    |run 1 x {
    |  eq x
    |}
    |""".stripMargin

MiniKanrenLang.run(brokenSource)
```

A successful run returns `Right(List(...))`, while parser failures return `Left(ParseError(...))`.
