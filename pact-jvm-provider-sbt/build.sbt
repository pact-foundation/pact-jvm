sbtPlugin := true
isSnapshot := true

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-provider" % Common.version,
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.mavenLocal

/** Console */
initialCommands in console := "import au.com.dius.pact.provider.sbt._"
