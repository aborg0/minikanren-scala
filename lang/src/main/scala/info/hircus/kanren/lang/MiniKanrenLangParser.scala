package info.hircus.kanren.lang

import fastparse._
import fastparse.NoWhitespace._
import info.hircus.kanren.dslir.QueryIR
import info.hircus.kanren.dslir.QueryIR.{Declaration, Expr, Goal, Program}

object MiniKanrenLangParser {

  final case class ParseError(message: String, line: Int, column: Int)

  private def ws[$: P]: P[Unit] = P((CharsWhileIn(" \r\n\t", 1) | ("#" ~ CharsWhile(_ != '\n', 0))).rep)
  private def keyword[$: P](kw: String): P[Unit] = P(kw ~ !CharIn("a-zA-Z0-9_"))
  private def ident[$: P]: P[String] = P((CharIn("a-zA-Z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def lowerIdent[$: P]: P[String] = P((CharIn("a-z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def upperIdent[$: P]: P[String] = P((CharIn("A-Z_") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def number[$: P]: P[Int] = P(StringIn("+", "-").? ~ CharsWhileIn("0-9", 1)).!.map(_.toInt)
  private def stringLit[$: P]: P[String] = P("\"" ~/ CharsWhile(c => c != '"' && c != '\\').! ~ "\"")
  private def charLit[$: P]: P[Char] = P("'" ~/ CharsWhile(c => c != '\'' && c != '\\').! ~ "'").map(_.charAt(0))

  private def term[$: P]: P[Expr] = P(ws ~ (listTerm | nilTerm | stringTerm | charTerm | numberTerm | varRef) ~ ws)
  private def varRef[$: P]: P[Expr] = ident.map(QueryIR.Ref)
  private def numberTerm[$: P]: P[Expr] = number.map(n => QueryIR.Atom(n))
  private def stringTerm[$: P]: P[Expr] = stringLit.map(s => QueryIR.Atom(s))
  private def charTerm[$: P]: P[Expr] = charLit.map(c => QueryIR.Atom(c))
  private def nilTerm[$: P]: P[Expr] = keyword("nil").map(_ => QueryIR.Atom(Nil))
  private def listTerm[$: P]: P[Expr] =
    P("[" ~/ term.rep(sep = ws ~ "," ~ ws) ~ (ws ~ "|" ~/ ws ~ term).? ~ "]").map {
      case (heads, Some(tail)) =>
        heads.toList.foldRight(tail: Expr) { case (head, acc) => QueryIR.Pair(head, acc) }
      case (heads, None) => QueryIR.ListExpr(heads.toList)
    }

  private def eqGoal[$: P]: P[Goal] = P(keyword("eq") ~/ ws ~ term ~ ws ~ term).map { case (a, b) => QueryIR.Eq(a, b) }
  private def neqGoal[$: P]: P[Goal] = P(keyword("neq") ~/ ws ~ term ~ ws ~ term).map { case (a, b) => QueryIR.Neq(a, b) }
  private def callGoal[$: P]: P[Goal] =
    P(ident ~ ws ~ "(" ~ ws ~ term.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map {
      case (name, args) => QueryIR.Rel(name, args.toList)
    }

  private def newEqGoal[$: P]: P[Goal] = P(term ~ ws ~ "=" ~ ws ~ term).map { case (a, b) => QueryIR.Eq(a, b) }
  private def notEqGoal[$: P]: P[Goal] = P(keyword("not") ~/ ws ~ newEqGoal).map {
    case QueryIR.Eq(lhs, rhs) => QueryIR.Neq(lhs, rhs)
    case other => other
  }

  private def infixCallGoal[$: P]: P[Goal] =
    P(term ~ ws ~ ident ~ ws ~ term).map { case (lhs, name, rhs) => QueryIR.Rel(name, List(lhs, rhs)) }

  private def newAtomicGoal[$: P]: P[Goal] = P(notEqGoal | newEqGoal | callGoal | infixCallGoal)

  private def newGoal[$: P]: P[Goal] = P(newAtomicGoal.rep(sep = ws ~ "&" ~ ws, min = 1)).map {
    case Nil => QueryIR.Conj(Nil)
    case head :: Nil => head
    case many => QueryIR.Conj(many.toList)
  }

  private def constDecl[$: P]: P[Declaration] =
    P(keyword("const") ~/ ws ~ ident.rep(sep = ws ~ "," ~ ws)).map(xs => QueryIR.ConstDecl(xs.toList))

  private def varDecl[$: P]: P[Declaration] =
    P(keyword("var") ~/ ws ~ ident.rep(sep = ws ~ "," ~ ws) ~ (ws ~ keyword("in") ~/ ws ~ listTerm).?).map {
      case (xs, domain) => QueryIR.VarDecl(xs.toList, domain)
    }

  private def relDecl[$: P]: P[Declaration] =
    P(keyword("rel") ~/ ws ~ "/" ~/ ws ~ number ~ ws ~ keyword("infix") ~/ ws ~ ident).map {
      case (arity, name) => QueryIR.RelDecl(name, arity, infix = true)
    }

  private def ruleHead[$: P]: P[(String, List[String])] =
    P(ident ~ ws ~ "(" ~ ws ~ ident.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map { case (name, params) => (name, params.toList) }

  private def ruleDecl[$: P]: P[Declaration] =
    P(ruleHead ~ ws ~ "=" ~/ ws ~ newGoal).map {
      case (name, params, body) =>
        QueryIR.Rule(name, params, body)
    }

  private def factDecl[$: P]: P[Declaration] =
    P(callGoal | infixCallGoal).map {
      case QueryIR.Rel(name, args) => QueryIR.Fact(name, args)
      case _ => throw new IllegalArgumentException("Expected relation fact")
    }

  private def askDecl[$: P]: P[(String, List[Goal])] =
    P(keyword("ask") ~/ ws ~ ident ~ ws ~ newGoal.rep(sep = ws ~ "&" ~ ws, min = 1)).map { case (focus, goals) => (focus, goals.toList) }

  private def declarativeLine[$: P]: P[Either[Declaration, (String, List[Goal])]] =
    P(constDecl.map(Left(_)) | varDecl.map(Left(_)) | relDecl.map(Left(_)) | ruleDecl.map(Left(_)) | askDecl.map(Right(_)) | factDecl.map(Left(_)))

  private def declarativeProgram[$: P]: P[Program] =
    P(ws ~ declarativeLine.rep(sep = ws, min = 1) ~ ws ~ End).map { entries =>
      val decls = entries.collect { case Left(decl) => decl }.toList
      val ask = entries.collectFirst { case Right(value) => value }.getOrElse(throw new IllegalArgumentException("Missing ask clause"))
      QueryIR.Program(focus = ask._1, goals = ask._2, limit = -1, constrained = false, declarations = decls)
    }

  private def prologVarTerm[$: P]: P[Expr] = P(upperIdent).map(QueryIR.Ref)
  private def prologAtomTerm[$: P]: P[Expr] = P(lowerIdent).map(name => QueryIR.Atom(name))
  private def prologListTerm[$: P]: P[Expr] =
    P("[" ~/ prologTerm.rep(sep = ws ~ "," ~ ws) ~ (ws ~ "|" ~/ ws ~ prologTerm).? ~ "]").map {
      case (heads, Some(tail)) =>
        heads.toList.foldRight(tail: Expr) { case (head, acc) => QueryIR.Pair(head, acc) }
      case (heads, None) => QueryIR.ListExpr(heads.toList)
    }
  private def prologTerm[$: P]: P[Expr] = P(ws ~ (prologListTerm | nilTerm | stringTerm | charTerm | numberTerm | prologVarTerm | prologAtomTerm) ~ ws)

  private def prologCall[$: P]: P[QueryIR.Rel] =
    P(lowerIdent ~ ws ~ "(" ~ ws ~ prologTerm.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map {
      case (name, args) => QueryIR.Rel(name, args.toList)
    }

  private def prologCallGoal(rel: QueryIR.Rel): Goal = rel match {
    case QueryIR.Rel("eq", List(lhs, rhs)) => QueryIR.Eq(lhs, rhs)
    case QueryIR.Rel("neq", List(lhs, rhs)) => QueryIR.Neq(lhs, rhs)
    case other => other
  }

  private def extractRel(value: Any): QueryIR.Rel = value match {
    case rel: QueryIR.Rel => rel
    case product: Product =>
      product.productIterator.collectFirst { case rel: QueryIR.Rel => rel }
        .getOrElse(throw new IllegalArgumentException("Missing relation in Prolog parser output"))
    case _ => throw new IllegalArgumentException("Invalid Prolog relation output")
  }

  private def extractGoal(value: Any): Goal = value match {
    case goal: Goal => goal
    case product: Product =>
      product.productIterator.collectFirst { case goal: Goal => goal }
        .getOrElse(throw new IllegalArgumentException("Missing goal in Prolog parser output"))
    case _ => throw new IllegalArgumentException("Invalid Prolog goal output")
  }

  private def prologGoal[$: P]: P[Goal] =
    P(prologCall.rep(sep = ws ~ "," ~ ws, min = 1)).map { calls =>
      val goals = calls.toList.map(prologCallGoal)
      goals match {
        case head :: Nil => head
        case many => QueryIR.Conj(many)
      }
    }

  private def prologFactDecl[$: P]: P[Declaration] =
    P(prologCall ~ ws ~ ".").map { value =>
      val rel = extractRel(value)
      QueryIR.Fact(rel.name, rel.args)
    }

  private def prologRuleDecl[$: P]: P[Declaration] =
    P(prologCall ~ ws ~ ":-" ~ ws ~ prologGoal ~ ws ~ ".").map {
      value =>
        val headRel = extractRel(value)
        val body = extractGoal(value)
        val params = headRel.args.indices.map(i => "_p" + i.toString).toList
        val headMatches = headRel.args.zip(params).map { case (arg, param) => QueryIR.Eq(QueryIR.Ref(param), arg) }
        val ruleBody = QueryIR.Conj(headMatches :+ body)
        QueryIR.Rule(headRel.name, params, ruleBody)
    }

  private def queryFocus(goal: Goal): Option[String] = {
    def inExpr(expr: Expr): List[String] = expr match {
      case QueryIR.Ref(name) => List(name)
      case QueryIR.Atom(_) => Nil
      case QueryIR.Pair(head, tail) => inExpr(head) ++ inExpr(tail)
      case QueryIR.ListExpr(items) => items.flatMap(inExpr)
    }
    def inGoal(g: Goal): List[String] = g match {
      case QueryIR.Eq(lhs, rhs) => inExpr(lhs) ++ inExpr(rhs)
      case QueryIR.Neq(lhs, rhs) => inExpr(lhs) ++ inExpr(rhs)
      case QueryIR.Rel(_, args) => args.flatMap(inExpr)
      case QueryIR.Conj(goals) => goals.flatMap(inGoal)
      case QueryIR.Disj(goals) => goals.flatMap(inGoal)
    }
    inGoal(goal).headOption
  }

  private def prologQuery[$: P]: P[(String, Goal)] =
    P("?-" ~/ ws ~ prologGoal ~ ws ~ ".").map { goal =>
      val focus = queryFocus(goal).getOrElse("result")
      (focus, goal)
    }

  private def prologClause[$: P]: P[Declaration] =
    P(ws ~ (prologRuleDecl | prologFactDecl) ~ ws)

  private def prologProgram[$: P]: P[Program] =
    P(ws ~ prologClause.rep ~ ws ~ prologQuery ~ ws ~ End).map {
      case (decls, (focus, goal)) =>
        QueryIR.Program(
          focus = focus,
          goals = List(goal),
          limit = -1,
          constrained = false,
          declarations = decls.toList
        )
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

  private def rootOld[$: P]: P[Program] = P(ws ~ runProgram ~ ws ~ End)
  private def rootNew[$: P]: P[Program] = P(declarativeProgram)
  private def rootProlog[$: P]: P[Program] = P(prologProgram)

  def parse(input: String): Either[ParseError, Program] = {
    fastparse.parse(input, rootOld(using _)) match {
      case Parsed.Success(program, _) => Right(program)
      case oldFailure: Parsed.Failure =>
        fastparse.parse(input, rootNew(using _)) match {
          case Parsed.Success(program, _) => Right(program)
          case newFailure: Parsed.Failure =>
            fastparse.parse(input, rootProlog(using _)) match {
              case Parsed.Success(program, _) => Right(program)
              case prologFailure: Parsed.Failure =>
                val position = prologFailure.index
                val (line, col) = indexToLineCol(input, position)
                Left(ParseError(prologFailure.trace().longMsg, line, col))
            }
        }
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
