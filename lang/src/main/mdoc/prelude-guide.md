# Prelude and Built-in Guide

This guide summarizes built-in relations, convenience imports, and Scala 3 extensions.

## One-Stop Import

Use `AllOps` to import built-ins from Prelude, MKMath, StringOps, ListOps, and core MiniKanren helpers.

```scala
import info.hircus.kanren.AllOps._

val x = make_var(Symbol("x"))
val result = run(-1, x)(mod_o(10, 3, x))
```

## Numeric and Conversion Built-ins

These are available from the external DSL and Scala API:

- `mod_o/3`
- `atom_number/2`
- `number_codes/2`
- `number_chars/2`

Examples:

```scala
val x = make_var(Symbol("x"))
run(-1, x)(atom_number("42", x))
run(-1, x)(number_codes(42, x))
run(-1, x)(number_chars(42, x))
run(-1, x)(mod_o(10, 3, x))
```

## Scala 3 Extension Methods

### StringExtensions

```scala
import info.hircus.kanren.StringExtensions._

val x = make_var(Symbol("x"))
run(-1, x)("42".toNumber(x))
run(-1, x)("hi".toCodes(x))
run(-1, x)("HELLO".lower(x))
```

### NumericExtensions

```scala
import info.hircus.kanren.NumericExtensions._

val x = make_var(Symbol("x"))
run(-1, x)(42.toAtom(x))
run(-1, x)(42.toCharList(x))
run(-1, x)(10.mod(3, x))
```

## Prelude Registry

`PreludeRegistry` enables runtime customization of built-in dispatch in `QueryCompiler`.

```scala
import info.hircus.kanren.dslir.PreludeRegistry
import info.hircus.kanren.MiniKanren

PreludeRegistry.register("always_42", { args =>
  MiniKanren.mkEqual(42, args.head)
})

// Cleanup
PreludeRegistry.unregister("always_42")
PreludeRegistry.clear()
```

Behavior notes:

- Registered names can introduce new relations.
- Registered names can override existing built-ins.
- Registry entries are process-local runtime state.
