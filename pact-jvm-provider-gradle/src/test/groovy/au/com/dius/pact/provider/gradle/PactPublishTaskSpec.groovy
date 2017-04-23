package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.broker.PactBrokerClient
import org.apache.commons.io.IOUtils
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.nio.charset.Charset

class PactPublishTaskSpec extends Specification {

  private PactPublishTask task
  private PactPlugin plugin
  private Project project
  private PactBrokerClient brokerClient

  def setup() {
    project = ProjectBuilder.builder().build()
    plugin = new PactPlugin()
    plugin.apply(project)
    task = project.tasks.pactPublish

    project.file("${project.buildDir}/pacts").mkdirs()
    project.file("${project.buildDir}/pacts/test_pact.json").withWriter {
      IOUtils.copy(PactPublishTaskSpec.getResourceAsStream('/pacts/foo_pact.json'), it, Charset.forName('UTF-8'))
    }

    brokerClient = GroovySpy(PactBrokerClient, global: true)
  }

  def 'rasies an exception if no pact publish configuration is found'() {
    when:
    task.publishPacts()

    then:
    thrown(GradleScriptException)
  }

  def 'successful publish'() {
    given:
    project.pact {
      publish {

      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(_, _) >> 'HTTP/1.1 200 OK'
  }

  def 'failure to publish'() {
    given:
    project.pact {
      publish {

      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(_, _) >> 'FAILED! 500 BOOM - It went boom, Mate!'
    thrown(GradleScriptException)
  }

  def 'passes in any authentication creds to the broker client'() {
    given:
    project.pact {
      publish {
        pactBrokerUsername = 'my user name'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * new PactBrokerClient(_, ['authentication': ['basic', 'my user name', null]]) >> brokerClient
    1 * brokerClient.uploadPactFile(_, _) >> 'HTTP/1.1 200 OK'
  }

}
