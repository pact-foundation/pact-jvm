package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

class PactCreateVersionTagMojoSpec extends Specification {

  private PactCreateVersionTagMojo mojo

  def setup() {
    mojo = new PactCreateVersionTagMojo()
    mojo.pactBrokerUrl = 'http://broker:1234'
    mojo.pacticipant = 'test'
    mojo.pacticipantVersion = '1234'
    mojo.tag = 'testTag'
  }

  def 'throws an exception if pactBrokerUrl is not provided'() {
    given:
    mojo.pactBrokerUrl = null

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pactBrokerUrl is required'
  }

  def 'throws an exception if pactBrokerUrl is empty'() {
    given:
    mojo.pactBrokerUrl = ''

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pactBrokerUrl is required'
  }

  def 'throws an exception if pacticipant is not provided'() {
    given:
    mojo.pacticipant = null

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pacticipant is required'
  }

  def 'throws an exception if pacticipant is empty'() {
    given:
    mojo.pacticipant = ''

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pacticipant is required'
  }

  def 'throws an exception if pacticipantVersion is not provided'() {
    given:
    mojo.pacticipantVersion = null

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pacticipantVersion is required'
  }

  def 'throws an exception if pacticipantVersion is empty'() {
    given:
    mojo.pacticipantVersion = ''

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'pacticipantVersion is required'
  }

  def 'throws an exception if tag is not provided'() {
    given:
    mojo.tag = null

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'tag is required'
  }

  def 'throws an exception if tag is empty'() {
    given:
    mojo.tag = ''

    when:
    mojo.prepare()

    then:
    def ex = thrown(MojoExecutionException)
    ex.message == 'tag is required'
  }

  def 'creates a broker client if not specified before'() {
    given:
    mojo.brokerClient = null

    when:
    mojo.prepare()

    then:
    mojo.brokerClient != null
    mojo.brokerClient.pactBrokerUrl == mojo.pactBrokerUrl
  }

  def 'calls pact broker client with mandatory arguments'() {
    given:
    mojo.brokerClient = Mock(PactBrokerClient)

    when:
    mojo.execute()

    then:
    notThrown(MojoExecutionException)
    1 * mojo.brokerClient.createVersionTag(
            'test', '1234', 'testTag')
  }
}
