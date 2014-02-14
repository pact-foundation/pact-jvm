import au.com.dius.pact.provider.Main
import sbt._

object PactJvmPlugin extends Plugin {
  val verifyPacts = taskKey[Unit]("Verify this provider adheres to the pacts provided by its consumers.")

  val pactConfig = settingKey[File]("json file containing configuration for the provider test server.")
  val pactRoot = settingKey[File]("root folder for pact files, all .json files in root and sub folders are assumed to be pacts.")

  val pactSettings = Seq(
    pactConfig := file("pact-config.json"),
    pactRoot := file("pacts"),
    verifyPacts := {
      implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
      import Main._
      runPacts(loadFiles(pactRoot.value, pactConfig.value))
    }
  )
}