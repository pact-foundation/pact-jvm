name := "pact-author-jvm"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"    % "2.2.3" % "test",
  "com.typesafe.play" %% "play-json" % "2.2.0"
)
