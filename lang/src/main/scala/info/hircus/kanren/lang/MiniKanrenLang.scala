package info.hircus.kanren.lang

import info.hircus.kanren.dslir.QueryCompiler

object MiniKanrenLang {

  def run(source: String): Either[MiniKanrenLangParser.ParseError, List[Any]] = {
    MiniKanrenLangParser.parse(source).map(QueryCompiler.execute)
  }
}
