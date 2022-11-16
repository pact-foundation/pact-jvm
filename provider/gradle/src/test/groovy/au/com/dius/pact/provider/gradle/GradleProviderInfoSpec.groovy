package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.provider.PactVerification
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class GradleProviderInfoSpec extends Specification {
  Project project
  ObjectFactory objectFactory

  @SuppressWarnings('ThrowRuntimeException')
  def setup() {
    objectFactory = [
      newInstance: { Class type, args ->
        switch (type) {
          case GradleConsumerInfo: return new GradleConsumerInfo(args[0])
          case PactBrokerConsumerConfig: return new PactBrokerConsumerConfig(objectFactory)
          case ConsumerVersionSelectorConfig: return new ConsumerVersionSelectorConfig()
          default: throw new RuntimeException("Invalid type ${type}")
        }
      }
    ] as ObjectFactory
    project = Mock(Project) {
      getObjects() >> objectFactory
    }
  }

  def 'hasPactWith - defaults the consumer verification type to what is set on the provider'() {
    given:
    def provider = new GradleProviderInfo('provider', project.objects)
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    when:
    provider.hasPactWith('boddy the consumer') {

    }

    then:
    provider.consumers.first().verificationType == PactVerification.ANNOTATED_METHOD
  }

  def 'fromPactBroker configures the pact broker options'() {
    given:
    def provider = new GradleProviderInfo('provider', project.objects)

    when:
    provider.fromPactBroker {
      selectors = latestTags('test')
      enablePending = true
      providerTags = ['master']
      providerBranch = 'master'
    }

    then:
    provider.brokerConfig.selectors == [
      new ConsumerVersionSelectors.Selector('test', true, null, null )
    ]
    provider.brokerConfig.enablePending
    provider.brokerConfig.providerTags == ['master']
    provider.brokerConfig.providerBranch == 'master'
  }

  @Unroll
  def 'fromPactBroker throws an exception if pending pacts is enabled but there are no provider tags or provider branch'() {
    given:
    def provider = new GradleProviderInfo('provider', project.objects)

    when:
    provider.fromPactBroker {
      selectors = latestTags('test')
      enablePending = true
      providerTags = tags
      providerBranch = branch
    }

    then:
    def ex = thrown(GradleScriptException)
    ex.message.trim().startsWith('No providerTags or providerBranch: To use the pending pacts feature, you need to provide the list of ' +
      'provider names')

    where:

    tags << [null, [], ['']]
    branch << [null, ' ', '']
  }

  def 'supports specifying a fallback tag'() {
    given:
    def provider = new GradleProviderInfo('provider', project.objects)

    when:
    provider.fromPactBroker {
      selectors = latestTags(fallbackTag: 'A', 'test', 'test2')
      enablePending = true
      providerTags = ['master']
    }

    then:
    provider.brokerConfig.selectors == [
      new ConsumerVersionSelectors.Selector('test', true, null, 'A'),
      new ConsumerVersionSelectors.Selector('test2', true, null, 'A')
    ]
  }

  def 'supports specifying selectors with a block'() {
    given:
    def project = ProjectBuilder.builder().build()
    def provider = new GradleProviderInfo('provider', project.objects)

    when:
    provider.fromPactBroker {
      withSelectors {
        branch('test', 'test2', 'A')
      }
      enablePending = true
      providerTags = ['master']
    }

    then:
    provider.brokerConfig.selectors == [
      new ConsumerVersionSelectors.Branch('test', 'test2', 'A')
    ]
  }
}
