package au.com.dius.pact.provider.junitsupport

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import spock.lang.Specification
import spock.lang.Unroll

class TestDescriptionSpec extends Specification {
  def 'when BrokerUrlSource tests description includes tag if present'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
      [ new ProviderState('Test State') ], new Request(), new Response())
    def source = new BrokerUrlSource('url', 'url', [:], [:], tag)

    expect:
    def generator = new TestDescription(interaction, source, 'the-consumer-name')
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
    def source = new DirectorySource(Mock(File))

    expect:
    def generator = new TestDescription(interaction, source, 'consumer')
    'consumer - Upon Interaction 1 ' == generator.generateDescription()
  }

  @Unroll
  def 'when pending pacts is #pending'() {
    given:
    def interaction = new RequestResponseInteraction('Interaction 1',
      [ new ProviderState('Test State') ], new Request(), new Response())
    def pactSource =  new BrokerUrlSource('url', 'url', [:], [:], 'master',
      new PactBrokerResult('test', 'test', 'test', [], pending == 'enabled', null, false, true, null))
    def generator = new TestDescription(interaction, pactSource, 'the-consumer-name')

    expect:
    description == generator.generateDescription()

    where:
    pending    | description
    'enabled'  | 'test [tag:master] - Upon Interaction 1 <PENDING> '
    'disabled' | 'test [tag:master] - Upon Interaction 1 '
  }
}
