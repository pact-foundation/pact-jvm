package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.broker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.fusesource.jansi.AnsiConsole
import java.io.File

/**
 * Task to push pact files to a pact broker
 */
@Mojo(name = "publish")
open class PactPublishMojo : PactBaseMojo() {

    @Parameter(required = true, defaultValue = "\${project.version}")
    private lateinit var projectVersion: String

    @Parameter(defaultValue = "false")
    private var trimSnapshot: Boolean = false

    @Parameter(defaultValue = "\${project.build.directory}/pacts")
    private lateinit var pactDirectory: String

    private var brokerClient: PactBrokerClient? = null

    @Parameter
    private var tags: MutableList<String> = mutableListOf()

    @Parameter
    private var excludes: MutableList<String> = mutableListOf()

    override fun execute() {
      AnsiConsole.systemInstall()

      if (pactBrokerUrl.isNullOrEmpty() && brokerClient == null) {
        throw MojoExecutionException("pactBrokerUrl is required")
      }

      val snapShotDefinitionString = "-SNAPSHOT"
      val emptyString = ""
      if (trimSnapshot && projectVersion.contains(snapShotDefinitionString)) {
          projectVersion = projectVersion.replaceFirst(snapShotDefinitionString, emptyString)
      }

      if (brokerClient == null) {
        brokerClient = PactBrokerClient(pactBrokerUrl!!, brokerClientOptions())
      }

      val pactDirectory = File(pactDirectory)

      if (!pactDirectory.exists()) {
        println("Pact directory $pactDirectory does not exist, skipping uploading of pacts")
      } else {
        val excludedList = this.excludes.map { Regex(it) }
        try {
          var anyFailed = false
          pactDirectory.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { pactFile ->
            if (pactFileIsExcluded(excludedList, pactFile)) {
              println("Not publishing '${pactFile.name}' as it matches an item in the excluded list")
            } else {
              print("Publishing '${pactFile.name}' ... ")
              val result = brokerClient!!.uploadPactFile(pactFile, projectVersion, tags).toString()
              println(result)
              if (!anyFailed && result.startsWith("FAILED!")) {
                anyFailed = true
              }
            }
          }

          if (anyFailed) {
            throw MojoExecutionException("One or more of the pact files were rejected by the pact broker")
          }
        } finally {
          AnsiConsole.systemUninstall()
        }
      }
    }

  private fun pactFileIsExcluded(exclusions: List<Regex>, pactFile: File) =
    exclusions.any { it.matches(pactFile.nameWithoutExtension) }
}
