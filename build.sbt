name := "pact-author-jvm"

organization := "com.dius"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers ++= Seq("typesafe-releases" at "http://repo.typesafe.com/typesafe/releases",
				  "spray repo" at "http://repo.spray.io",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "com.dius"          %% "pact-model-jvm" % "1.0-SNAPSHOT",
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "io.spray"          %  "spray-can"      % "1.2-RC1",
  "com.typesafe.akka" %% "akka-actor"     % "2.2.3",
  "com.typesafe.play" %% "play-json"      % "2.2.0"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
