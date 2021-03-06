enablePlugins(ScalaJSPlugin)

name := "Scala miniKanren root project"
crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.2")
scalaVersion in ThisBuild := "2.12.4" // or any other Scala version >= 2.10.2 for Scala.js

// This is an application with a main method
scalaJSUseMainModuleInitializer := true

lazy val root = project.in(file(".")).
  aggregate(miniKanrenJS, miniKanrenJVM, miniKanrenExamplesJS, miniKanrenExamplesJVM).
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
    "-feature",
    //"-language:implicitConversions",
    //"-language:postfixOps",
    //"-language:higherKinds",
    //"-language:reflectiveCalls",
    "-Xlint",
    //"-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    //"-Ywarn-unused", // not applicable in 2.10
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  ),
  scalacOptions in Test -= "-Ywarn-numeric-widen"

)


lazy val miniKanren = crossProject.in(file(".")).
  settings(
    commonSettings,
    name := "Scala miniKanren",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test",
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  ).jvmSettings(
    coverageEnabled := true,
    fork in Test := true,
    javaOptions in Test += "-Xss11m",
    javaOptions in Test += "-Xmx3g",
    scalacOptions += "-Xfatal-warnings"
  ).jsSettings(
    coverageEnabled := false
  )

lazy val miniKanrenExamples = crossProject.in(file(".") / "examples").
  dependsOn(miniKanren).
  settings(
    commonSettings,
    name := "Scala miniKanren examples"

  ).jvmSettings(
  coverageEnabled := false,
  initialCommands := """
                       |import info.hircus.kanren.MiniKanren._
                       |import info.hircus.kanren.Prelude._
                       |import info.hircus.kanren.MKMath._
                       |import info.hircus.kanren.examples.PalProd._
                       |import info.hircus.kanren.examples.SendMoreMoney._
                       |
                       |var x = make_var('x)
                       |var y = make_var('y)
                       |var z = make_var('z)
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
).enablePlugins(TutPlugin)

lazy val miniKanrenJVM = miniKanren.jvm

lazy val miniKanrenJS = miniKanren.js

lazy val miniKanrenExamplesJVM = miniKanrenExamples.jvm

lazy val miniKanrenExamplesJS = miniKanrenExamples.js

LaikaPlugin.defaults

inConfig(LaikaKeys.Laika)(Seq(
//  sourceDirectories := Seq(baseDirectory.value / "docs"),
  LaikaKeys.encoding := "UTF-8"
))