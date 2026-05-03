# LazyList vs fs2 vs fairstream benchmark (JVM)

This repository now includes a JVM-only benchmark entrypoint to compare:

- existing LazyList-based goal evaluation
- an fs2 + cats-effect prototype (`GoalIO`)
- a fairstream (`FairT[IO, *]`) prototype

The benchmark currently uses the same relation (`member`) implemented in both runtimes over the same fixed list workload, and verifies result parity before printing timing stats.

It also includes a second workload focused on interleaving/branching behavior (`cond_i` + `always_o`) to better reflect miniKanren's fairness-oriented combinators.

## Run

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark"
```

Override defaults with `runMain` arguments:

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark listSize=1000 takeN=1000 warmups=1 runs=3"
```

Run only one workload:

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=member"
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=branching takeN=1000 warmups=1 runs=3"
```

## Current defaults

- `listSize = 2000`
- `takeN = 2000`
- `warmups = 1`
- `runs = 5`
- `workload = both` (`member`, `branching`, or `both`)

## Example output (first local run)

- LazyList avg: `43372.80 ms`
- fs2 IO avg: `19540.40 ms`
- Speed ratio (`fs2/lazy`): `0.451`

These numbers are environment-dependent and should be treated as directional.

## Quick dual-workload sample

Command:

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=both listSize=200 takeN=200 warmups=0 runs=2"
```

Observed averages:

- `member` workload: LazyList `127.50 ms`, fs2 `205.00 ms`, ratio `1.608`
- `branching` workload: LazyList `37.50 ms`, fs2 `271.50 ms`, ratio `7.240`

Interpretation: on this small quick-run sample, fs2 is slower on both workloads (and much slower on the branching/interleaving case). Run larger trials before drawing final conclusions.

## Stable trial matrix (warmups=1, runs=5)

Commands used:

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=member listSize=1000 takeN=1000 warmups=1 runs=5"
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=member listSize=2000 takeN=2000 warmups=1 runs=5"
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=branching takeN=1000 warmups=1 runs=5"
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=branching takeN=2000 warmups=1 runs=5"
```

Observed averages (lower is better):

| Workload | Parameters | LazyList avg (ms) | fs2 avg (ms) | fairstream avg (ms) | Ratio fs2/lazy | Ratio fair/lazy |
|---|---|---:|---:|---:|---:|---:|
| member | listSize=1000, takeN=1000 | 1160.80 | 829.20 | 828.20 | 0.714 | 0.713 |
| member | listSize=2000, takeN=2000 | 4679.00 | 3410.20 | 2742.00 | 0.729 | 0.586 |
| branching | takeN=1000 | 23.60 | 91.00 | 25.60 | 3.856 | 1.085 |
| branching | takeN=2000 | 37.40 | 160.00 | 54.40 | 4.278 | 1.455 |

Boundary observation:

- `member` with `listSize=3000 takeN=3000` failed in this environment with a deep recursive lookup stack trace rooted at `Substitution$SimpleSubst.lookup`.

Recommendation from current data:

1. Do not replace `LazyList` globally with fs2.
2. `fairstream` currently looks like the strongest replacement candidate for the `member`-style scans (wins at both measured sizes, especially at 2000).
3. For branching/interleaving-heavy workloads, `fairstream` is much better than fs2 but still slower than `LazyList` in the stable matrix, so migration should stay workload-driven.
4. Next step before any full migration: optimize substitution lookup depth (`Substitution$SimpleSubst.lookup`) and re-run the same matrix.

## Three-runtime run example

```bash
sbt "miniKanrenExamplesJVM/runMain info.hircus.kanren.examples.Fs2MiniKanrenBenchmark workload=both listSize=500 takeN=500 warmups=1 runs=3"
```

First local three-runtime sample (quick-run config: `workload=both listSize=200 takeN=200 warmups=0 runs=2`):

- `member` workload:
	- LazyList avg: `142.00 ms`
	- fs2 avg: `180.50 ms` (ratio `1.271`)
	- fairstream avg: `55.00 ms` (ratio `0.387`)
- `branching` workload:
	- LazyList avg: `20.00 ms`
	- fs2 avg: `93.00 ms` (ratio `4.650`)
	- fairstream avg: `11.00 ms` (ratio `0.550`)
