package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.CanIDeployResult
import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PactCanIDeployTaskSpec extends Specification {

  private PactCanIDeployTask task
  private PactPlugin plugin
  private Project project

  def setup() {
    project = ProjectBuilder.builder().build()
    plugin = new PactPlugin()
    plugin.apply(project)
    task = project.tasks.canIDeploy
  }

  def 'raises an exception if no pact broker configuration is found'() {
    when:
    task.canIDeploy()

    then:
    thrown(GradleScriptException)
  }

  def 'raises an exception if no pacticipant is provided'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.canIDeploy()

    then:
    def ex = thrown(GradleScriptException)
    ex.message == 'The CanIDeploy task requires -Ppacticipant=...'
  }

  def 'raises an exception if pacticipantVersion and latest is not provided'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.evaluate()

    when:
    task.canIDeploy()

    then:
    def ex = thrown(GradleScriptException)
    ex.message == 'The CanIDeploy task requires -PpacticipantVersion=... or -Platest=true'
  }

  def 'pacticipantVersion can be missing if latest is provided'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.ext.latest = 'true'
    project.evaluate()

    task.brokerClient = Mock(PactBrokerClient) {
      canIDeploy(_, _, _, _, _) >> new CanIDeployResult(true, '', '', null, null)
    }

    when:
    task.canIDeploy()

    then:
    notThrown(GradleScriptException)
  }

  def 'calls the pact broker client'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.ext.pacticipantVersion = '1.0.0'
    project.evaluate()

    task.brokerClient = Mock(PactBrokerClient)

    when:
    task.canIDeploy()

    then:
    notThrown(GradleScriptException)
    1 * task.brokerClient.canIDeploy('pacticipant', '1.0.0', _, _, _) >>
      new CanIDeployResult(true, '', '', null, null)
  }

  def 'prints verification results url when pact broker client returns one'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.ext.pacticipantVersion = '1.0.0'
    project.ext.latest = 'true'
    project.ext.toTag = 'prod'
    project.evaluate()

    task.brokerClient = Mock(PactBrokerClient)

    when:
    task.canIDeploy()

    then:
    notThrown(GradleScriptException)
    1 * task.brokerClient.canIDeploy('pacticipant', '1.0.0',
      new Latest.UseLatest(true), 'prod', _) >> new CanIDeployResult(true, '', '', null, 'verificationResultUrl')
  }

  def 'passes optional parameters to the pact broker client'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.ext.pacticipantVersion = '1.0.0'
    project.ext.latest = 'true'
    project.ext.toTag = 'prod'
    project.evaluate()

    task.brokerClient = Mock(PactBrokerClient)

    when:
    task.canIDeploy()

    then:
    notThrown(GradleScriptException)
    1 * task.brokerClient.canIDeploy('pacticipant', '1.0.0',
      new Latest.UseLatest(true), 'prod', _) >> new CanIDeployResult(true, '', '', null, null)
  }

  def 'throws an exception if the pact broker client says no'() {
    given:
    project.pact {
      broker {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.ext.pacticipant = 'pacticipant'
    project.ext.pacticipantVersion = '1.0.0'
    project.evaluate()

    task.brokerClient = Mock(PactBrokerClient)

    when:
    task.canIDeploy()

    then:
    1 * task.brokerClient.canIDeploy('pacticipant', '1.0.0', _, _, _) >>
      new CanIDeployResult(false, 'Bad version', 'Bad version', null, null)
    def ex = thrown(GradleScriptException)
    ex.message == 'Can you deploy? Computer says no ¯\\_(ツ)_/¯ Bad version'
  }
}
