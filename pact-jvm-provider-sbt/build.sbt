sbtPlugin := true
isSnapshot := true

libraryDependencies ++= Seq(
  "au.com.dius" %% "pact-jvm-provider" % Common.version
)

resolvers += Resolver.mavenLocal
