# fs2 vs LazyList benchmark (JVM)

This repository now includes a JVM-only benchmark entrypoint to compare:

- existing LazyList-based goal evaluation
- an fs2 + cats-effect prototype (`GoalIO`)

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

| Workload | Parameters | LazyList avg (ms) | fs2 avg (ms) | Ratio fs2/lazy |
|---|---|---:|---:|---:|
| member | listSize=1000, takeN=1000 | 1042.60 | 795.60 | 0.763 |
| member | listSize=2000, takeN=2000 | 4896.00 | 3090.20 | 0.631 |
| branching | takeN=1000 | 29.20 | 136.40 | 4.671 |
| branching | takeN=2000 | 43.40 | 166.40 | 3.834 |

Boundary observation:

- `member` with `listSize=3000 takeN=3000` failed in this environment with a deep recursive lookup stack trace rooted at `Substitution$SimpleSubst.lookup`.

Recommendation from current data:

1. Do not replace `LazyList` globally with fs2 yet.
2. Keep the benchmark harness and treat fs2 as workload-specific: promising for the linear `member` style scan, but currently much slower on fairness/interleaving-heavy goals.
3. If migration is still interesting, next step should be targeted optimization of the fs2 `mplus_i`/`bind_i` path and substitution lookup depth before re-running the same matrix.
