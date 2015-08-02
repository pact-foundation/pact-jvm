sbtPlugin := true

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-consumer" % Common.version,
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.3.2.201404171909-r",
  "org.specs2" %% "specs2" % "2.3.11" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2")

scalaVersion := "2.10.5"

resolvers += Resolver.mavenLocal
