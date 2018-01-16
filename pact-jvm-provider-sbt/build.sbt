sbtPlugin := true
isSnapshot := true

scalaVersion := "2.12.4"
organization := "au.com.dius"

val currentVersion = "3.5.11"
version := currentVersion

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-provider-scalasupport" % currentVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.mavenLocal

/** Console */
initialCommands in console := "import au.com.dius.pact.provider.sbt._"
