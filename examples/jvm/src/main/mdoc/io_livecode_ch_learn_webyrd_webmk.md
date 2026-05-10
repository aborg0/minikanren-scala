# miniKanren: an interactive Tutorial

Based on [a short interactive tutorial](http://io.livecode.ch/learn/webyrd/webmk) from [minikanren.org](http://minikanren.org/).

## Core miniKanren

Core miniKanren extends Scheme with three operations:
`==` (here in scala: `unify`/`mkEqual`/`===`), `fresh` (here in scala: `make_var`), and `conde`.
There is also `run`, which serves as an interface between
Scheme and miniKanren, and whose value is a list.

`==` unifies two terms. `fresh`, which
syntactically looks like `lambda`, introduces lexically-scoped
Scheme variables that are bound to new logic variables; `fresh`
also performs conjunction of the relations within its body. Thus
`(fresh (x y z) (== x z) (== 3 y))`

```scala mdoc:silent
import info.hircus.kanren._
import info.hircus.kanren.MiniKanren._
import info.hircus.kanren.MKMath._
val x = make_var(Symbol("x"))
val y = make_var(Symbol("y"))
val z = make_var(Symbol("z"))
all(mkEqual(x, z), mkEqual(3, y))
```

would introduce logic variables `x`, `y`, and `z`,
then associate `x` with `z` and `y`
with `3`.  This, however, is not a legal miniKanren
program---we must wrap a `run` around the entire expression.
`(run 1 (q) (fresh (x y z) (== x z) (== 3 y)))`

   ```scala mdoc
val q = make_var(Symbol("q"))
run(1, q)(all(mkEqual(make_var(Symbol("x")), make_var(Symbol("z"))), mkEqual(build_num(3), make_var(Symbol("y")))))
   ```

The value returned is a list containing the single
value `(_.0)`; we
say that `_.0` is
the *reified value* of the unbound query variable `q` and thus
represents any value. `q` also remains unbound in
`(run 1 (q) (fresh (x y) (== x q) (== 3 y)))`

   ```scala mdoc
run(1, q)(all(mkEqual(make_var(Symbol("x")), q), mkEqual(build_num(3), make_var(Symbol("y")))))
   ```

We can get back more interesting values by unifying the query variable with another term.

    (run 1 (y)
      (fresh (x z)
        (== x z)
        (== 3 y)))

```scala mdoc
val ex3_x = make_var(Symbol("x"))
val ex3_z = make_var(Symbol("z"))
val three = build_num(3)
run(1, y)(all(mkEqual(ex3_x, ex3_z), mkEqual(build_num(3), y)))
```
> Note that numbers are represented in binary form, so `3` (`three`) is `11` (`(1, (1, List()))`, similar to `HList`s, these are encoded as tuples).

    (run 1 (q)
      (fresh (x z)
        (== x z)
        (== 3 z)
        (== q x)))

```scala mdoc
val ex4_x = make_var(Symbol("x"))
val ex4_z = make_var(Symbol("z"))
val ex4_q = make_var(Symbol("q"))
run(1, ex4_q)(all(mkEqual(ex4_x, ex4_z), mkEqual(build_num(3), ex4_z), mkEqual(ex4_q, ex4_x)))
```

    (run 1 (y)
      (fresh (x y)
        (== 4 x)
        (== x y))
      (== 3 y))

```scala mdoc
val ex5_x = make_var(Symbol("x"))
run(1, y)(all(mkEqual(build_num(4), ex5_x), mkEqual(ex5_x, make_var(Symbol("y"))), mkEqual(build_num(3), y)))
```

Each of these examples returns `(3)`; in the
rightmost example, the `y` introduced by `fresh` is
different from the `y` introduced by `run`.

A `run` expression can return the empty list, indicating that
the body of the expression is logically inconsistent.

    (run 1 (x) (== 4 3))

```scala mdoc
run(1, make_var(Symbol("x")))(mkEqual(build_num(4), build_num(3)))
```

    (run 1 (x) (== 5 x) (== 6 x))

```scala mdoc
val ex8_x = make_var(Symbol("x"))
run(1, x)(all(mkEqual(build_num(5), ex8_x), mkEqual(build_num(6), ex8_x)))
```

We say that a logically inconsistent relation *fails*,
while a logically consistent relation, such as `(== 3 3)`, *succeeds*.

`conde` (`cond_e` in Scala), which resembles `cond` syntactically, is used
to produce multiple answers.  Logically, `conde` can be thought
of as disjunctive normal form: each clause represents a disjunct, and
is independent of the other clauses, with the relations within a
clause acting as the conjuncts. For example, this expression produces
two answers.

    (run 2 (q)
      (fresh (w x y)
        (conde
          ((== `(,x ,w ,x) q)
           (== y w))
          ((== `(,w ,x ,w) q)
           (== y w)))))

```scala mdoc
val ex9_q = make_var(Symbol("q"))
val ex9_w = make_var(Symbol("w"))
val ex9_x = make_var(Symbol("x"))
val ex9_y = make_var(Symbol("y"))
run(2, ex9_q)(cond_e(
  (mkEqual(List(ex9_x, ex9_w, ex9_x), ex9_q), mkEqual(ex9_y, ex9_w)),
  (mkEqual(List(ex9_w, ex9_x, ex9_w), ex9_q), mkEqual(ex9_y, ex9_w))
))
```

Although the two `conde` lines are different, the
values returned are identical.  This is because distinct reified
unbound variables are assigned distinct subscripts, increasing from
left to right&mdash;the numbering starts over again from zero within each
answer, which is why the reified value of `x`
is `_.0` in the
first answer
but `_.1` in the
second.  The argument `2` in `run` denotes the
maximum length of the resultant list.  If `run*` (`run(-1, ...)` in Scala)
is used instead, then there is no maximum imposed.  This can easily lead to
infinite loops.
