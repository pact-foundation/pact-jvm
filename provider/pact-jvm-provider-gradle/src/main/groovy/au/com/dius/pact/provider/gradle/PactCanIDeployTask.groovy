package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.apache.commons.lang3.StringUtils
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

/**
 * Task to push pact files to a pact broker
 */
@SuppressWarnings('Println')
class PactCanIDeployTask extends DefaultTask {

  @TaskAction
  void canIDeploy() {
    AnsiConsole.systemInstall()
    if (!project.pact.broker) {
      throw new GradleScriptException('You must add a pact broker configuration to your build before you can ' +
        'use the CanIDeploy task', null)
    }

    Broker config = project.pact.broker
    def options = [:]
    if (StringUtils.isNotEmpty(config.pactBrokerToken)) {
      options.authentication = [config.pactBrokerAuthenticationScheme ?: 'bearer',
                                config.pactBrokerToken]
    } else if (StringUtils.isNotEmpty(config.pactBrokerUsername)) {
      options.authentication = [config.pactBrokerAuthenticationScheme ?: 'basic',
                                config.pactBrokerUsername, config.pactBrokerPassword]
    }
    def brokerClient = new PactBrokerClient(config.pactBrokerUrl, options)
    if (!project.hasProperty('pacticipant')) {
      throw new GradleScriptException('The CanIDeploy task requires -Ppacticipant=...', null)
    }
    String pacticipant = project.property('pacticipant')
    if (!project.hasProperty('pacticipantVersion')) {
      throw new GradleScriptException('The CanIDeploy task requires -PpacticipantVersion=...', null)
    }
    String pacticipantVersion = project.property('pacticipantVersion')
    Latest latest = new Latest.UseLatest(false)
    if (project.hasProperty('latest')) {
      String latestProp = project.property('latest')
      if (latestProp == 'true') {
        latest = new Latest.UseLatest(true)
      } else if (latestProp == 'false') {
        latest = new Latest.UseLatest(false)
      } else {
        latest = new Latest.UseLatestTag(latestProp)
      }
    }
    String to = null
    if (project.hasProperty('to')) {
      to = project.property('to')
    }
    def result = brokerClient.canIDeploy(pacticipant, pacticipantVersion, latest, to)
    if (result.ok) {
      AnsiConsole.out().println(Ansi.ansi().a('Computer says yes \\o/ ').a(result.message).a('\n\n')
        .fg(Ansi.Color.GREEN).a(result.reason).reset())
    } else {
      AnsiConsole.out().println(Ansi.ansi().a('Computer says no ¯\\_(ツ)_/¯ ').a(result.message).a('\n\n')
        .fg(Ansi.Color.RED).a(result.reason).reset())
    }

    AnsiConsole.systemUninstall()

    if (!result.ok) {
      throw new GradleScriptException("Can you deploy? Computer says no ¯\\_(ツ)_/¯ ${result.message}", null)
    }
  }
}
