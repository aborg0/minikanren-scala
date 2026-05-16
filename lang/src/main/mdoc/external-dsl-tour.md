# External DSL Tour

The external DSL is a beginner-friendly language that compiles into the existing miniKanren engine.

```scala mdoc:silent
import info.hircus.kanren.lang.MiniKanrenLang
import info.hircus.kanren.lang.MiniKanrenLangParser
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

## Comments in Source

All DSL modes support comments through shared whitespace parsing:

- `# ...` hash line comments
- `// ...` slash line comments
- `/* ... */` block comments
- `% ...` line comments (handy for Prolog-style examples)

```scala mdoc
val commentedSource =
  """
    |#!cypher
    |// Find one endpoint
    |MATCH (a)-[:neq]->(b) /* keep only one */ RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(commentedSource)
```

## Declarative Infix Syntax (Experimental)

The original `run { ... }` syntax remains fully supported.
You can also use a declaration-oriented syntax with constants, variables, infix relations, rules, and `ask`.
You can force this parser mode with `#!declarative` (see `Explicit DSL Mode Header` below).

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
You can force this parser mode with `#!prolog` (see `Explicit DSL Mode Header` below).

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

## Flix Fixpoints DSL (Experimental)

You can force this parser mode with `#!flix` (see `Explicit DSL Mode Header` below).

```scala mdoc
val flixSource =
  """
    |parent(alice, bob).
    |parent(bob, carol).
    |ancestor(X, Y) :- parent(X, Y).
    |ancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).
    |query ancestor(alice, Y).
    |""".stripMargin

MiniKanrenLangParser.parse(flixSource)
```

```scala mdoc:crash
val recursiveFlixRunSource =
  """
    |parent(alice, bob).
    |parent(bob, carol).
    |ancestor(X, Y) :- parent(X, Y).
    |ancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).
    |query ancestor(alice, Y).
    |""".stripMargin

MiniKanrenLang.run(recursiveFlixRunSource)
```

```scala mdoc
val recursiveFlixParseSource =
  """
    |parent(alice, bob).
    |parent(bob, carol).
    |ancestor(X, Y) :- parent(X, Y).
    |ancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).
    |query ancestor(alice, Y).
    |""".stripMargin

MiniKanrenLangParser.parse(recursiveFlixParseSource)
```

## Cypher DSL (Experimental)

You can force this parser mode with `#!cypher` (see `Explicit DSL Mode Header` below).

```scala mdoc
val cypherSource =
  """
    |MATCH (a)-[:neq]->(b) RETURN a
    |""".stripMargin

MiniKanrenLang.run(cypherSource)
```

```scala mdoc
val cypherReturnTupleSource =
  """
    |MATCH (a)-[:neq]->(b) RETURN a, b
    |""".stripMargin

MiniKanrenLangParser.parse(cypherReturnTupleSource)
```

```scala mdoc
val cypherMultiMatchSource =
  """
    |MATCH (a)-[:neq]->(b), (b)-[:neq]->(c) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherMultiMatchSource)
```

```scala mdoc
val cypherReverseEdgeSource =
  """
    |MATCH (a)<-[:parent]-(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherReverseEdgeSource)
```

```scala mdoc
val cypherUndirectedEdgeSource =
  """
    |MATCH (a)-[:parent]-(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherUndirectedEdgeSource)
```

```scala mdoc
val cypherRelVarSource =
  """
    |MATCH (a)-[r:parent]->(b) RETURN r
    |""".stripMargin

MiniKanrenLangParser.parse(cypherRelVarSource)
```

```scala mdoc
val cypherRelVarUndirectedSource =
  """
    |MATCH (a)-[r:parent]-(b) RETURN r
    |""".stripMargin

MiniKanrenLangParser.parse(cypherRelVarUndirectedSource)
```

```scala mdoc
val cypherWhereAndSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a = a AND b = b RETURN b
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWhereAndSource)
```

```scala mdoc
val cypherWhereOrSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a = a OR b = b RETURN b
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWhereOrSource)
```

```scala mdoc
val cypherWhereGroupedSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE (a = a OR b = b) AND a <> b RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWhereGroupedSource)
```

```scala mdoc
val cypherNotEqualSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a <> b RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNotEqualSource)
```

```scala mdoc
val cypherLabelSource =
  """
    |MATCH (a:Person)-[:neq]->(b:Person) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherLabelSource)
```

```scala mdoc
val cypherNodePropsSource =
  """
    |MATCH (a:Person {name: "alice", age: 42})-[:parent]->(b {name: "bob"}) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNodePropsSource)
```

```scala mdoc
val cypherNodePropsBooleanSource =
  """
    |MATCH (a:Person {active: true})-[:parent]->(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNodePropsBooleanSource)
```

