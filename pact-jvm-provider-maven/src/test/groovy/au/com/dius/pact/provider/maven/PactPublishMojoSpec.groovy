package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.broker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

class PactPublishMojoSpec extends Specification {

  private PactPublishMojo mojo
  private PactBrokerClient brokerClient
  private File mockFile

  def setup() {
    brokerClient = GroovyMock(PactBrokerClient, global: true)
    mojo = new PactPublishMojo(pactDirectory: 'some/dir', brokerClient: brokerClient)
    mockFile = GroovyStub(File, global: true)
  }

  def 'uploads all pacts to the pact broker'() {
    given:
    new File('some/dir') >> mockFile
    mockFile.eachFileMatch(_, _, _) >> { args ->
      args[2].call(mockFile)
      args[2].call(mockFile)
      args[2].call(mockFile)
    }

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _) >> 'OK'
  }

  def 'Fails with an exception if any pacts fail to upload'() {
    given:
    new File('some/dir') >> mockFile
    mockFile.eachFileMatch(_, _, _) >> { args ->
      args[2].call(mockFile)
      args[2].call(mockFile)
      args[2].call(mockFile)
    }

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _) >> 'OK' >> 'FAILED! Bang' >> 'OK'
    thrown(MojoExecutionException)
  }

  def 'if the broker username is set, passes in the creds to the broker client'() {
    given:
    mojo.pactBrokerUsername = 'username'
    mojo.pactBrokerPassword = 'password'
    mojo.brokerClient = null
    mojo.pactBrokerUrl = '/broker'
    new File('some/dir') >> mockFile
    mockFile.eachFileMatch(_, _, _)

    when:
    mojo.execute()

    then:
    new PactBrokerClient('/broker', _) >> { args ->
      assert args[1] == [authentication: ['basic', 'username', 'password']]
      brokerClient
    }
  }

}
