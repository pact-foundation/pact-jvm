package au.com.dius.pact.core.model.generators

import spock.lang.Specification
import spock.lang.Unroll

class MockServerURLGeneratorSpec extends Specification {

  @SuppressWarnings('LineLength')
  @Unroll
  def 'generate returns null when #desc'() {
    expect:
    new MockServerURLGenerator(example, '.*\\/(orders\\/\\d+)$').generate(context) == null

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
      .generate(context) == 'http://mockserver/orders/5678'

    where:
    context << [
      [mockServer: [href: 'http://mockserver']],
      [mockServer: [href: 'http://mockserver/']]
    ]
  }
}
