name := "pact-provider-jvm"

organization := "com.dius"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq("spray repo" at "http://repo.spray.io",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "com.dius" %% "pact-model-jvm" % "1.0-SNAPSHOT",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC3", //scalatest is the runtime test library, hoping for simple single thread
  "commons-io" % "commons-io" % "2.4",
  "io.spray" % "spray-client" % "1.2-RC1",
  "io.spray" % "spray-can" % "1.2-RC1",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.specs2" %% "specs2" % "2.3.1" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test"
)


