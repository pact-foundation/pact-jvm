
resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.2.1",
  "commons-io"  % "commons-io"     % "2.4",
  "net.databinder" %% "unfiltered-netty-server" % "0.8.2",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "org.json4s" % "json4s-native_2.10"  % "3.2.6",
  "org.json4s" % "json4s-jackson_2.10" % "3.2.6",
  "org.specs2" %% "specs2"         % "2.3.11" % "test",
  "org.mockito" % "mockito-core"   % "1.8.5" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

initialCommands := """
  import au.com.dius.pact.model._;
  import au.com.dius.pact.provider._;
"""
