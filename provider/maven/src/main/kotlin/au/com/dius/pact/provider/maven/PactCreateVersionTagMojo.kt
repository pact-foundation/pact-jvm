package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.isNotEmpty
import com.github.ajalt.mordant.TermColors
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

/**
 * Task to push new version tags to the Pact Broker
 */
@Mojo(name = "create-version-tag")
open class PactCreateVersionTagMojo : PactBaseMojo() {

  @Parameter(property = "pacticipant")
  private var pacticipant: String? = ""

  @Parameter(property = "pacticipantVersion")
  private var pacticipantVersion: String? = ""

  override fun execute() {
    TODO("Not yet implemented")
  }

}
