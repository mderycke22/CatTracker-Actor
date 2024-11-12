ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

val AkkaVersion = "2.10.0"
val ScalaTestVersion = "3.2.19"

lazy val root = (project in file("."))
  .settings(
    name := "CatTracker-Backend"
  )

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "8.0.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "8.0.0",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.scalactic" %% "scalactic" % ScalaTestVersion,
  "com.typesafe.akka" %% "akka-http" % "10.7.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.7.0",
  "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
  "org.postgresql" % "postgresql" % "42.7.3"
)