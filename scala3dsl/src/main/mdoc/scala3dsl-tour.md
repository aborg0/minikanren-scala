# Scala 3 DSL Tour

This module provides a more idiomatic Scala 3 surface over miniKanren.

```scala mdoc:silent
import info.hircus.kanren.dsl3.Scala3DSL.*
```

## Supported DSL landscape

This repository has three primary DSL surfaces:

- Core Scala API (`MiniKanren`, `Prelude`, `AllOps`) for low-level control
- Scala 3 DSL (this page) for idiomatic Scala syntax
- External DSL (`MiniKanrenLang`) for script-like query text and multiple parser modes

Quick Scala 3 DSL example:

```scala mdoc
val sx = v("sx")
run(sx, limit = -1)(contains(sx, list(1, 2, 3))).toList
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

## Bottles puzzle feasibility (3L and 5L to reach 4L)

Use a list input for bottle capacities and a target amount.
Refactor the check into miniKanren relations:

- `contains(cap, capacities)` picks a valid bottle
- `ge_o(cap, target)` ensures at least one bottle can hold target
- `mod_o(target, gcd(capacities), 0)` checks divisibility

```scala mdoc
def gcdAll(values: List[Int]): Int =
	values.map(math.abs).reduce((a, b) => BigInt(a).gcd(BigInt(b)).toInt)

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

bottleFeasibleWithKanren(List(3, 5), 4)
```

Known sequence of states that reaches 4L (second bottle):

```scala mdoc
List(
	(0, 0),
	(0, 5),
	(3, 2),
	(0, 2),
	(2, 0),
	(2, 5),
	(3, 4)
)
```
Later, this can be extended into full relation-based state transitions (`state`, `step`) and queried declaratively with the DSL itself.
