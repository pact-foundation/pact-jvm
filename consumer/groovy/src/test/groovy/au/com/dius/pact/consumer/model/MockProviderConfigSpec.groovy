package au.com.dius.pact.consumer.model

import spock.lang.Ignore
import spock.lang.Specification

class MockProviderConfigSpec extends Specification {
  @Ignore // Reverted the change for this test as it breaks all the HTTPS tests on GitHub Windows agents
  def 'url test'() {
    expect:
    new MockProviderConfig(hostname, port).url() ==~ /http:\/\/[a-z0-9\-]+\:\d+/

    where:

    hostname        | port
    '127.0.0.1'     | 0
    'localhost'     | 1234
    '[::1]'         | 1234
    'ip6-localhost' | 1234
    '::1'           | 0
  }
}
