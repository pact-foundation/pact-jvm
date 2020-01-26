package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.isNotEmpty
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

/**
 * Task to push pact files to a pact broker
 */
@Mojo(name = "can-i-deploy")
open class PactCanIDeployMojo : PactBaseMojo() {

  private var brokerClient: PactBrokerClient? = null

  @Parameter(property = "pacticipant")
  private var pacticipant: String? = ""

  @Parameter(property = "pacticipantVersion")
  private var pacticipantVersion: String? = ""

  @Parameter(property = "latest", defaultValue = "")
  private var latest: String? = ""

  @Parameter(property = "to", defaultValue = "")
  private var to: String? = ""

  override fun execute() {
    try {
      AnsiConsole.systemInstall()

      if (pactBrokerUrl.isNullOrEmpty() && brokerClient == null) {
        throw MojoExecutionException("pactBrokerUrl is required")
      }

      if (brokerClient == null) {
        brokerClient = PactBrokerClient(pactBrokerUrl!!, brokerClientOptions())
      }

      if (pacticipant.isNullOrEmpty()) {
        throw MojoExecutionException("The can-i-deploy task requires -Dpacticipant=...", null)
      }
      if (pacticipantVersion.isNullOrEmpty()) {
        throw MojoExecutionException("The can-i-deploy task requires -DpacticipantVersion=...", null)
      }

      val latest = setupLatestParam()
      val result = brokerClient!!.canIDeploy(pacticipant!!, pacticipantVersion!!, latest, to)
      if (result.ok) {
        AnsiConsole.out().println(Ansi.ansi().a("Computer says yes \\o/ ").a(result.message).a("\n\n")
          .fg(Ansi.Color.GREEN).a(result.reason).reset())
      } else {
        AnsiConsole.out().println(Ansi.ansi().a("Computer says no ¯\\_(ツ)_/¯ ").a(result.message).a("\n\n")
          .fg(Ansi.Color.RED).a(result.reason).reset())
      }

      if (!result.ok) {
        throw MojoExecutionException("Can you deploy? Computer says no ¯\\_(ツ)_/¯ ${result.message}", null)
      }
    } finally {
      AnsiConsole.systemUninstall()
    }
  }

  private fun setupLatestParam(): Latest {
    var latest: Latest = Latest.UseLatest(false)
    if (this.latest.isNotEmpty()) {
      latest = when (this.latest) {
        "true" -> {
          Latest.UseLatest(true)
        }
        "false" -> {
          Latest.UseLatest(false)
        }
        else -> {
          Latest.UseLatestTag(this.latest!!)
        }
      }
    }
    return latest
  }
}
