name := "pact-runner-jvm"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "2.2.3" % "test",
  "com.typesafe.play" %% "play" % "2.2.0",
  "org.mockito" % "mockito-core" % "1.8.5"
)


