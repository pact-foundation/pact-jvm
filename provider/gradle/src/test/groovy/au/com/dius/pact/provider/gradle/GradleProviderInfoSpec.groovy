package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.PactVerification
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import spock.lang.Specification
import spock.lang.Unroll

class GradleProviderInfoSpec extends Specification {

  def 'defaults the consumer verification type to what is set on the provider'() {
    given:
    def provider = new GradleProviderInfo('provider', Mock(Project))
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    when:
    provider.hasPactWith('boddy the consumer') {

    }

    then:
    provider.consumers.first().verificationType == PactVerification.ANNOTATED_METHOD
  }

  def 'fromPactBroker configures the pact broker options'() {
    given:
    def provider = new GradleProviderInfo('provider', Mock(Project))

    when:
    provider.fromPactBroker {
      selectors = latestTags('test')
      enablePending = true
      providerTags = ['master']
    }

    then:
    provider.brokerConfig == new PactBrokerConsumerConfig([new ConsumerVersionSelector('test', true, null, null)],
      true, ['master'])
  }

  @Unroll
  def 'fromPactBroker throws an exception if pending pacts is enabled but there are no provider tags'() {
    given:
    def provider = new GradleProviderInfo('provider', Mock(Project))

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

  def 'supports specifying a fallback tag'() {
    given:
    def provider = new GradleProviderInfo('provider', Mock(Project))

    when:
    provider.fromPactBroker {
      selectors = latestTags(fallbackTag: 'A', 'test', 'test2')
      enablePending = true
      providerTags = ['master']
    }

    then:
    provider.brokerConfig == new PactBrokerConsumerConfig([
      new ConsumerVersionSelector('test', true, null, 'A'),
      new ConsumerVersionSelector('test2', true, null, 'A')
    ], true, ['master'])
  }
}
