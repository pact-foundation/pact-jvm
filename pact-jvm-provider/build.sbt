
resolvers ++= Seq("spray repo" at "http://repo.spray.io",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0.RC3", //scalatest is the runtime test library, hoping for simple single thread
  "commons-io" % "commons-io" % "2.4",
  "io.spray" % "spray-client" % "1.2.0",
  "io.spray" % "spray-can" % "1.2.0",
  // Play ships 2.2.0, Spray 1.2.0 needs 2.2.3 these lines should overwrite it
      "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.2.3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.specs2" %% "specs2" % "2.3.1" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test"
)


