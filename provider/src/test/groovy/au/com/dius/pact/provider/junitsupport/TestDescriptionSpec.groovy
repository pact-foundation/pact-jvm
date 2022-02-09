package au.com.dius.pact.provider.junitsupport

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import spock.lang.Specification
import spock.lang.Unroll

class TestDescriptionSpec extends Specification {
  def 'when BrokerUrlSource tests description includes tag if present'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
      [ new ProviderState('Test State') ], new Request(), new Response())
    def pact = new RequestResponsePact(
      new au.com.dius.pact.core.model.Provider(),
      new au.com.dius.pact.core.model.Consumer('the-consumer-name'),
      [ interaction ],
      [:],
      new BrokerUrlSource('url', 'url', [:], [:], tag)
    )

    expect:
    def generator = new TestDescription(interaction, pact.source, null, pact.consumer)
    description == generator.generateDescription()

    where:
    tag      | description
    'master' | 'the-consumer-name [tag:master] - Upon Interaction 1 '
    null     | 'the-consumer-name - Upon Interaction 1 '
    ''       | 'the-consumer-name - Upon Interaction 1 '
  }

  def 'when non broker pact source tests name are built correctly'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
      [ new ProviderState('Test State') ], new Request(), new Response())
    def pact = new RequestResponsePact(new au.com.dius.pact.core.model.Provider(),
      new au.com.dius.pact.core.model.Consumer(),
      [ interaction ],
      [:],
      new DirectorySource(Mock(File))
    )

    expect:
    def generator = new TestDescription(interaction, pact.source, null, pact.consumer)
    'consumer - Upon Interaction 1 ' == generator.generateDescription()
  }

  @Unroll
  def 'when pending pacts is #pending'() {
    given:
    def interaction = new RequestResponseInteraction('Interaction 1',
      [ new ProviderState('Test State') ], new Request(), new Response())
    def pactSource =  new BrokerUrlSource('url', 'url', [:], [:], 'master',
      new PactBrokerResult('test', 'test', 'test', [], [],
        pending == 'enabled', null, false, true, null))
    def pact = new RequestResponsePact(new au.com.dius.pact.core.model.Provider(),
      new au.com.dius.pact.core.model.Consumer('the-consumer-name'), [interaction ],
      [:], pactSource)
    def generator = new TestDescription(interaction, pact.source, null, pact.consumer)

    expect:
    description == generator.generateDescription()

    where:
    pending    | description
    'enabled'  | 'test [tag:master] - Upon Interaction 1 <PENDING> '
    'disabled' | 'test [tag:master] - Upon Interaction 1 '
  }
}
