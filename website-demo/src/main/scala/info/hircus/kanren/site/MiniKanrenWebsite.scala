package info.hircus.kanren.site

import info.hircus.kanren.MiniKanren
import info.hircus.kanren.MiniKanren.make_var
import info.hircus.kanren.examples.{PalProd, SendMoreMoney}
import info.hircus.kanren.dsl3.Scala3DSL
import info.hircus.kanren.dsl3.Scala3DSL.{all, any, append, contains, list, query, runList, v, ===}
import info.hircus.kanren.lang.MiniKanrenLang
import scala.collection.compat.immutable.LazyList
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("MiniKanrenWebsite")
object MiniKanrenWebsite {

  @JSExport
  def runLanguage(source: String): js.Object =
    MiniKanrenLang.run(source) match {
      case Right(results) => success("Language query succeeded.", results)
      case Left(err) =>
        js.Dynamic.literal(
          ok = false,
          message = err.message,
          line = err.line,
          column = err.column,
          results = js.Array()
        )
    }

  @JSExport
  def runScala3Demo(exampleId: String, payload: String): js.Object = {
    val focus = v("x")

    val results = exampleId match {
      case "equality" =>
        runList(query(focus).withLimit(1).where(focus === parseScalar(payload.trim)))
      case "disjunction" =>
        val values = parseCsv(payload)
        runList(query(focus).where(any(values.map(value => focus === value)*)))
      case "contains" =>
        val config = parseKeyValuePayload(payload)
        val value = parseScalar(config.getOrElse("value", "2"))
        val items = parseCsv(config.getOrElse("list", "1, 2, 3"))
        runList(query(focus).where(all(focus === value, contains(value, list(items*)))) )
      case "append" =>
        val config = parseKeyValuePayload(payload)
        val left = parseCsv(config.getOrElse("left", "1, 2"))
        val right = parseCsv(config.getOrElse("right", "3, 4"))
        runList(query(focus).withLimit(1).where(append(list(left*), list(right*), focus)))
      case "reify_recursive" =>
        val q = v("q")
        val n = parseScalar(payload).asInstanceOf[Int]
        runList(query(q).where(
          if (n >= 2) any(q === 1, q === 2)
          else if (n >= 1) q === 1
          else any() // fail
        ))
      case other =>
        return js.Dynamic.literal(
          ok = false,
          message = s"Unknown Scala 3 demo: $other",
          results = js.Array()
        )
    }

    success(s"Scala 3 demo '$exampleId' executed.", results)
  }

  @JSExport
  def runExample(exampleId: String): js.Object = {
    val q = make_var(Symbol("q"))
    val results = exampleId match {
      case "send-more-money" => MiniKanren.run(1, q)(SendMoreMoney.solve_puzzle(q))
      // Palindrome product is a known heavy query; maprun is substantially faster than run.
      case "palprod" => MiniKanren.maprun(1, q)(PalProd.palprod_o(q))
      case other =>
        return js.Dynamic.literal(
          ok = false,
          message = s"Unknown example: $other",
          results = js.Array()
        )
    }

    success(s"Example '$exampleId' executed.", results)
  }

  @JSExport
  def docsIndex(): js.Array[js.Object] =
    js.Array(
      docEntry("Examples tutorial", "docs/viewer.html?doc=examples/io_livecode_ch_learn_webyrd_webmk.md"),
      docEntry("Scala 3 DSL tour", "docs/viewer.html?doc=scala3dsl/scala3dsl-tour.md"),
      docEntry("Prelude guide", "docs/viewer.html?doc=lang/prelude-guide.md"),
      docEntry("External DSL tour", "docs/viewer.html?doc=lang/external-dsl-tour.md")
    )

  private def success(message: String, results: Seq[Any]): js.Object =
    js.Dynamic.literal(
      ok = true,
      message = message,
      results = results.iterator.map(renderValue).toJSArray
    )

  private def docEntry(title: String, href: String): js.Object =
    js.Dynamic.literal(title = title, href = href)

  private def parseKeyValuePayload(payload: String): Map[String, String] =
    payload.linesIterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap { line =>
        line.split(":", 2) match {
          case Array(key, value) => Some(key.trim.toLowerCase -> value.trim)
          case _ => None
        }
      }
      .toMap

  private def parseCsv(payload: String): List[Any] =
    payload
      .split(",")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(parseScalar)
      .toList

  private def parseScalar(raw: String): Any = {
    val trimmed = raw.trim
    if (trimmed.isEmpty) ""
    else if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) trimmed.substring(1, trimmed.length - 1)
    else trimmed.toIntOption.getOrElse(trimmed)
  }

  private def toIdiomaticList(value: Any): Option[List[Any]] = value match {
    case Nil => Some(Nil)
    case (head, tail) =>
      toIdiomaticList(tail).map(normalizedTail => normalizeValue(head) :: normalizedTail)
    case _ => None
  }

  private def normalizeValue(value: Any): Any =
    toIdiomaticList(value) match {
      case Some(items) => items
      case None =>
        value match {
          case (head, tail) => (normalizeValue(head), normalizeValue(tail))
          case other => other
        }
    }

  private def renderValue(value: Any): String =
    normalizeValue(value) match {
      case items: List[?] => items.map(renderValue).mkString("[", ", ", "]")
      case (head, tail) => s"(${renderValue(head)} . ${renderValue(tail)})"
      case text: String => text
      case other => other.toString
    }
}
