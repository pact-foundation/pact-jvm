sbtPlugin := true
isSnapshot := true

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-provider" % Common.version,
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

resolvers += Resolver.mavenLocal
