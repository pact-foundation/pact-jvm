name := "pact-model-jvm"

organization := "com.dius"

scalaVersion := "2.10.3"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "io.spray"           % "spray-http"     % "1.2.0",
  // Play ships 2.2.0, Spray 1.2.0 needs 2.2.3 these lines should overwrite it
    "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.json4s"        %% "json4s-native"  % "3.2.6",
  "org.json4s"        %% "json4s-jackson" % "3.2.6",
  "org.scalaz"        %% "scalaz-core"    % "7.0.2"
)
