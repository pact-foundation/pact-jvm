import sbt._
import sbt.Keys._


object BuildSettings {
	val publishSettings = Seq(
		version := "1.0",
		organization := "com.dius",
	    publishMavenStyle := true,
	    // when playing around with a local install of nexus use this:
        credentials += Credentials("Sonatype Nexus Repository Manager", "localhost", "deployment", "admin123"),
	    publishTo := Some("releases" at "http://localhost:8081/nexus/content/repositories/releases/"),
	    // when sonatype comes online use this:
	    // credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
	    // publishTo := {
	    //   // val nexus = "https://oss.sonatype.org/"
	    //   val nexus = "http://localhost:8081/nexus/"
	    //   if (version.value.trim.endsWith("SNAPSHOT"))
	    //     Some("snapshots" at nexus + "content/repositories/snapshots")
	    //   else
	    //     Some("releases" at nexus + "service/local/staging/deploy/maven2")
	    // },
	    pomExtra :=
	      <url>https://github.com/DiUS/pact-jvm</url>
	      <licenses>
	        <license>
	          <name>Apache 2</name>
	          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
	          <distribution>repo</distribution>
	        </license>
	      </licenses>
	      <scm>
	        <url>https://github.com/DiUS/pact-jvm</url>
	        <connection>https://github.com/DiUS/pact-jvm.git</connection>
	      </scm>
	      <developers>
	        <developer>
	          <id>thetrav</id>
	          <name>Travis Dixon</name>
	          <email>the.trav@gmail.com</email>
	        </developer>
	      </developers>
  	)

	val commonSettings = Defaults.defaultSettings ++ publishSettings
	val skipPublish = Seq(
		publish := { },
		publishLocal := { }//,
		// publishSigned := { }
	)
}

object RootBuild extends Build {
	import BuildSettings._
	lazy val pact = Project( 
		id = "pact-jvm",
		base = file("."),
		settings = commonSettings ++ skipPublish)
		.aggregate(model, consumer, provider)

	lazy val model = Project(
		id = "pact-model-jvm",
		base = file("model"),
		settings = commonSettings)

	lazy val consumer = Project( 
		id = "pact-consumer-jvm",
		base = file("consumer"),
		settings = commonSettings)

	lazy val provider = Project( 
		id = "pact-provider-jvm",
		base = file("provider"),
		settings = commonSettings)
}