```scala mdoc
val cypherNodePropsListSource =
  """
    |MATCH (a {tags: ["scala", "kanren"], scores: [1, 2, 3]})-[:parent]->(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNodePropsListSource)
```

```scala mdoc
val cypherNodePropsNestedMapSource =
  """
    |MATCH (a {meta: {rank: 1, active: true}})-[:parent]->(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNodePropsNestedMapSource)
```

```scala mdoc
val cypherNodePropsNestedMapInListSource =
  """
    |MATCH (a {history: [{year: 2020}, {year: 2021}]})-[:parent]->(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherNodePropsNestedMapInListSource)
```

```scala mdoc
val cypherWhereStringLiteralSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a = "alice" RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWhereStringLiteralSource)
```

```scala mdoc
val cypherWhereNumericLiteralSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE b <> 42 RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWhereNumericLiteralSource)
```

```scala mdoc
val cypherWherePropertySource =
  """
    |MATCH (a:Person)-[:neq]->(b:Person) WHERE a.name = "alice" RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWherePropertySource)
```

```scala mdoc
val cypherWherePropertyNeqSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE b.age <> 42 RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWherePropertyNeqSource)
```

```scala mdoc
val cypherWherePropertyToPropertyEqSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a.name = b.name RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWherePropertyToPropertyEqSource)
```

```scala mdoc
val cypherWherePropertyToPropertyNeqSource =
  """
    |MATCH (a)-[:neq]->(b) WHERE a.age <> b.age RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(cypherWherePropertyToPropertyNeqSource)
```

### Cypher Supported Subset

Supported now:

- `MATCH` with one or more patterns separated by commas
- Directed edges: `(a)-[:rel]->(b)` and `(a)<-[:rel]-(b)`
- Undirected edges: `(a)-[:rel]-(b)`
- Relationship variables: `[r:REL]`
- Node labels: `(a:Person)`
- Node property maps: `(a {name: "alice", age: 42, active: true, tags: ["scala", "kanren"], meta: {rank: 1}})`
- `WHERE` with `=` and `<>`
- `WHERE` predicates joined by `AND` and `OR`
- Parenthesized grouping in `WHERE`
- Operand forms in `WHERE`: variable, string literal, numeric literal, property access (`a.name`)
- `RETURN` with one variable or multiple variables (`RETURN a, b`)
- Explicit parser mode header: `#!cypher`

Current lowering rules:

- Node labels are lowered via `label_o(node, "Label")`
- Node property maps are lowered via `prop_o(node, "field", value)`
- Property access is lowered via `prop_o(node, "field", temp)` plus comparison on `temp`
- `RETURN a, b` is lowered by binding a synthetic focus variable to a list expression
- Relationship variables are lowered by binding `r = "REL"`

Not supported yet:

- `CREATE`, `DELETE`, `SET`, `MERGE`
- Path expressions

## Explicit DSL Mode Header

Use a mode header at the top of the source to force parser mode.

Headers are recognized even if the source starts with blank lines or leading whitespace before the `#!...` line.
When a header is present, parsing is strict for that mode and does not fall back to other DSL parsers.

Supported headers:

- `#!legacy` (or `#!run`) for the original `run { ... }` syntax
- `#!declarative` for the declaration-oriented DSL
- `#!prolog` for Prolog-like syntax
- `#!flix` for Flix-style facts/rules/query
- `#!cypher` for Cypher-like graph query syntax

```scala mdoc
val forcedCypherSource =
  """
    |#!cypher
    |MATCH (a)-[:neq]->(b) RETURN a
    |""".stripMargin

MiniKanrenLangParser.parse(forcedCypherSource)
```

```scala mdoc
val forcedFlixSource =
  """
    |#!flix
    |parent(alice, bob).
    |query parent(alice, X).
    |""".stripMargin

MiniKanrenLangParser.parse(forcedFlixSource)
```

```scala mdoc
val forcedLegacySource =
  """
    |#!legacy
    |run 1 x {
    |  eq x 7
    |}
    |""".stripMargin

MiniKanrenLangParser.parse(forcedLegacySource)
```

```scala mdoc
val forcedDeclarativeSource =
  """
    |#!declarative
    |const Anakin, Luke, Leia
    |var x, y, z
    |rel/2 infix fatherOf
    |Anakin fatherOf Luke
    |Anakin fatherOf Leia
    |sibling(x, y) = fatherOf(z, x) & fatherOf(z, y) & not x = y
    |ask x sibling(Luke, x)
    |""".stripMargin

MiniKanrenLangParser.parse(forcedDeclarativeSource)
```

```scala mdoc
val forcedPrologSource =
  """
    |#!prolog
    |father_of(anakin, luke).
    |father_of(anakin, leia).
    |?- father_of(anakin, Y).
    |""".stripMargin

MiniKanrenLangParser.parse(forcedPrologSource)
```

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
