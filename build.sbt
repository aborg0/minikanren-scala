import org.scalajs.linker.interface.ModuleKind
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

enablePlugins(ScalaJSPlugin, LaikaPlugin)

name := "Scala miniKanren root project"
ThisBuild / crossScalaVersions := Seq("2.12.21", "2.13.18", "3.8.3")
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / publishTo := {
  val owner = sys.env.getOrElse("GITHUB_REPOSITORY_OWNER", "michel-slm")
  val repo = sys.env.get("GITHUB_REPOSITORY").flatMap(_.split("/").lastOption).getOrElse("minikanren-scala")
  Some("GitHub Packages" at s"https://maven.pkg.github.com/$owner/$repo")
}
ThisBuild / credentials ++= {
  for {
    token <- sys.env.get("GITHUB_TOKEN")
    user <- sys.env.get("GITHUB_ACTOR").orElse(sys.env.get("GITHUB_REPOSITORY_OWNER"))
  } yield Credentials("GitHub Package Registry", "maven.pkg.github.com", user, token)
}.toSeq

// This is an application with a main method
scalaJSUseMainModuleInitializer := true

lazy val root = project.in(file(".")).
  aggregate(
    miniKanrenJS,
    miniKanrenJVM,
    miniKanrenExamplesJS,
    miniKanrenExamplesJVM,
    miniKanrenScala3DSL,
    miniKanrenScala3DSLJS,
    miniKanrenLang,
    miniKanrenLangJS,
    miniKanrenWebsite
  ).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val commonSettings = Seq(
  version := "0.1-SNAPSHOT",

  scalacOptions ++= Seq(
    //"-target:jvm-1.8", // not applicable in 2.10
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-feature"
    //"-language:implicitConversions",
    //"-language:postfixOps",
    //"-language:higherKinds",
    //"-language:reflectiveCalls",
    //"-Xfatal-warnings"
  ) ++ (
    if (scalaVersion.value.startsWith("3")) Seq() else Seq(
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    )
  ),
  Test / scalacOptions -= "-Ywarn-numeric-widen"

)


lazy val miniKanren = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  settings(
    commonSettings,
    name := "Scala miniKanren",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.18.1" % Test,
    libraryDependencies += "org.scala-lang.modules" %%% "scala-collection-compat" % "2.13.0",
    libraryDependencies += ("org.scala-js" %% "scalajs-stubs" % "1.1.0" /* % Provided */).cross(CrossVersion.for3Use2_13)
  ).jvmSettings(
    coverageEnabled := true,
    Test / fork := true,
    Test / javaOptions += "-Xss11m",
    Test / javaOptions += "-Xmx3g"
  ).jsSettings(
    coverageEnabled := false
  )

lazy val miniKanrenExamples = crossProject(JSPlatform, JVMPlatform).in(file(".") / "examples").
  dependsOn(miniKanren).
  settings(
    commonSettings,
    name := "Scala miniKanren examples"

  ).jvmSettings(
  coverageEnabled := false,
  mdocIn := (Compile / sourceDirectory).value / "mdoc",
  mdocOut := target.value / "mdoc",
  initialCommands := """
                       |import info.hircus.kanren.MiniKanren._
                       |import info.hircus.kanren.Prelude._
                       |import info.hircus.kanren.MKMath._
                       |import info.hircus.kanren.examples.PalProd._
                       |import info.hircus.kanren.examples.SendMoreMoney._
                       |
                       |var x = make_var(Symbol("x"))
                       |var y = make_var(Symbol("y"))
                       |var z = make_var(Symbol("z"))
                       |
                       |def time(block: => Any) = {
                       |  val start = System currentTimeMillis ()
                       |  val res   = block
                       |  val stop  = System currentTimeMillis ()
                       |  ((stop-start), res)
                       |}
                       |
                       |def ntimes(n: Int, block: => Any) = {
                       |  // folding a list of Longs is cumbersome
                       |  def adder(x:Long,y:Long) = x+y
                       |  val zero : Long = 0
                       |
                       |  // compute only once!
                       |  val res = (for (i <- 0 until n) yield (time(block)._1)).toList
                       |  println("Elapsed times: " + res)
                       |  println("Avg: " + (res.foldLeft(zero)(adder) / n))
                       |}
                       |""".stripMargin
).jsSettings(
  coverageEnabled := false
).enablePlugins(MdocPlugin)

lazy val miniKanrenJVM = miniKanren.jvm

lazy val miniKanrenJS = miniKanren.js

lazy val miniKanrenExamplesJVM = miniKanrenExamples.jvm

lazy val miniKanrenExamplesJS = miniKanrenExamples.js

lazy val miniKanrenScala3DSLCross = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("scala3dsl")).
  dependsOn(miniKanren).
  settings(
    commonSettings,
    name := "Scala miniKanren Scala 3 DSL",
    scalaVersion := "3.8.3",
    crossScalaVersions := Seq("3.8.3"),
    conflictWarning := ConflictWarning.disable,
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.18.1" % Test
  )

lazy val miniKanrenScala3DSL = miniKanrenScala3DSLCross.jvm.
  enablePlugins(MdocPlugin).
  settings(
    mdocIn := baseDirectory.value.getParentFile / "src" / "main" / "mdoc",
    mdocOut := target.value / "mdoc",
    initialCommands := """
                         |import info.hircus.kanren.dsl3.Scala3DSL.*
                         |
                         |val x = v("x")
                         |val y = v("y")
                         |val z = v("z")
                         |""".stripMargin
  )

lazy val miniKanrenScala3DSLJS = miniKanrenScala3DSLCross.js

lazy val miniKanrenLangCross = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("lang")).
  dependsOn(miniKanren).
  settings(
    commonSettings,
    name := "Scala miniKanren language",
    scalaVersion := "3.8.3",
    crossScalaVersions := Seq("3.8.3"),
    conflictWarning := ConflictWarning.disable,
    libraryDependencies += "com.lihaoyi" %%% "fastparse" % "3.1.1",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.18.1" % Test
  )

lazy val miniKanrenLang = miniKanrenLangCross.jvm.
  enablePlugins(MdocPlugin).
  settings(
    mdocIn := baseDirectory.value.getParentFile / "src" / "main" / "mdoc",
    mdocOut := target.value / "mdoc",
    initialCommands := """
                         |import info.hircus.kanren.lang.MiniKanrenLang
                         |""".stripMargin
  )

lazy val miniKanrenLangJS = miniKanrenLangCross.js

lazy val miniKanrenWebsite = project.in(file("website-demo")).
  enablePlugins(ScalaJSPlugin).
  dependsOn(miniKanrenExamplesJS, miniKanrenScala3DSLJS, miniKanrenLangJS).
  settings(
    commonSettings,
    name := "Scala miniKanren website demo",
    scalaVersion := "3.8.3",
    crossScalaVersions := Seq("3.8.3"),
    conflictWarning := ConflictWarning.disable,
    coverageEnabled := false,
    scalaJSUseMainModuleInitializer := false,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.NoModule))
  )