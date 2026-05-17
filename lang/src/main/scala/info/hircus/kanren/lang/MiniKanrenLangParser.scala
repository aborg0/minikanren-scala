package info.hircus.kanren.lang

import fastparse._
import fastparse.NoWhitespace._
import info.hircus.kanren.dslir.QueryIR
import info.hircus.kanren.dslir.QueryIR.{Declaration, Expr, Goal, Program}

object MiniKanrenLangParser {

  final case class ParseError(message: String, line: Int, column: Int)

  private def ws[$: P]: P[Unit] = P((
    CharsWhileIn(" \r\n\t", 1) |
    ("#" ~ CharsWhile(_ != '\n', 0)) |
    ("//" ~ CharsWhile(_ != '\n', 0)) |
    ("/*" ~ (!"*/" ~ AnyChar).rep ~ "*/") |
    ("%" ~ CharsWhile(_ != '\n', 0))
  ).rep)
  private def keyword[$: P](kw: String): P[Unit] = P(kw ~ !CharIn("a-zA-Z0-9_"))
  private def ident[$: P]: P[String] = P((CharIn("a-zA-Z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def lowerIdent[$: P]: P[String] = P((CharIn("a-z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def upperIdent[$: P]: P[String] = P((CharIn("A-Z_") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!)
  private def number[$: P]: P[Int] = P(StringIn("+", "-").? ~ CharsWhileIn("0-9", 1)).!.map(_.toInt)
  
  // Operator parsing for infix notation (highest precedence: *, /; then +, -; then comparisons)
  private def addOp[$: P]: P[String] = P(StringIn("+", "-")).!
  private def mulOp[$: P]: P[String] = P(StringIn("*", "/")).!
  private def cmpOp[$: P]: P[String] = P(StringIn("=:=", "=\\=", "=<", ">=", "<", ">")).!
  private def opToRelation(op: String, lhs: Expr, rhs: Expr): Goal = op match {
    case "+" => QueryIR.Rel("add_o", List(lhs, rhs, QueryIR.Ref("_result")))
    case "-" => QueryIR.Rel("sub_o", List(lhs, rhs, QueryIR.Ref("_result")))
    case "*" => QueryIR.Rel("mul_o", List(lhs, rhs, QueryIR.Ref("_result")))
    case "/" => QueryIR.Rel("div_o", List(lhs, rhs, QueryIR.Ref("_result")))
    case "<" => QueryIR.Rel("lt_o", List(lhs, rhs))
    case ">" => QueryIR.Rel("gt_o", List(lhs, rhs))
    case "=<" => QueryIR.Rel("le_o", List(lhs, rhs))
    case ">=" => QueryIR.Rel("ge_o", List(lhs, rhs))
    case "=:=" => QueryIR.Rel("eq_num_o", List(lhs, rhs))
    case "=\\=" => QueryIR.Rel("ne_num_o", List(lhs, rhs))
    case _ => throw new IllegalArgumentException("Unknown operator: " + op)
  }
  private def escapedChar[$: P]: P[Char] =
    P("\\" ~/ (
      ("u" ~ CharIn("0-9a-fA-F").rep(exactly = 4).!).map(hex => Integer.parseInt(hex, 16).toChar) |
      CharIn("\\\"'nrt").!.map {
        case "\\" => '\\'
        case "\"" => '"'
        case "'" => '\''
        case "n" => '\n'
        case "r" => '\r'
        case "t" => '\t'
        case _ => throw new IllegalArgumentException("Unknown escape sequence")
      }
    ))

  private def plainStringChar[$: P]: P[Char] =
    P(CharPred(c => c != '"' && c != '\\').!).map(_.charAt(0))

  private def plainCharLiteral[$: P]: P[Char] =
    P(CharPred(c => c != '\'' && c != '\\').!).map(_.charAt(0))

  private def plainSingleQuotedStringChar[$: P]: P[Char] =
    P(CharPred(c => c != '\'' && c != '\\').!).map(_.charAt(0))

  private def singleQuotedStringLit[$: P]: P[String] =
    P("'" ~/ (escapedChar | plainSingleQuotedStringChar).rep ~ "'").map(_.mkString)

  private def stringLit[$: P]: P[String] =
    P("\"" ~/ (escapedChar | plainStringChar).rep ~ "\"").map(_.mkString)

  private def charLit[$: P]: P[Char] =
    P("'" ~/ (escapedChar | plainCharLiteral) ~ "'")

  private def term[$: P]: P[Expr] = P(ws ~ (listTerm | nilTerm | stringTerm | charTerm | numberTerm | varRef) ~ ws)
  private def varRef[$: P]: P[Expr] = ident.map(QueryIR.Ref.apply)
  private def numberTerm[$: P]: P[Expr] = number.map(n => QueryIR.Atom(n))
  private def stringTerm[$: P]: P[Expr] = stringLit.map(s => QueryIR.Atom(s))
  private def charTerm[$: P]: P[Expr] = charLit.map(c => QueryIR.Atom(c))
  private def singleQuotedStringTerm[$: P]: P[Expr] = singleQuotedStringLit.map(s => QueryIR.Atom(s))
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

  private def prefixCallGoal[$: P]: P[Goal] =
    P(ident ~ ws ~ term.rep(sep = ws, min = 1)).map {
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

  private def prologVarTerm[$: P]: P[Expr] = P(upperIdent).map(QueryIR.Ref.apply)
  private def prologAtomTerm[$: P]: P[Expr] = P(lowerIdent).map(name => QueryIR.Atom(name))
  private def prologListTerm[$: P]: P[Expr] =
    P("[" ~/ prologTerm.rep(sep = ws ~ "," ~ ws) ~ (ws ~ "|" ~/ ws ~ prologTerm).? ~ "]").map {
      case (heads, Some(tail)) =>
        heads.toList.foldRight(tail: Expr) { case (head, acc) => QueryIR.Pair(head, acc) }
      case (heads, None) => QueryIR.ListExpr(heads.toList)
    }
  private def prologTerm[$: P]: P[Expr] = P(ws ~ (prologListTerm | nilTerm | stringTerm | singleQuotedStringTerm | charTerm | numberTerm | prologVarTerm | prologAtomTerm) ~ ws)

  private def prologCall[$: P]: P[QueryIR.Rel] =
    P(lowerIdent ~ ws ~ "(" ~ ws ~ prologTerm.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map {
      case (name, args) => QueryIR.Rel(name, args.toList)
    }

  private def prologCallGoal(rel: QueryIR.Rel): Goal = rel match {
    case QueryIR.Rel("eq", List(lhs, rhs)) => QueryIR.Eq(lhs, rhs)
    case QueryIR.Rel("neq", List(lhs, rhs)) => QueryIR.Neq(lhs, rhs)
    case other => other
  }

  // Prolog operator expressions with precedence
  private def prologMulExpr[$: P]: P[Expr] =
    P(prologTerm ~ (ws ~ mulOp ~ ws ~ prologTerm).rep).map {
      case (left, ops) if ops.isEmpty => left
      case (left, ops) => ops.foldLeft(left) { case (acc, (op, right)) =>
        QueryIR.Atom((op, acc, right))
      }
    }

  private def prologAddExpr[$: P]: P[Expr] =
    P(prologMulExpr ~ (ws ~ addOp ~ ws ~ prologMulExpr).rep).map {
      case (left, ops) if ops.isEmpty => left
      case (left, ops) => ops.foldLeft(left) { case (acc, (op, right)) =>
        QueryIR.Atom((op, acc, right))
      }
    }

  private def prologOperatorGoal[$: P]: P[Goal] =
    P(prologAddExpr ~ ws ~ cmpOp ~ ws ~ prologAddExpr).map { case (lhs, op, rhs) =>
      opToRelation(op, lhs, rhs)
    }

  private def prologOperatorEqGoal[$: P]: P[Goal] =
    P(prologAddExpr ~ ws ~ "=" ~ ws ~ prologAddExpr).map { case (lhs, rhs) =>
      (lhs, rhs) match {
        case (QueryIR.Atom((op: String, l: Expr, r: Expr)), _) =>
          QueryIR.Rel(
            if (op == "+") "add_o" else if (op == "-") "sub_o" else if (op == "*") "mul_o" else "div_o",
            List(l, r, rhs)
          )
        case (_, QueryIR.Atom((op: String, l: Expr, r: Expr))) =>
          QueryIR.Rel(
            if (op == "+") "add_o" else if (op == "-") "sub_o" else if (op == "*") "mul_o" else "div_o",
            List(l, r, lhs)
          )
        case _ => QueryIR.Eq(lhs, rhs)
      }
    }

  private def prologAtomicGoal[$: P]: P[Goal] = P(prologOperatorGoal | prologOperatorEqGoal | prologCall.map(prologCallGoal))

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
    P(prologAtomicGoal.rep(sep = ws ~ "," ~ ws, min = 1)).map { goals =>
      goals.toList match {
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
      case (headRel: QueryIR.Rel, body: Goal) =>
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

  private def goal[$: P]: P[Goal] = P(eqGoal | neqGoal | allGoal | anyGoal | callGoal | prefixCallGoal)

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


  // Flix Fixpoints DSL (Datalog-like)
  // Convention: lowercase identifiers are atoms, uppercase identifiers are variables.
  private def flixAtomTerm[$: P]: P[Expr] = P(lowerIdent).map(name => QueryIR.Atom(name))
  private def flixVarTerm[$: P]: P[Expr] = P(upperIdent).map(QueryIR.Ref.apply)
  private def flixTerm[$: P]: P[Expr] = P(ws ~ (numberTerm | stringTerm | charTerm | nilTerm | flixVarTerm | flixAtomTerm) ~ ws)

  private def flixHeadParam[$: P]: P[String] = P(upperIdent)

  private def flixRelGoal[$: P]: P[QueryIR.Rel] =
    P(lowerIdent ~ "(" ~ ws ~ flixTerm.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")").map {
      case (name, args) => QueryIR.Rel(name, args.toList)
    }

  private def flixBodyGoal[$: P]: P[Goal] = P(flixRelGoal)

  private def flixFact[$: P]: P[Declaration] =
    P(lowerIdent ~ "(" ~ ws ~ flixTerm.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")" ~ ws ~ ".").map {
      case (name, args) => QueryIR.Fact(name, args.toList)
    }

  private def flixRule[$: P]: P[Declaration] =
    P(lowerIdent ~ "(" ~ ws ~ flixHeadParam.rep(sep = ws ~ "," ~ ws) ~ ws ~ ")" ~ ws ~ ":-" ~ ws ~
      flixBodyGoal.rep(sep = ws ~ "," ~ ws, min = 1) ~ ws ~ ".").map {
      case (headName, headArgs, bodyGoals) =>
        val params = headArgs.toList
        val body = bodyGoals.toList match {
          case head :: Nil => head
          case many => QueryIR.Conj(many)
        }
        QueryIR.Rule(headName, params, body)
    }

  private def flixQuery[$: P]: P[(String, Goal)] =
    P("query" ~ ws ~ flixRelGoal ~ ws ~ ".").map {
      case rel =>
        val focus = rel.args.collectFirst { case QueryIR.Ref(name) => name }.getOrElse("result")
        (focus, rel)
    }
  private def flixClause[$: P]: P[Declaration] = P(ws ~ (flixRule | flixFact) ~ ws)
  private def flixProgram[$: P]: P[Program] =
    P(ws ~ flixClause.rep ~ ws ~ flixQuery ~ ws ~ End).map {
      case (decls, (focus, goal)) =>
        QueryIR.Program(focus = focus, goals = List(goal), limit = -1, constrained = false, declarations = decls.toList)
    }
  private def rootFlix[$: P]: P[Program] = P(flixProgram)

  // Cypher DSL (subset: MATCH ... WHERE ... RETURN ...)
  private def cypherIdent[$: P]: P[String] = P(CharIn("a-zA-Z") ~ CharsWhileIn("a-zA-Z0-9_", 0)).!
  private sealed trait CypherWhereOperand
  private final case class CyWhereRef(name: String) extends CypherWhereOperand
  private final case class CyWhereAtom(value: Any) extends CypherWhereOperand
  private final case class CyWhereProp(base: String, field: String) extends CypherWhereOperand
  private sealed trait CypherWhereExpr
  private final case class CyWherePred(left: CypherWhereOperand, op: String, right: CypherWhereOperand) extends CypherWhereExpr
  private final case class CyWhereAnd(parts: List[CypherWhereExpr]) extends CypherWhereExpr
  private final case class CyWhereOr(parts: List[CypherWhereExpr]) extends CypherWhereExpr
  private sealed trait CypherRelDir
  private case object CyDirOut extends CypherRelDir
  private case object CyDirIn extends CypherRelDir
  private case object CyDirAny extends CypherRelDir

  private def cypherBoolLit[$: P]: P[Expr] =
    P(StringIn("true", "false").!).map(v => QueryIR.Atom(v == "true"))

  private def cypherNullLit[$: P]: P[Expr] =
    P("null").map(_ => QueryIR.Atom(null))

  private def cypherNumberAny(raw: String): Any = {
    val normalized = raw.trim
    if (normalized.exists(ch => ch == '.' || ch == 'e' || ch == 'E')) normalized.toDouble
    else normalized.toIntOption.orElse(normalized.toLongOption).getOrElse(normalized.toDouble)
  }

  private val cypherNumberPattern = "^[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[eE][+-]?\\d+)?$".r

  private def cypherNumberToken[$: P]: P[String] =
    P(CharIn("+\\-0-9.eE").rep(min = 1).!).filter(token => cypherNumberPattern.matches(token))

  private def cypherNumberValue[$: P]: P[Any] =
    cypherNumberToken.map(cypherNumberAny)

  private def cypherNumberLit[$: P]: P[Expr] =
    cypherNumberValue.map(v => QueryIR.Atom(v))

  private def cypherExprToAny(expr: Expr): Any = expr match {
    case QueryIR.Atom(value) => value
    case QueryIR.ListExpr(items) => items.map(cypherExprToAny)
    case QueryIR.Pair(head, tail) => (cypherExprToAny(head), cypherExprToAny(tail))
    case QueryIR.Ref(name) => name
  }

  private def cypherNodePropValue[$: P]: P[Expr] =
    P(cypherMapValue | cypherListValue | cypherBoolLit | cypherNullLit | stringLit.map(s => QueryIR.Atom(s): Expr) | cypherNumberLit)

  private def cypherMapValue[$: P]: P[Expr] =
    P("{" ~ ws ~ cypherNodeProp.rep(sep = ws ~ "," ~ ws) ~ ws ~ "}").map { entries =>
      QueryIR.Atom(scala.collection.immutable.Map(entries.toList.map { case (k, v) => k -> cypherExprToAny(v) }*))
    }

  private def cypherListValue[$: P]: P[Expr] =
    P("[" ~ ws ~ cypherNodePropValue.rep(sep = ws ~ "," ~ ws) ~ ws ~ "]").map(values => QueryIR.ListExpr(values.toList))

  private def cypherNodeProp[$: P]: P[(String, Expr)] =
    P(cypherIdent.! ~ ws ~ ":" ~ ws ~ cypherNodePropValue)

  private def cypherNodeProps[$: P]: P[List[(String, Expr)]] =
    P("{" ~ ws ~ cypherNodeProp.rep(sep = ws ~ "," ~ ws) ~ ws ~ "}").map(_.toList)

  private def cypherNode[$: P]: P[(String, Option[String], List[(String, Expr)])] =
    P("(" ~ ws ~ cypherIdent.! ~ (ws ~ ":" ~ ws ~ cypherIdent.!).? ~ (ws ~ cypherNodeProps).? ~ ws ~ ")").map {
      case (name, label, propsOpt) => (name, label, propsOpt.getOrElse(Nil))
    }

  private def cypherRelSpec[$: P]: P[(Option[String], String)] = P(
    (":" ~ cypherIdent.!).map(rel => (None, rel)) |
      (cypherIdent.! ~ ":" ~ cypherIdent.!).map { case (v, rel) => (Some(v), rel) }
  )

  private def cypherRelLink[$: P]: P[(Option[String], String, CypherRelDir)] = P(
    ("-[" ~ cypherRelSpec ~ "]->").map { case (rVar, rel) => (rVar, rel, CyDirOut: CypherRelDir) } |
      ("<-[" ~ cypherRelSpec ~ "]-").map { case (rVar, rel) => (rVar, rel, CyDirIn: CypherRelDir) } |
      ("-[" ~ cypherRelSpec ~ "]-").map { case (rVar, rel) => (rVar, rel, CyDirAny: CypherRelDir) }
  )

  private def cypherPattern[$: P]: P[(String, Option[String], List[(String, Expr)], Option[String], String, String, Option[String], List[(String, Expr)], CypherRelDir)] = P(
    for {
      left <- cypherNode
      _ <- ws
      link <- cypherRelLink
      _ <- ws
      right <- cypherNode
    } yield (left._1, left._2, left._3, link._1, link._2, right._1, right._2, right._3, link._3)
  )

  private def cypherPairGoal(rel: String, from: String, to: String): Goal = rel match {
    case "eq" => QueryIR.Eq(QueryIR.Ref(from), QueryIR.Ref(to))
    case "neq" => QueryIR.Neq(QueryIR.Ref(from), QueryIR.Ref(to))
    case other => QueryIR.Rel(other, List(QueryIR.Ref(from), QueryIR.Ref(to)))
  }

  private def cypherPatternGoals(pattern: (String, Option[String], List[(String, Expr)], Option[String], String, String, Option[String], List[(String, Expr)], CypherRelDir)): List[Goal] = pattern match {
    case (a, aLabel, aProps, relVar, rel, b, bLabel, bProps, dir) =>
      val relGoal = dir match {
        case CyDirOut => cypherPairGoal(rel, a, b)
        case CyDirIn => cypherPairGoal(rel, b, a)
        case CyDirAny =>
          rel match {
            case "eq" | "neq" => cypherPairGoal(rel, a, b)
            case _ => QueryIR.Disj(List(cypherPairGoal(rel, a, b), cypherPairGoal(rel, b, a)))
          }
      }
      val labelGoals =
        aLabel.map(label => QueryIR.Rel("label_o", List(QueryIR.Ref(a), QueryIR.Atom(label)))).toList ++
          bLabel.map(label => QueryIR.Rel("label_o", List(QueryIR.Ref(b), QueryIR.Atom(label)))).toList
      val propGoals =
        aProps.map { case (field, value) => QueryIR.Rel("prop_o", List(QueryIR.Ref(a), QueryIR.Atom(field), value)) } ++
          bProps.map { case (field, value) => QueryIR.Rel("prop_o", List(QueryIR.Ref(b), QueryIR.Atom(field), value)) }
      val relVarGoals = relVar.map(v => QueryIR.Eq(QueryIR.Ref(v), QueryIR.Atom(rel))).toList
      relGoal :: (labelGoals ++ propGoals ++ relVarGoals)
  }

  private def cypherWhereOperand[$: P]: P[CypherWhereOperand] =
    P(
      (cypherIdent.! ~ ws ~ "." ~ ws ~ cypherIdent.!).map { case (base, field) => CyWhereProp(base, field): CypherWhereOperand } |
        StringIn("true", "false").!.map(v => CyWhereAtom(v == "true"): CypherWhereOperand) |
        "null".map(_ => CyWhereAtom(null): CypherWhereOperand) |
        stringLit.map(s => CyWhereAtom(s): CypherWhereOperand) |
        cypherNumberValue.map(v => CyWhereAtom(v): CypherWhereOperand) |
        cypherIdent.!.map(name => CyWhereRef(name): CypherWhereOperand)
    )

  private def cypherWherePredicate[$: P]: P[CypherWhereExpr] =
    P(cypherWhereOperand ~ ws ~ StringIn("=", "<>").! ~ ws ~ cypherWhereOperand).map {
      case (a, op, b) => CyWherePred(a, op, b)
    }

  private def cypherWhereAtom[$: P]: P[CypherWhereExpr] =
    P(("(" ~ ws ~ cypherWhereExpr ~ ws ~ ")") | cypherWherePredicate)

  private def cypherWhereAndExpr[$: P]: P[CypherWhereExpr] =
    P(cypherWhereAtom.rep(sep = ws ~ StringIn("AND", "and") ~ ws, min = 1)).map { parts =>
      parts.toList match {
        case head :: Nil => head
        case many => CyWhereAnd(many)
      }
    }

  private def cypherWhereExpr[$: P]: P[CypherWhereExpr] =
    P(cypherWhereAndExpr.rep(sep = ws ~ StringIn("OR", "or") ~ ws, min = 1)).map { parts =>
      parts.toList match {
        case head :: Nil => head
        case many => CyWhereOr(many)
      }
    }

  private def cypherWhere[$: P]: P[CypherWhereExpr] =
    P("WHERE" ~ ws ~ cypherWhereExpr)

  private def lowerCypherWhereOperand(op: CypherWhereOperand, counter: () => Int): (Expr, List[Goal]) = op match {
    case CyWhereRef(name) => (QueryIR.Ref(name), Nil)
    case CyWhereAtom(value) => (QueryIR.Atom(value), Nil)
    case CyWhereProp(base, field) =>
      val tmp = QueryIR.Ref("_cy_prop_" + counter().toString)
      (tmp, List(QueryIR.Rel("prop_o", List(QueryIR.Ref(base), QueryIR.Atom(field), tmp))))
  }

  private def mkConj(goals: List[Goal]): Goal = goals match {
    case head :: Nil => head
    case many => QueryIR.Conj(many)
  }

  private def lowerCypherWherePredicate(pred: CyWherePred, counter: () => Int): List[Goal] = {
    val (leftExpr, leftGoals) = lowerCypherWhereOperand(pred.left, counter)
    val (rightExpr, rightGoals) = lowerCypherWhereOperand(pred.right, counter)
    val cmp = pred.op match {
      case "=" => QueryIR.Eq(leftExpr, rightExpr)
      case "<>" => QueryIR.Neq(leftExpr, rightExpr)
      case _ => QueryIR.Eq(leftExpr, rightExpr)
    }
    leftGoals ++ rightGoals :+ cmp
  }

  private def lowerCypherWhereExpr(expr: CypherWhereExpr, counter: () => Int): Goal = expr match {
    case pred: CyWherePred => mkConj(lowerCypherWherePredicate(pred, counter))
    case CyWhereAnd(parts) => mkConj(parts.map(lowerCypherWhereExpr(_, counter)))
    case CyWhereOr(parts) => QueryIR.Disj(parts.map(lowerCypherWhereExpr(_, counter)))
  }

  private def lowerCypherWhereTop(expr: CypherWhereExpr, counter: () => Int): List[Goal] = expr match {
    case pred: CyWherePred => lowerCypherWherePredicate(pred, counter)
    case CyWhereAnd(parts) => parts.flatMap(lowerCypherWhereTop(_, counter))
    case other => List(lowerCypherWhereExpr(other, counter))
  }

  private def cypherReturn[$: P]: P[List[String]] =
    P("RETURN" ~ ws ~ cypherIdent.!.rep(sep = ws ~ "," ~ ws, min = 1)).map(_.toList)
  private def cypherQuery[$: P]: P[Program] =
    P(
      "MATCH" ~ ws ~
      cypherPattern.rep(sep = ws ~ "," ~ ws, min = 1) ~ ws ~
      cypherWhere.? ~ ws ~
      cypherReturn
    ).map {
      case (patterns, whereOpt, retVars) =>
        val matchGoals = patterns.toList.flatMap(cypherPatternGoals)
        var whereTmpCounter = 0
        def nextWhereTmp(): Int = {
          val current = whereTmpCounter
          whereTmpCounter = whereTmpCounter + 1
          current
        }
        val whereGoals = whereOpt.map(expr => lowerCypherWhereTop(expr, nextWhereTmp)).getOrElse(Nil)
        val (focus, returnGoals) = retVars match {
          case head :: Nil => (head, Nil)
          case many =>
            val synthetic = "_cy_return"
            val bind = QueryIR.Eq(QueryIR.Ref(synthetic), QueryIR.ListExpr(many.map(QueryIR.Ref.apply)))
            (synthetic, List(bind))
        }
        val goals = matchGoals ++ whereGoals ++ returnGoals
        QueryIR.Program(focus = focus, goals = goals, limit = -1, constrained = false)
    }
  private def rootCypher[$: P]: P[Program] = P(ws ~ cypherQuery ~ ws ~ ";".? ~ ws ~ End)

  private def rootOld[$: P]: P[Program] = P(ws ~ runProgram ~ ws ~ End)
  private def rootNew[$: P]: P[Program] = P(declarativeProgram)
  private def rootProlog[$: P]: P[Program] = P(prologProgram)

  private def parseDirected(input: String, mode: String): Either[ParseError, Program] = {
    val trimmed = input.dropWhile(ch => ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
    val headerLineEnd = trimmed.indexOf('\n')
    val body = if (headerLineEnd >= 0) trimmed.substring(headerLineEnd + 1) else ""
    val headerLineOffset = 1 + trimmed.substring(0, math.max(0, headerLineEnd)).count(_ == '\n')

    val parser = mode match {
      case "cypher" => rootCypher(using _)
      case "flix" => rootFlix(using _)
      case "legacy" => rootOld(using _)
      case "declarative" => rootNew(using _)
      case "prolog" => rootProlog(using _)
      case _ => rootOld(using _)
    }

    fastparse.parse(body, parser) match {
      case Parsed.Success(program, _) => Right(program)
      case failure: Parsed.Failure =>
        val (line, col) = indexToLineCol(body, failure.index)
        Left(ParseError(failure.trace().longMsg, line + headerLineOffset, col))
    }
  }

  def parse(input: String): Either[ParseError, Program] = {
    val trimmed = input.dropWhile(ch => ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
    if (trimmed.startsWith("#!cypher")) {
      return parseDirected(input, "cypher")
    }
    if (trimmed.startsWith("#!flix")) {
      return parseDirected(input, "flix")
    }
    if (trimmed.startsWith("#!legacy") || trimmed.startsWith("#!run")) {
      return parseDirected(input, "legacy")
    }
    if (trimmed.startsWith("#!declarative")) {
      return parseDirected(input, "declarative")
    }
    if (trimmed.startsWith("#!prolog")) {
      return parseDirected(input, "prolog")
    }

    fastparse.parse(input, rootOld(using _)) match {
      case Parsed.Success(program, _) => Right(program)
      case oldFailure: Parsed.Failure =>
        fastparse.parse(input, rootNew(using _)) match {
          case Parsed.Success(program, _) => Right(program)
          case newFailure: Parsed.Failure =>
            fastparse.parse(input, rootProlog(using _)) match {
              case Parsed.Success(program, _) => Right(program)
              case prologFailure: Parsed.Failure =>
                fastparse.parse(input, rootFlix(using _)) match {
                  case Parsed.Success(program, _) => Right(program)
                  case flixFailure: Parsed.Failure =>
                    fastparse.parse(input, rootCypher(using _)) match {
                      case Parsed.Success(program, _) => Right(program)
                      case cypherFailure: Parsed.Failure =>
                        val position = cypherFailure.index
                        val (line, col) = indexToLineCol(input, position)
                        Left(ParseError(cypherFailure.trace().longMsg, line, col))
                    }
                }
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
