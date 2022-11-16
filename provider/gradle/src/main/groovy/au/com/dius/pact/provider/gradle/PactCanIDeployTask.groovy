package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import com.github.ajalt.mordant.TermColors
import org.gradle.api.GradleScriptException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task to verify the deployment state using a pact broker
 */
@SuppressWarnings(['Println', 'DuplicateStringLiteral'])
abstract class PactCanIDeployTask extends PactCanIDeployBaseTask {

  static final String PACTICIPANT = 'pacticipant'
  static final String PACTICIPANT_VERSION = 'pacticipantVersion'
  static final String TO = 'toTag'
  static final String LATEST = 'latest'

  @Internal
  abstract PactBrokerClient brokerClient

  @Input
  @Optional
  abstract Property<Broker> getBroker()

  @Input
  @Optional
  abstract Property<Object> getPacticipant()

  @Input
  @Optional
  abstract Property<Object> getPacticipantVersion()

  @Input
  @Optional
  abstract Property<Object> getToProp()

  @Input
  @Optional
  abstract Property<Object> getLatestProp()

  @TaskAction
  void canIDeploy() {
    if (!broker.present) {
      throw new GradleScriptException('You must add a pact broker configuration to your build before you can ' +
        'use the CanIDeploy task', null)
    }

    if (brokerClient == null) {
      Broker config = broker.get()
      brokerClient = setupBrokerClient(config)
    }
    if (!pacticipant.present) {
      throw new GradleScriptException('The CanIDeploy task requires -Ppacticipant=...', null)
    }
    String pacticipant = pacticipant.get()
    Latest latest = setupLatestParam()
    if ((latest instanceof Latest.UseLatestTag || latest.latest == false) &&
      !pacticipantVersion.present) {
      throw new GradleScriptException('The CanIDeploy task requires -PpacticipantVersion=... or -Platest=true', null)
    }
    String pacticipantVersion = pacticipantVersion.orElse('').get()
    String to = null
    if (toProp.present) {
      to = toProp.get()
    }
    def t = new TermColors()
    logger.debug(
      "Calling canIDeploy(pacticipant=$pacticipant, pacticipantVersion=$pacticipantVersion, latest=$latest, to=$to)"
    )
    def result = brokerClient.canIDeploy(pacticipant, pacticipantVersion, latest, to)
    if (result.ok) {
      println("Computer says yes \\o/ ${result.message}\n\n${t.green.invoke(result.reason)}")
    } else {
      println("Computer says no ¯\\_(ツ)_/¯ ${result.message}\n\n${t.red.invoke(result.reason)}")
    }

    if (result.verificationResultUrl != null) {
      println("VERIFICATION RESULTS\n--------------------\n1. ${result.verificationResultUrl}\n")
    }

    if (!result.ok) {
      throw new GradleScriptException("Can you deploy? Computer says no ¯\\_(ツ)_/¯ ${result.message}", null)
    }
  }

  private Latest setupLatestParam() {
    Latest latest = new Latest.UseLatest(false)
    if (latestProp.present) {
      String latestProp = latestProp.get()
      if (latestProp == 'true') {
        latest = new Latest.UseLatest(true)
      } else if (latestProp == 'false') {
        latest = new Latest.UseLatest(false)
      } else {
        latest = new Latest.UseLatestTag(latestProp)
      }
    }
    latest
  }
}
