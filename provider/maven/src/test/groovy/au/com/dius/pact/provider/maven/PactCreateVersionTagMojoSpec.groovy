package au.com.dius.pact.provider.maven

import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

class PactCreateVersionTagMojoSpec extends Specification {

  private PactCreateVersionTagMojo mojo

  def setup() {
    mojo = new PactCreateVersionTagMojo()
    mojo.pactBrokerUrl = 'http://broker:1234'
    mojo.pacticipant = 'test'
  }

  def 'dummy'() {
    expect:
    true
  }
}
