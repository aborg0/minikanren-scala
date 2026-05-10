# Scala 3 DSL Tour

This module provides a more idiomatic Scala 3 surface over miniKanren.

```scala mdoc:silent
import info.hircus.kanren.dsl3.Scala3DSL.*
```

## Basic unification

```scala mdoc
val q = v("q")
run(q, limit = 1)(q === 42).toList
```

`run` returns a `LazyList`, so you can stream results and only evaluate what you need.

```scala mdoc
val q2 = v("q2")
run(q2, limit = -1)(any(q2 === 1, q2 === 2, q2 === 3)).take(2).toList
```

## Idiomatic relation names

Instead of low-level Lisp names, use:

- `head` (alias of `car_o`)
- `tail` (alias of `cdr_o`)
- `contains` (alias of `member_o`)
- `append` (alias of `append_o`)
- `isPair` and `isEmptyList`

```scala mdoc
val h = v("h")
run(h, limit = -1)(head(pair(1, list(2, 3)), h)).toList
```

```scala mdoc
val m = v("m")
run(m, limit = -1)(contains(m, list(1, 2, 3))).toList
```

## Result postprocessing

Pair-lists are normalized to Scala lists in DSL output.

```scala mdoc
val p = v("p")
run(p, limit = 1)(p === list(1, 2, 3)).toList
```

## Eager compatibility

If you prefer eager results, use `runList`.

```scala mdoc
val e = v("e")
runList(e, limit = 1)(e === 99)
```
