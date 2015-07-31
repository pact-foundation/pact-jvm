libraryDependencies ++= Seq(
  "org.specs2"   %% "specs2"         % "2.3.11" % "test",
  "junit"        %  "junit"          % "4.11" % "test",
  "net.databinder" %% "unfiltered-netty-server" % "0.8.2",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "org.json4s"   %% "json4s-native"  % "3.2.11",
  "org.json4s"   %% "json4s-jackson" % "3.2.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.json" % "json" % "20140107",
  "org.apache.httpcomponents" % "httpclient" % "4.4.1"
)

initialCommands := """
  import au.com.dius.pact.model._;
"""
