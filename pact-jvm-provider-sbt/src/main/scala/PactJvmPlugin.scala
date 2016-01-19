import au.com.dius.pact.provider.sbtsupport.Main
import sbt.Keys.TaskStreams
import sbt._

object PactJvmPlugin extends Plugin {
  val verifyPacts = taskKey[Unit]("**DEPRECATED** Verify this provider adheres to the pacts provided by its consumers.")

  val pactConfig = settingKey[File]("json file containing configuration for the provider test server.")
  val pactRoot = settingKey[File]("root folder for pact files, all .json files in root and sub folders are assumed to be pacts.")

  val pactSettings = Seq(
    pactConfig := file("pact-config.json"),
    pactRoot := file("pacts"),
    verifyPacts := {
      implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
      val s: TaskStreams = Keys.streams.value
      s.log.warn("=== WARNING: verifyPacts is deprecated and is being replaced with an updated task (pactVerify). ===")
      import Main._
      runPacts(loadFiles(pactRoot.value, pactConfig.value))
    }
  )
}
