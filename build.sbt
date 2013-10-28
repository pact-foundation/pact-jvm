name := "pact-runner-jvm"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq("spray repo" at "http://repo.spray.io",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "io.spray" % "spray-client" % "1.2-RC1",
  "io.spray" % "spray-can" % "1.2-RC1",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.play" %% "play-json" % "2.2.0",
  "org.mockito" % "mockito-core" % "1.8.5" % "test",
  "org.specs2" %% "specs2" % "2.2.3" % "test"
)


