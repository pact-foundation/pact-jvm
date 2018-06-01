package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.fusesource.jansi.AnsiConsole
import java.io.File

/**
 * Task to push pact files to a pact broker
 */
@Mojo(name = "publish")
open class PactPublishMojo : AbstractMojo() {

    @Parameter(required = true, defaultValue = "\${project.version}")
    private lateinit var projectVersion: String

    @Parameter(defaultValue = "false")
    private var trimSnapshot: Boolean = false

    @Parameter(defaultValue = "\${project.build.directory}/pacts")
    private lateinit var pactDirectory: String

    @Parameter(required = true)
    private lateinit var pactBrokerUrl: String

    @Parameter
    private var pactBrokerServerId: String? = null

    @Parameter
    private var pactBrokerUsername: String? = null

    @Parameter
    private var pactBrokerPassword: String? = null

    @Parameter(defaultValue = "basic")
    private var pactBrokerAuthenticationScheme: String? = null

    private var brokerClient: PactBrokerClient? = null

    @Parameter(defaultValue = "\${settings}", readonly = true)
    private lateinit var settings: Settings

    @Component
    private lateinit var decrypter: SettingsDecrypter

    @Parameter
    private var tags: MutableList<String> = mutableListOf()

    override fun execute() {
      AnsiConsole.systemInstall()

      if (trimSnapshot && projectVersion.endsWith("-SNAPSHOT")) {
          projectVersion = projectVersion.substring(0, projectVersion.length - 9)
      }

      if (brokerClient == null) {
        val options = mutableMapOf<String, Any>()
        if (!pactBrokerUsername.isNullOrEmpty()) {
          options["authentication"] = listOf(pactBrokerAuthenticationScheme ?: "basic", pactBrokerUsername,
            pactBrokerPassword)
        } else if (!pactBrokerServerId.isNullOrEmpty()) {
          val serverDetails = settings.getServer(pactBrokerServerId)
          val request = DefaultSettingsDecryptionRequest(serverDetails)
          val result = decrypter.decrypt(request)
          options["authentication"] = listOf(pactBrokerAuthenticationScheme ?: "basic", serverDetails.username,
            result.server.password)
        }
        brokerClient = PactBrokerClient(pactBrokerUrl, options)
      }

      val pactDirectory = File(pactDirectory)

      if (!pactDirectory.exists()) {
        println("Pact directory $pactDirectory does not exist, skipping uploading of pacts")
      } else {

        try {
          var anyFailed = false
          pactDirectory.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { pactFile ->
            print("Publishing ${pactFile.name} ... ")
            val result = brokerClient!!.uploadPactFile(pactFile, projectVersion, tags).toString()
            println(result)
            if (!anyFailed && result.startsWith("FAILED!")) {
              anyFailed = true
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
}
