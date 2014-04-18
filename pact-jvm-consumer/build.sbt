resolvers ++= Seq("typesafe-releases" at "http://repo.typesafe.com/typesafe/releases",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "junit"        %  "junit"          % "4.11",
  "net.databinder" %% "unfiltered-netty-server" % "0.7.1",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "org.clapper"  %% "avsl" % "1.0.1",
  "org.json4s"   %% "json4s-native"  % "3.2.6",
  "org.json4s"   %% "json4s-jackson" % "3.2.6",
  "org.specs2"   %% "specs2"         % "2.2.3" % "test",
  "org.mockito"  %  "mockito-all"    % "1.9.5" % "test"
)
