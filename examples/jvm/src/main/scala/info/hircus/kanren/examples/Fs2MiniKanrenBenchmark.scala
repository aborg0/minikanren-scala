package info.hircus.kanren.examples

import cats.effect.{ExitCode, IO, IOApp}
import fs2.{Pull, Stream}
import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren.{Goal, Subst, Var}
import info.hircus.kanren.Prelude.list2pair

object Fs2MiniKanrenBenchmark extends IOApp {

  private def parseInt(s: String): Option[Int] =
    scala.util.Try(s.toInt).toOption

  private def parseArgs(args: List[String]): Map[String, String] =
    args.flatMap { raw =>
      raw.split("=", 2).toList match {
        case key :: value :: Nil => Some(key -> value)
        case _ => None
      }
    }.toMap

  object MiniKanrenIO {
    type GoalIO = Subst => Stream[IO, Subst]

    def succeed: GoalIO = { s: Subst => Stream.emit(s) }

    def fail: GoalIO = { _: Subst => Stream.empty }

    def bind(aInf: Stream[IO, Subst], g: GoalIO): Stream[IO, Subst] =
      aInf.flatMap(g)

    def mplus(aInf: Stream[IO, Subst], f: => Stream[IO, Subst]): Stream[IO, Subst] =
      aInf ++ f

    def mplus_i(aInf: Stream[IO, Subst], f: => Stream[IO, Subst]): Stream[IO, Subst] =
      aInf.pull.uncons1.flatMap {
        case None =>
          f.pull.echo
        case Some((head, tail)) =>
          tail.pull.uncons1.flatMap {
            case None =>
              Pull.output1(head) >> f.pull.echo
            case Some((head2, tail2)) =>
              Pull.output1(head) >> mplus_i(f, Stream.emit(head2) ++ tail2).pull.echo
          }
      }.stream

    def bind_i(aInf: Stream[IO, Subst], g: GoalIO): Stream[IO, Subst] =
      aInf.pull.uncons1.flatMap {
        case None =>
          Pull.done
        case Some((head, tail)) =>
          tail.pull.uncons1.flatMap {
            case None =>
              g(head).pull.echo
            case Some((head2, tail2)) =>
              mplus_i(g(head), bind(Stream.emit(head2) ++ tail2, g)).pull.echo
          }
      }.stream

    def any_e(g1: GoalIO, g2: GoalIO): GoalIO = { s: Subst =>
      mplus(g1(s), g2(s))
    }

    def any_o(g: => GoalIO): GoalIO = if_e(g, succeed, any_o(g))

    def always_o: GoalIO = any_o(succeed)

    def all_aux(bindfn: (Stream[IO, Subst], GoalIO) => Stream[IO, Subst])(gs: GoalIO*): GoalIO =
      gs.toList match {
        case Nil => succeed
        case g :: Nil => g
        case g :: gs2 =>
          s: Subst => bindfn(g(s), all(gs2: _*))
      }

    def all(gs: GoalIO*): GoalIO = all_aux(bind)(gs: _*)

    def all_i(gs: GoalIO*): GoalIO = all_aux(bind_i)(gs: _*)

    def both(g0: GoalIO, g1: GoalIO): GoalIO = { s: Subst =>
      g0(s).flatMap(g1)
    }

    def if_e(testg: GoalIO, conseqg: => GoalIO, altg: => GoalIO): GoalIO = {
      s: Subst =>
        mplus(both(testg, conseqg)(s), altg(s))
    }

    def if_i(testg: GoalIO, conseqg: => GoalIO, altg: => GoalIO): GoalIO = {
      s: Subst =>
        mplus_i(both(testg, conseqg)(s), altg(s))
    }

    def mkEqual(t1: Any, t2: Any): GoalIO = { s: Subst =>
      s.unify(t1, t2) match {
        case Some(s2) => succeed(s2)
        case None => fail(s)
      }
    }

    def runIO(n: Int, v: Var)(g0: GoalIO, gs: GoalIO*): IO[List[Any]] = {
      val g = gs.toList match {
        case Nil => g0
        case gls => all(g0 :: gls: _*)
      }
      val allres = g(MiniKanren.empty_s).map { s: Subst => MiniKanren.reify(MiniKanren.walk_*(v, s)) }
      val bounded = if (n < 0) allres else allres.take(n.toLong)
      bounded.compile.toList
    }
  }

