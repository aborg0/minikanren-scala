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

## Declarative Infix Syntax (Experimental)

The original `run { ... }` syntax remains fully supported.
You can also use a declaration-oriented syntax with constants, variables, infix relations, rules, and `ask`.

```scala mdoc
val declarativeSource =
  """
    |const Anakin, Luke, Leia
    |var x, y, z
    |rel/2 infix fatherOf
    |Anakin fatherOf Luke
    |Anakin fatherOf Leia
    |sibling(x, y) = fatherOf(z, x) & fatherOf(z, y) & not x = y
    |ask x sibling(Luke, x)
    |""".stripMargin

MiniKanrenLang.run(declarativeSource)
```

Notes:

- `not x = y` is v1 sugar for disequality.
- `var a in [1, 2, 3]` is accepted as finite-domain syntax.
- List head-tail deconstruction is supported: `[h | t]` and `[a, b | t]`.

## Prolog-like Syntax (Experimental)

You can also write facts/rules with `.` and `:-`, then query with `?-`.

```scala mdoc
val prologSource =
  """
    |father_of(anakin, luke).
    |father_of(anakin, leia).
    |?- father_of(anakin, Y).
    |""".stripMargin

MiniKanrenLang.run(prologSource)
```

Notes:

- Variables are uppercase (for example `X`, `Y`, `Z`).
- Atoms are lowercase (for example `anakin`, `leia`).
- List head-tail deconstruction works in terms, for example `eq([H|T], [1,2,3])`.

## String and Number Built-ins

```scala mdoc
val conversionSource =
  """
    |run 1 x {
    |  atom_number "42" x
    |}
    |""".stripMargin

MiniKanrenLang.run(conversionSource)
```

```scala mdoc
val modSource =
  """
    |run 1 x {
    |  mod_o 10 3 x
    |}
    |""".stripMargin

MiniKanrenLang.run(modSource)
```

Available conversion helpers include:

- `atom_number/2`
- `number_codes/2`
- `number_chars/2`

## Prelude Registry Customization

`QueryCompiler` supports runtime custom relation registration through `PreludeRegistry`.

```scala mdoc
import info.hircus.kanren.dslir.PreludeRegistry
import info.hircus.kanren.MiniKanren

PreludeRegistry.register("always_42", { args =>
  MiniKanren.mkEqual(42, args.head)
})
```

Custom entries can be removed with `PreludeRegistry.unregister(name)` or fully reset with `PreludeRegistry.clear()`.
