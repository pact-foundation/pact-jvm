package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.support.isNotEmpty
import com.github.ajalt.mordant.TermColors
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

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

  @Parameter(property = "toTag", defaultValue = "")
  private var to: String? = ""

  @Parameter(property = "retriesWhenUnknown", defaultValue = "0")
  private var retriesWhenUnknown: Int? = 0

  @Parameter(property = "retryInterval", defaultValue = "10")
  private var retryInterval: Int? = 10

  override fun execute() {
    val t = TermColors()

    if (pactBrokerUrl.isNullOrEmpty() && brokerClient == null) {
      throw MojoExecutionException("pactBrokerUrl is required")
    }

    if (brokerClient == null) {
      brokerClient = PactBrokerClient(pactBrokerUrl!!, brokerClientOptions(), brokerClientConfig())
    }

    if (pacticipant.isNullOrEmpty()) {
      throw MojoExecutionException("The can-i-deploy task requires -Dpacticipant=...", null)
    }

    val latest = setupLatestParam()
    if ((latest !is Latest.UseLatest || !latest.latest) && pacticipantVersion.isNullOrEmpty()) {
      throw MojoExecutionException("The can-i-deploy task requires -DpacticipantVersion=... or -Dlatest=true", null)
    }

    val result = brokerClient!!.canIDeploy(pacticipant!!, pacticipantVersion.orEmpty(), latest, to)
    if (result.ok) {
      println("Computer says yes \\o/ ${result.message}\n\n${t.green(result.reason)}")
    } else {
      println("Computer says no ¯\\_(ツ)_/¯ ${result.message}\n\n${t.red(result.reason)}")
    }

    if (!result.ok) {
      throw MojoExecutionException("Can you deploy? Computer says no ¯\\_(ツ)_/¯ ${result.message}", null)
    }
  }

  private fun brokerClientConfig(): PactBrokerClientConfig {
    return PactBrokerClientConfig(retriesWhenUnknown ?: 0, retryInterval ?: 10)
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
