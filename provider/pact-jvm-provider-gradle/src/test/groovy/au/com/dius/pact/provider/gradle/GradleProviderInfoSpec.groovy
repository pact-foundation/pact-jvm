package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.PactVerification
import org.gradle.api.GradleScriptException
import spock.lang.Specification
import spock.lang.Unroll

class GradleProviderInfoSpec extends Specification {

  def 'defaults the consumer verification type to what is set on the provider'() {
    given:
    def provider = new GradleProviderInfo('provider')
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    when:
    provider.hasPactWith('boddy the consumer') {

    }

    then:
    provider.consumers.first().verificationType == PactVerification.ANNOTATED_METHOD
  }

  def 'fromPactBroker configures the pact broker options'() {
    given:
    def provider = new GradleProviderInfo('provider')

    when:
    provider.fromPactBroker {
      selectors = latestTags('test')
      enablePending = true
      providerTags = ['master']
    }

    then:
    provider.brokerConfig == new PactBrokerConsumerConfig([new ConsumerVersionSelector('test', true)],
      true, ['master'])
  }

  @Unroll
  def 'fromPactBroker throws an exception if pending pacts is enabled but there are no provider tags'() {
    given:
    def provider = new GradleProviderInfo('provider')

    when:
    provider.fromPactBroker {
      selectors = latestTags('test')
      enablePending = true
      providerTags = tags
    }

    then:
    def ex = thrown(GradleScriptException)
    ex.message.trim().startsWith('No providerTags: To use the pending pacts feature, you need to provide the list of ' +
      'provider names')

    where:

    tags << [null, [], ['']]
  }

}
