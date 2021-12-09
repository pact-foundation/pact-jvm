package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import com.github.ajalt.mordant.TermColors
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Task to verify the deployment state using a pact broker
 */
@SuppressWarnings(['Println', 'DuplicateStringLiteral'])
class PactCanIDeployTask extends PactCanIDeployBaseTask {

  private static final String PACTICIPANT = 'pacticipant'
  private static final String PACTICIPANT_VERSION = 'pacticipantVersion'
  private static final String TO = 'toTag'
  private static final String LATEST = 'latest'

  @Internal
  PactBrokerClient brokerClient

  @TaskAction
  void canIDeploy() {
    if (!project.pact.broker) {
      throw new GradleScriptException('You must add a pact broker configuration to your build before you can ' +
        'use the CanIDeploy task', null)
    }

    if (brokerClient == null) {
      Broker config = project.pact.broker
      brokerClient = setupBrokerClient(config)
    }

    if (!project.hasProperty(PACTICIPANT)) {
      throw new GradleScriptException('The CanIDeploy task requires -Ppacticipant=...', null)
    }
    String pacticipant = project.property(PACTICIPANT)
    Latest latest = setupLatestParam()
    if ((latest instanceof Latest.UseLatestTag || latest.latest == false) &&
      !project.hasProperty(PACTICIPANT_VERSION)) {
      throw new GradleScriptException('The CanIDeploy task requires -PpacticipantVersion=... or -Dlatest=true', null)
    }
    String pacticipantVersion = project.hasProperty(PACTICIPANT_VERSION) ? project.property(PACTICIPANT_VERSION) : ''
    String to = null
    if (project.hasProperty(TO)) {
      to = project.property(TO)
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

    if (!result.ok) {
      throw new GradleScriptException("Can you deploy? Computer says no ¯\\_(ツ)_/¯ ${result.message}", null)
    }
  }

  private Latest setupLatestParam() {
    Latest latest = new Latest.UseLatest(false)
    if (project.hasProperty(LATEST)) {
      String latestProp = project.property(LATEST)
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
