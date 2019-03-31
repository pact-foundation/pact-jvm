package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.PactBrokerClient
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
  private File pactFile

  def setup() {
    project = ProjectBuilder.builder().build()
    plugin = new PactPlugin()
    plugin.apply(project)
    task = project.tasks.pactPublish

    project.file("${project.buildDir}/pacts").mkdirs()
    pactFile = project.file("${project.buildDir}/pacts/test_pact.json")
    pactFile.withWriter {
      IOUtils.copy(PactPublishTaskSpec.getResourceAsStream('/pacts/foo_pact.json'), it, Charset.forName('UTF-8'))
    }

    brokerClient = GroovySpy(PactBrokerClient, global: true, constructorArgs: ['baseUrl'])
  }

  def 'raises an exception if no pact publish configuration is found'() {
    when:
    task.publishPacts()

    then:
    thrown(GradleScriptException)
  }

  def 'successful publish'() {
    given:
    project.pact {
      publish {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(_, _, _) >> 'HTTP/1.1 200 OK'
  }

  def 'failure to publish'() {
    given:
    project.pact {
      publish {
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(_, _, _) >> 'FAILED! 500 BOOM - It went boom, Mate!'
    thrown(GradleScriptException)
  }

  def 'passes in basic authentication creds to the broker client'() {
    given:
    project.pact {
      publish {
        pactBrokerUsername = 'my user name'
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * new PactBrokerClient(_, ['authentication': ['basic', 'my user name', null]]) >> brokerClient
    1 * brokerClient.uploadPactFile(_, _, _) >> 'HTTP/1.1 200 OK'
  }

  def 'passes in bearer token to the broker client'() {
    given:
    project.pact {
      publish {
        pactBrokerToken = 'token1234'
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * new PactBrokerClient(_, ['authentication': ['bearer', 'token1234']]) >> brokerClient
    1 * brokerClient.uploadPactFile(_, _, _) >> 'HTTP/1.1 200 OK'
  }

  def 'passes in any tags to the broker client'() {
    given:
    project.pact {
      publish {
        tags = ['tag1']
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(_, _, ['tag1']) >> 'HTTP/1.1 200 OK'
  }

  def 'allows pact files to be excluded from publishing'() {
    given:
    project.pact {
      publish {
        excludes = ['other-pact', 'pact\\-\\d+']
        pactBrokerUrl = 'pactBrokerUrl'
      }
    }
    project.evaluate()

    List<File> excluded = ['pact-1', 'pact-2', 'other-pact'].collect { pactName ->
      def file = project.file("${project.buildDir}/pacts/${pactName}.json")
      file.withWriter {
        IOUtils.copy(PactPublishTaskSpec.getResourceAsStream('/pacts/foo_pact.json'), it, Charset.forName('UTF-8'))
      }
      file
    }

    when:
    task.publishPacts()

    then:
    1 * brokerClient.uploadPactFile(pactFile, _, []) >> 'HTTP/1.1 200 OK'
    0 * brokerClient.uploadPactFile(excluded[0], _, [])
    0 * brokerClient.uploadPactFile(excluded[1], _, [])
    0 * brokerClient.uploadPactFile(excluded[2], _, [])
  }

}
