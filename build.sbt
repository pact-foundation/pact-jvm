name := "pact-model-jvm"

organization := "com.dius"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "spray repo" at "http://repo.spray.io"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "io.spray"           % "spray-http"     % "1.2-RC1",
  "com.typesafe.akka" %% "akka-actor"     % "2.2.3"
)