package au.com.dius.pact.provider

import au.com.dius.pact.core.model.ClosurePactSource
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.pactbroker.PactBrokerConsumer
import spock.lang.Specification
import spock.lang.Unroll

class ConsumerInfoSpec extends Specification {

  private ConsumerInfo consumerInfo

  def setup() {
    consumerInfo = new ConsumerInfo()
  }

  @Unroll
  def 'set pact file should handle all the possible parameters correctly'() {
    when:
    consumerInfo.setPactFile(source)

    then:
    consumerInfo.pactSource.class == expectedSource

    where:

    source                                              | expectedSource
    new URL('file:/var/tmp/file')                  | UrlSource
    new FileSource(new File('/var/tmp/file'))  | FileSource
    { -> }                                              | ClosurePactSource
    '/var/tmp/file'                                     | FileSource

  }

  def 'include the tag when converting from a PactBrokerConsumer'() {
    expect:
    ConsumerInfo.from(consumer).pactSource.tag == tag

    where:
    consumer | tag
    new PactBrokerConsumer('test', 'test', 'url', [], 'TAG') | 'TAG'
  }

}
