
resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0.RC3", //scalatest is the runtime test library, hoping for simple single thread
  "commons-io"  % "commons-io"     % "2.4",
  "org.specs2" %% "specs2"         % "2.3.1" % "test",
  "org.mockito" % "mockito-core"   % "1.8.5" % "test",
  "com.twitter"%% "finagle-http"   % "6.2.0",
  "org.json4s" %% "json4s-native"  % "3.2.6",
  "org.json4s" %% "json4s-jackson" % "3.2.6"
)


