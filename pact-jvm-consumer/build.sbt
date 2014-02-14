resolvers ++= Seq("typesafe-releases" at "http://repo.typesafe.com/typesafe/releases",
                  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository")

libraryDependencies ++= Seq(
  "org.specs2"        %% "specs2"         % "2.2.3" % "test",
  "org.mockito"       % "mockito-all"     % "1.9.5" % "test",
  "junit"             % "junit"           % "4.11"  % "test"
)
