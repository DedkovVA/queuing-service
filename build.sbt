name := "queuing-service"

version := "0.1"

val _scalaVersion = "2.12.4"

val akkaVersion = "2.5.11"
val akkaHttpVersion = "10.1.1"

resolvers in ThisBuild += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.bintrayRepo("hseeberger", "maven")

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

val httpDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.20.1"
)

val loggingDependencies = Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.11",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
)

val queuingService = (Project("queuing-service", file("."))
  settings(
    mainClass := Some("com.github.dedkovva.qs.Boot"),
    scalaVersion := _scalaVersion,
    scalaVersion in ThisBuild := _scalaVersion,
    libraryDependencies ++= (akkaDependencies ++ httpDependencies ++ loggingDependencies ++ testDependencies)
  )
)