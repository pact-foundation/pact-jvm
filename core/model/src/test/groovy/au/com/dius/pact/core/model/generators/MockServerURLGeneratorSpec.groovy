package au.com.dius.pact.core.model.generators

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class MockServerURLGeneratorSpec extends Specification {

  @Unroll
  def 'generate returns null when #desc'() {
    expect:
    new MockServerURLGenerator(example, '.*\\/(orders\\/\\d+)$').generate(context, null) == null

    where:

    desc                                                  | example                        | context
    'the test context is empty'                           | 'http://localhost:1234'        | [:]
    'there is no mock server details in the test context' | 'http://localhost:1234'        | [some: 'value']
    'the mock server details is invalid'                  | 'http://localhost:1234'        | [mockServer: 'value']
    'the mock server details is empty'                    | 'http://localhost:1234'        | [mockServer: [:]]
    'the mock server details has no URL'                  | 'http://localhost:1234'        | [mockServer: [href: null]]
    'the example value does not match'                    | 'http://localhost:1234/orders' | [mockServer: [href: 'http://mockserver']]
  }

  @Unroll
  def 'replaces the non-matching parts with the mock server base URL'() {
    expect:
    new MockServerURLGenerator('http://localhost:1234/orders/5678', '.*\\/(orders\\/\\d+)$')
      .generate(context, null) == 'http://mockserver/orders/5678'

    where:
    context << [
      [mockServer: [href: 'http://mockserver']],
      [mockServer: [href: 'http://mockserver/']]
    ]
  }

  def 'examples from Pact Compatability Suite'() {
    expect:
    new MockServerURLGenerator('http://localhost:9876/pacts/provider/{provider}/for-verification',
      '.*(\\/\\Qpacts\\E\\/\\Qprovider\\E\\/\\Q{provider}\\E\\/\\Qfor-verification\\E)$')
      .generate([mockServer: [href: 'http://localhost:40955']], null) ==
      'http://localhost:40955/pacts/provider/%7Bprovider%7D/for-verification'
  }
}
