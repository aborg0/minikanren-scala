package info.hircus.kanren.dslir

import info.hircus.kanren.MiniKanren.{Goal => MKGoal}

/**
  * Runtime registry for external DSL built-in relations.
  *
  * This allows custom relation handlers to be added (or existing names to be
  * overridden) without changing QueryCompiler.
  */
object PreludeRegistry {

  type BuiltinRelation = List[Any] => MKGoal

  private var customRelations: Map[String, BuiltinRelation] = Map.empty

  def register(name: String, relation: BuiltinRelation): Unit = synchronized {
    customRelations = customRelations.updated(name, relation)
  }

  def unregister(name: String): Unit = synchronized {
    customRelations = customRelations - name
  }

  def clear(): Unit = synchronized {
    customRelations = Map.empty
  }

  def listCustom(): List[String] = synchronized {
    customRelations.keys.toList.sorted
  }

  def resolve(name: String, args: List[Any]): Option[MKGoal] = synchronized {
    customRelations.get(name).map(_(args))
  }
}