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
@SuppressWarnings(['Println', 'DuplicateStringLiteral'])
class PactCanIDeployTask extends DefaultTask {

  private static final String PACTICIPANT = 'pacticipant'
  private static final String PACTICIPANT_VERSION = 'pacticipantVersion'
  private static final String TO = 'to'
  private static final String LATEST = 'latest'

  @TaskAction
  void canIDeploy() {
    AnsiConsole.systemInstall()
    if (!project.pact.broker) {
      throw new GradleScriptException('You must add a pact broker configuration to your build before you can ' +
        'use the CanIDeploy task', null)
    }

    Broker config = project.pact.broker
    PactBrokerClient brokerClient = setupBrokerClient(config)
    if (!project.hasProperty(PACTICIPANT)) {
      throw new GradleScriptException('The CanIDeploy task requires -Ppacticipant=...', null)
    }
    String pacticipant = project.property(PACTICIPANT)
    if (!project.hasProperty(PACTICIPANT_VERSION)) {
      throw new GradleScriptException('The CanIDeploy task requires -PpacticipantVersion=...', null)
    }
    String pacticipantVersion = project.property(PACTICIPANT_VERSION)
    Latest latest = setupLatestParam()
    String to = null
    if (project.hasProperty(TO)) {
      to = project.property(TO)
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

  private static PactBrokerClient setupBrokerClient(Broker config) {
    def options = [:]
    if (StringUtils.isNotEmpty(config.pactBrokerToken)) {
      options.authentication = [config.pactBrokerAuthenticationScheme ?: 'bearer',
                                config.pactBrokerToken]
    } else if (StringUtils.isNotEmpty(config.pactBrokerUsername)) {
      options.authentication = [config.pactBrokerAuthenticationScheme ?: 'basic',
                                config.pactBrokerUsername, config.pactBrokerPassword]
    }
    new PactBrokerClient(config.pactBrokerUrl, options)
  }
}
