import sbt._
import sbt.Keys._
import com.typesafe.sbt.pgp.PgpKeys._

object BuildSettings {
	val publishSettings = Seq(
		version := "2.0-RC6",
		organization := "au.com.dius",
    scalaVersion := "2.10.3",

	    publishMavenStyle := true,
	    // when playing around with a local install of nexus use this:
//        credentials += Credentials("Sonatype Nexus Repository Manager", "localhost", "deployment", "admin123"),
//	    publishTo := Some("releases" at "http://localhost:8081/nexus/content/repositories/releases/"),
	    // when sonatype comes online use this:
	    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
	    publishTo := {
	       val nexus = "https://oss.sonatype.org/"
	       if (version.value.trim.endsWith("SNAPSHOT"))
	         Some("snapshots" at nexus + "content/repositories/snapshots")
	       else
	         Some("releases" at nexus + "service/local/staging/deploy/maven2")
	    },
//      publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
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
          <developer>
            <id>rholshausen</id>
            <name>Ronald Holshausen</name>
            <email>rholshausen@dius.com.au</email>
          </developer>
          <developer>
            <id>kenbot</id>
            <name>Ken Scambler</name>
            <email>ken.scambler@gmail.com</email>
          </developer>
	      </developers>
  	)

	val testSettings = Seq (
		testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
		testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")
	)

  val javacSettings = Seq (
    scalacOptions += "-target:jvm-1.6"
  )

	val commonSettings = Defaults.defaultSettings ++ publishSettings ++ testSettings ++ javacSettings
	val skipPublish = Seq(
		publish := { },
		publishLocal := { },
		publishSigned := { }
	)
	val skipTest = Seq(
		test:= {},
		testOnly := {}
	)
}

object RootBuild extends Build {

	import BuildSettings._
	lazy val pact = Project( 
		id = "pact-jvm",
		base = file("."),
		settings = commonSettings ++ skipPublish ++ skipTest)
		.aggregate(model, consumer, provider, plugin, consumerSbt, server, consumerSpecs2, providerSpecs2, junit, pactSpecification)

	def p(id: String, settings: Seq[Def.Setting[_]] = commonSettings) = Project(
		id = id, 
		base = file(id), 
		settings = settings :+ (name := id))

	lazy val model = p("pact-jvm-model")

	lazy val consumer = p("pact-jvm-consumer").dependsOn(model)

  lazy val consumerSpecs2 = p("pact-jvm-consumer-specs2").dependsOn(consumer)

  lazy val junit = p("pact-jvm-consumer-junit").dependsOn(consumer)

	lazy val provider = p("pact-jvm-provider").dependsOn(model)

  lazy val providerSpecs2 = p("pact-jvm-provider-specs2").dependsOn(model)

  lazy val plugin = p("pact-jvm-provider-sbt").dependsOn(provider)

  lazy val consumerSbt = p("pact-jvm-consumer-sbt")

  lazy val server = p("pact-jvm-server").dependsOn(model).dependsOn(consumer)

  lazy val pactSpecification = p("pact-specification-test", commonSettings ++ skipPublish).dependsOn(model)

}
