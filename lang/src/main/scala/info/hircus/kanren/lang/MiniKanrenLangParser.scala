package info.hircus.kanren.lang

import fastparse._
import fastparse.NoWhitespace._
import info.hircus.kanren.dslir.QueryIR
import info.hircus.kanren.dslir.QueryIR.{Expr, Goal, Program}

object MiniKanrenLangParser {

  final case class ParseError(message: String, line: Int, column: Int)

  private def ws[$: P]: P[Unit] = P((CharsWhileIn(" \r\n\t", 1) | ("#" ~ CharsWhile(_ != '\n', 0))).rep)
  private def keyword[$: P](kw: String): P[Unit] = P(kw ~ !CharIn("a-zA-Z0-9_"))
  private def ident[$: P]: P[String] = P((CharIn("a-zA-Z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def number[$: P]: P[Int] = P(StringIn("+", "-").? ~ CharsWhileIn("0-9", 1)).!.map(_.toInt)
  private def stringLit[$: P]: P[String] = P("\"" ~/ CharsWhile(c => c != '"' && c != '\\').! ~ "\"")

  private def term[$: P]: P[Expr] = P(ws ~ (listTerm | nilTerm | stringTerm | numberTerm | varRef) ~ ws)
  private def varRef[$: P]: P[Expr] = ident.map(QueryIR.Ref)
  private def numberTerm[$: P]: P[Expr] = number.map(n => QueryIR.Atom(n))
  private def stringTerm[$: P]: P[Expr] = stringLit.map(s => QueryIR.Atom(s))
  private def nilTerm[$: P]: P[Expr] = keyword("nil").map(_ => QueryIR.Atom(Nil))
  private def listTerm[$: P]: P[Expr] = P("[" ~/ term.rep(sep = ws ~ "," ~ ws) ~ "]").map(xs => QueryIR.ListExpr(xs.toList))

  private def eqGoal[$: P]: P[Goal] = P(keyword("eq") ~/ ws ~ term ~ ws ~ term).map { case (a, b) => QueryIR.Eq(a, b) }
  private def neqGoal[$: P]: P[Goal] = P(keyword("neq") ~/ ws ~ term ~ ws ~ term).map { case (a, b) => QueryIR.Neq(a, b) }
  private def callGoal[$: P]: P[Goal] =
    P(ident ~ ws ~ "(" ~ ws ~ term.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map {
      case (name, args) => QueryIR.Rel(name, args.toList)
    }

  private def allGoal[$: P]: P[Goal] =
    P(keyword("all") ~/ ws ~ "{" ~ ws ~ goal.rep(sep = ws ~ ";" ~ ws) ~ ws ~ "}").map(gs => QueryIR.Conj(gs.toList))

  private def anyGoal[$: P]: P[Goal] =
    P(keyword("any") ~/ ws ~ "{" ~ ws ~ goal.rep(sep = ws ~ ";" ~ ws) ~ ws ~ "}").map(gs => QueryIR.Disj(gs.toList))

  private def goal[$: P]: P[Goal] = P(eqGoal | neqGoal | allGoal | anyGoal | callGoal)

  private def runProgram[$: P]: P[Program] = {
    P(
      keyword("run") ~/ ws ~
        number.? ~
        ws ~
        ident ~
        ws ~
        "{" ~
        ws ~
        goal.rep(sep = ws ~ ";" ~ ws) ~
        ws ~
        "}"
    ).map { case (nOpt, focus, goals) =>
      QueryIR.Program(
        focus = focus,
        goals = goals.toList,
        limit = nOpt.getOrElse(-1),
        constrained = false
      )
    }
  }

  private def root[$: P]: P[Program] = P(ws ~ runProgram ~ ws ~ End)

  def parse(input: String): Either[ParseError, Program] = {
    fastparse.parse(input, root(using _)) match {
      case Parsed.Success(program, _) => Right(program)
      case f: Parsed.Failure =>
        val position = f.index
        val (line, col) = indexToLineCol(input, position)
        Left(ParseError(f.trace().longMsg, line, col))
    }
  }

  private def indexToLineCol(input: String, index: Int): (Int, Int) = {
    val safeIndex = math.max(0, math.min(index, input.length))
    val before = input.substring(0, safeIndex)
    val line = before.count(_ == '\n') + 1
    val col = before.reverse.takeWhile(_ != '\n').length + 1
    (line, col)
  }
}
