import sbt._
import sbt.Keys._


object RootBuild extends Build {
	lazy val pact = Project( 
		id = "pact-jvm",
		base = file(".")
	).aggregate(model, provider)

	lazy val model = Project(
		id = "pact-model-jvm",
		base = file("pact-model-jvm"))

	// lazy val consumer = Project( 
	// 	id = "pact-consumer-jvm",
	// 	base = file("pact-consumer-jvm"))

	lazy val provider = Project( 
		id = "pact-provider-jvm",
		base = file("pact-provider-jvm"))
}