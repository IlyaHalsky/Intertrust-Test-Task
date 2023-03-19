name := "backend-test-task"

version := "1.0"

scalaVersion := "2.13.10"

resourceDirectory := baseDirectory.value / "src/main/resources"

lazy val akkaVersion = "2.8.0"
libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.7.2",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)
