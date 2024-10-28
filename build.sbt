ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

val AkkaVersion = "2.9.7"
val ScalaTestVersion = "3.2.19"

lazy val root = (project in file("."))
  .settings(
    name := "CatTracker-Backend"
  )

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.scalactic" %% "scalactic" % ScalaTestVersion,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % "test"
)