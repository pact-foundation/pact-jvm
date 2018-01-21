sbtPlugin := true
isSnapshot := true

scalaVersion := "2.12.4"
organization := "au.com.dius"

val currentVersion = "3.5.12"
version := currentVersion

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-provider-scalasupport" % currentVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.mavenLocal

/** Console */
initialCommands in console := "import au.com.dius.pact.provider.sbt._"

useGpg := true

pomIncludeRepository := { _ => false }

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/DiUS/pact-jvm"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/DiUS/pact-jvm"),
    "scm:git@github.com:DiUS/pact-jvm.git"
  )
)

developers := List(
  Developer(
    id    = "rholshausen",
    name  = "Ronald Holshausen",
    email = "rholshausen@dius.com.au",
    url = url("https://github.com/DiUS/pact-jvm")
  ),
  Developer(
    id    = "thetrav",
    name  = "Travis Dixon",
    email = "the.trav@gmail.com",
    url = url("https://github.com/DiUS/pact-jvm")
  )
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false
