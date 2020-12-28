package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.CanIDeployResult
import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

class PactCanIDeployMojoSpec extends Specification {

  private PactCanIDeployMojo mojo

  def setup() {
    mojo = new PactCanIDeployMojo()
    mojo.pactBrokerUrl = 'http://broker:1234'
    mojo.pacticipant = 'test'
    mojo.pacticipantVersion = '1234'
  }

  def 'throws an exception if pactBrokerUrl is not provided'() {
    given:
    mojo.pactBrokerUrl = null

    when:
    mojo.execute()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pactBrokerUrl is required'
  }

  def 'throws an exception if pacticipant is not provided'() {
    given:
    mojo.pacticipant = null

    when:
    mojo.execute()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'The can-i-deploy task requires -Dpacticipant=...'
  }

  def 'throws an exception if pacticipantVersion and latest is not provided'() {
    given:
    mojo.pacticipantVersion = null

    when:
    mojo.execute()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'The can-i-deploy task requires -DpacticipantVersion=... or -Dlatest=true'
  }

  def 'pacticipantVersion can be missing if latest is provided'() {
    given:
    mojo.pacticipantVersion = null
    mojo.latest = 'true'
    mojo.brokerClient = Mock(PactBrokerClient) {
      canIDeploy(_, _, _, _) >> new CanIDeployResult(true, '', '', null)
    }

    when:
    mojo.execute()

    then:
    notThrown(MojoExecutionException)
  }

  def 'calls the pact broker client'() {
    given:
    mojo.brokerClient = Mock(PactBrokerClient)

    when:
    mojo.execute()

    then:
    notThrown(MojoExecutionException)
    1 * mojo.brokerClient.canIDeploy('test', '1234', _, _) >>
      new CanIDeployResult(true, '', '', null)
  }

  def 'passes optional parameters to the pact broker client'() {
    given:
    mojo.latest = 'true'
    mojo.to = 'prod'
    mojo.brokerClient = Mock(PactBrokerClient)

    when:
    mojo.execute()

    then:
    notThrown(MojoExecutionException)
    1 * mojo.brokerClient.canIDeploy('test', '1234',
      new Latest.UseLatest(true), 'prod') >> new CanIDeployResult(true, '', '', null)
  }

  def 'throws an exception if the pact broker client says no'() {
    given:
    mojo.brokerClient = Mock(PactBrokerClient)

    when:
    mojo.execute()

    then:
    1 * mojo.brokerClient.canIDeploy('test', '1234', _, _) >>
      new CanIDeployResult(false, 'Bad version', 'Bad version', null)
    def ex = thrown(MojoExecutionException)
    ex.message == 'Can you deploy? Computer says no ¯\\_(ツ)_/¯ Bad version'
  }
}