  private case class BenchResult(name: String, timesMs: List[Long], sample: List[Any])

  private def null_o(x: Any): Goal = MiniKanren.mkEqual(Nil, x)

  private def car_o(p: Any, a: Any): Goal = {
    val d = MiniKanren.make_var(Symbol("d"))
    MiniKanren.mkEqual((a, d), p)
  }

  private def cdr_o(p: Any, d: Any): Goal = {
    val a = MiniKanren.make_var(Symbol("a"))
    MiniKanren.mkEqual((a, d), p)
  }

  private def member_lazy(x: Any, l: Any): Goal =
    MiniKanren.if_e(null_o(l), MiniKanren.fail,
      MiniKanren.if_e(car_o(l, x), MiniKanren.succeed, { s: Subst =>
        val d = MiniKanren.make_var(Symbol("d"))
        MiniKanren.all(cdr_o(l, d), member_lazy(x, d))(s)
      }))

  private def null_o_io(x: Any): MiniKanrenIO.GoalIO = MiniKanrenIO.mkEqual(Nil, x)

  private def car_o_io(p: Any, a: Any): MiniKanrenIO.GoalIO = {
    val d = MiniKanren.make_var(Symbol("d"))
    MiniKanrenIO.mkEqual((a, d), p)
  }

  private def cdr_o_io(p: Any, d: Any): MiniKanrenIO.GoalIO = {
    val a = MiniKanren.make_var(Symbol("a"))
    MiniKanrenIO.mkEqual((a, d), p)
  }

  private def member_io(x: Any, l: Any): MiniKanrenIO.GoalIO =
    MiniKanrenIO.if_e(null_o_io(l), MiniKanrenIO.fail,
      MiniKanrenIO.if_e(car_o_io(l, x), MiniKanrenIO.succeed, { s: Subst =>
        val d = MiniKanren.make_var(Symbol("d"))
        MiniKanrenIO.all(cdr_o_io(l, d), member_io(x, d))(s)
      }))

  private def timed[A](block: IO[A]): IO[(Long, A)] =
    for {
      start <- IO.monotonic
      result <- block
      end <- IO.monotonic
    } yield ((end - start).toMillis, result)

  private def repeat(n: Int)(block: IO[Unit]): IO[Unit] =
    if (n <= 0) IO.unit else block >> repeat(n - 1)(block)

  private def measure(name: String, warmups: Int, runs: Int, task: IO[List[Any]]): IO[BenchResult] = {
    def loop(remaining: Int, acc: List[Long], last: List[Any]): IO[BenchResult] = {
      if (remaining <= 0) IO.pure(BenchResult(name, acc.reverse, last))
      else timed(task).flatMap { case (elapsed, res) =>
        loop(remaining - 1, elapsed :: acc, res)
      }
    }

    repeat(warmups)(task.void) >> loop(runs, Nil, Nil)
  }

  private def average(xs: List[Long]): Double =
    if (xs.isEmpty) 0.0 else xs.sum.toDouble / xs.size.toDouble

  private def p95(xs: List[Long]): Long = {
    if (xs.isEmpty) 0L
    else {
      val sorted = xs.sorted
      val idx = math.max(0, math.min(sorted.length - 1, math.ceil(sorted.length * 0.95).toInt - 1))
      sorted(idx)
    }
  }

  private def printResult(result: BenchResult): IO[Unit] = IO {
    val min = if (result.timesMs.isEmpty) 0L else result.timesMs.min
    val avg = average(result.timesMs)
    val p95v = p95(result.timesMs)
    println(s"${result.name}")
    println(s"  runs (ms): ${result.timesMs.mkString(", ")}")
    println(f"  min/avg/p95 (ms): $min / $avg%.2f / $p95v")
    println(s"  sample size: ${result.sample.size}")
  }

  private def lazyMemberTask(listSize: Int, takeN: Int): IO[List[Any]] = IO {
    val q = MiniKanren.make_var(Symbol("q"))
    val data = list2pair((1 to listSize).toList.map(_.asInstanceOf[Any]))
    MiniKanren.run(takeN, q)(member_lazy(q, data))
  }

