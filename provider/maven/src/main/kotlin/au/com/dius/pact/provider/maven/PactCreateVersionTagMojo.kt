package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

/**
 * Task to push new version tags to the Pact Broker
 */
@Mojo(name = "create-version-tag")
open class PactCreateVersionTagMojo : PactBaseMojo() {

  private var brokerClient: PactBrokerClient? = null

  @Parameter(property = "pacticipant")
  private var pacticipant: String? = ""

  @Parameter(property = "pacticipantVersion")
  private var pacticipantVersion: String? = ""

  @Parameter(property = "tag")
  private var tag: String? = ""

  override fun execute() {
    prepare()
    createVersionTag()
  }

  fun prepare() {
    checkMandatoryArguments()
    createBrokerClient()
  }

  private fun checkMandatoryArguments() {
    dealWithNotProvidedPactURL()
    dealWithNotProvidedPacticipant()
    dealWithNotProvidedPacticipantVersion()
    dealWithNotProvidedTag()
  }

  private fun dealWithNotProvidedPactURL() =
      dealWithNotProvidedArgument(pactBrokerUrl, "pactBrokerUrl")

  private fun dealWithNotProvidedArgument(argument: String?, argumentName: String) {
    if (argument.isNullOrEmpty())
      throw MojoExecutionException("$argumentName is required")
  }

  private fun dealWithNotProvidedPacticipant() =
      dealWithNotProvidedArgument(pacticipant, "pacticipant")

  private fun dealWithNotProvidedPacticipantVersion() =
      dealWithNotProvidedArgument(pacticipantVersion, "pacticipantVersion")

  private fun dealWithNotProvidedTag() =
      dealWithNotProvidedArgument(tag, "tag")

  private fun createBrokerClient() {
    if (brokerClient == null)
      brokerClient = PactBrokerClient(pactBrokerUrl!!, brokerClientOptions(), brokerClientConfig())
  }

  private fun createVersionTag() =
      brokerClient!!.createVersionTag(pacticipant!!, pacticipantVersion!!, tag!!)
}
