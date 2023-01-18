package au.com.dius.pact.consumer.model

import spock.lang.Specification

class MockProviderConfigSpec extends Specification {
  def 'url test'() {
    expect:
    new MockProviderConfig(hostname, port).url() == url

    where:

    hostname        | port | url
    '127.0.0.1'     | 0    | 'http://localhost:0'
    'localhost'     | 1234 | 'http://localhost:1234'
    '[::1]'         | 1234 | 'http://ip6-localhost:1234'
    'ip6-localhost' | 1234 | 'http://ip6-localhost:1234'
    '::1'           | 0    | 'http://ip6-localhost:0'
  }
}
