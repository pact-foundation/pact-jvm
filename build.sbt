name := "pact-model-jvm"

organization := "com.dius"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "spray repo" at "http://repo.spray.io"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "io.spray"           % "spray-http"     % "1.2-RC1",
  "com.typesafe.akka" %% "akka-actor"     % "2.2.3",
  //NOTE: these two are published locally while waiting for my pull request to be released
  "org.json4s"        %% "json4s-native"  % "3.2.7-SNAPSHOT",
  "org.json4s"        %% "json4s-jackson" % "3.2.7-SNAPSHOT",
  "org.scalaz"        %% "scalaz-core"    % "7.0.2"
)