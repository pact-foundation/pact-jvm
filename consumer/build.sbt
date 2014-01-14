name := "pact-consumer-jvm"

organization := "com.dius"

scalaVersion := "2.10.3"

resolvers ++= Seq("typesafe-releases" at "http://repo.typesafe.com/typesafe/releases",
				  "spray repo" at "http://repo.spray.io",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "org.mockito"       % "mockito-all"     % "1.9.5" % "test",
  "junit"             % "junit"           % "4.11"  % "test",
  "io.spray"          %  "spray-can"      % "1.2.0",
  // Play ships 2.2.0, Spray 1.2.0 needs 2.2.3 these lines should overwrite it
      "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.2.3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.2.3"
)