  private def ioMemberTask(listSize: Int, takeN: Int): IO[List[Any]] = {
    val q = MiniKanren.make_var(Symbol("q"))
    val data = list2pair((1 to listSize).toList.map(_.asInstanceOf[Any]))
    MiniKanrenIO.runIO(takeN, q)(member_io(q, data))
  }

  private def lazyBranchingTask(takeN: Int): IO[List[Any]] = IO {
    val v = MiniKanren.make_var(Symbol("v"))
    MiniKanren.run(takeN, v)(MiniKanren.both(
      MiniKanren.all_i(
        MiniKanren.if_e(MiniKanren.mkEqual(false, v), MiniKanren.succeed,
          MiniKanren.mkEqual(true, v)),
        info.hircus.kanren.Prelude.always_o
      ),
      MiniKanren.mkEqual(true, v)
    ))
  }

  private def ioBranchingTask(takeN: Int): IO[List[Any]] = {
    val v = MiniKanren.make_var(Symbol("v"))
    MiniKanrenIO.runIO(takeN, v)(MiniKanrenIO.both(
      MiniKanrenIO.all_i(
        MiniKanrenIO.if_e(MiniKanrenIO.mkEqual(false, v), MiniKanrenIO.succeed,
          MiniKanrenIO.mkEqual(true, v)),
        MiniKanrenIO.always_o
      ),
      MiniKanrenIO.mkEqual(true, v)
    ))
  }

  private def compareWorkload(
                               title: String,
                               warmups: Int,
                               runs: Int,
                               lazyTask: IO[List[Any]],
                               ioTask: IO[List[Any]]
                             ): IO[Unit] = {
    for {
      _ <- IO.println("")
      _ <- IO.println(s"Workload: $title")
      lazyRes <- measure("LazyList runtime", warmups, runs, lazyTask)
      ioRes <- measure("fs2 IO runtime", warmups, runs, ioTask)
      _ <- IO.raiseWhen(lazyRes.sample != ioRes.sample)(
        new IllegalStateException(s"Parity check failed for workload '$title'")
      )
      _ <- IO.println("Parity check: OK")
      _ <- printResult(lazyRes)
      _ <- printResult(ioRes)
      lazyAvg = average(lazyRes.timesMs)
      ioAvg = average(ioRes.timesMs)
      ratio = if (lazyAvg == 0.0) 0.0 else ioAvg / lazyAvg
      _ <- IO.println(f"Speed ratio (fs2/lazy): $ratio%.3f")
    } yield ()
  }

  def run(args: List[String]): IO[ExitCode] = {
    val parsed = parseArgs(args)
    val listSize = parsed.get("listSize").flatMap(parseInt).getOrElse(2000)
    val takeN = parsed.get("takeN").flatMap(parseInt).getOrElse(2000)
    val warmups = parsed.get("warmups").flatMap(parseInt).getOrElse(1)
    val runs = parsed.get("runs").flatMap(parseInt).getOrElse(5)
    val workload = parsed.getOrElse("workload", "both")

    (for {
      _ <- IO.println("Benchmark: fs2 vs LazyList")
      _ <- IO.println(s"listSize=$listSize, takeN=$takeN, warmups=$warmups, runs=$runs, workload=$workload")
      _ <- if (workload == "member" || workload == "both")
        compareWorkload(
          "member relation over fixed list",
          warmups,
          runs,
          lazyMemberTask(listSize, takeN),
          ioMemberTask(listSize, takeN)
        )
      else IO.unit
      _ <- if (workload == "branching" || workload == "both")
        compareWorkload(
          "interleaving/branching goal (cond_i + always_o)",
          warmups,
          runs,
          lazyBranchingTask(takeN),
          ioBranchingTask(takeN)
        )
      else IO.unit
      _ <- IO.raiseWhen(workload != "member" && workload != "branching" && workload != "both")(
        new IllegalArgumentException("workload must be one of: member, branching, both")
      )
    } yield ExitCode.Success).handleErrorWith { e =>
      IO.println(s"Benchmark failed: ${e.getMessage}").as(ExitCode.Error)
    }
  }
}
