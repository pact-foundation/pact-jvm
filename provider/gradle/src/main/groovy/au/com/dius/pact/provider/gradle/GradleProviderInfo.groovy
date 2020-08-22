package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ConsumersGroup
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import org.gradle.api.GradleScriptException
import org.gradle.util.ConfigureUtil

/**
 * Extends the provider info to be setup in a gradle build
 */
class GradleProviderInfo extends ProviderInfo {
  def providerVersion
  /**
   * @deprecated Use providerTags instead
   */
  @Deprecated
  def providerTag
  Closure<List<String>> providerTags
  PactBrokerConsumerConfig brokerConfig

  GradleProviderInfo(String name) {
    super(name)
  }

  IConsumerInfo hasPactWith(String consumer, Closure closure) {
    def consumerInfo = new ConsumerInfo(consumer, null, true, [], this.verificationType)
    consumers << consumerInfo
    ConfigureUtil.configure(closure, consumerInfo)
    consumerInfo
  }

  List<IConsumerInfo> hasPactsWith(String consumersGroupName, Closure closure) {
    def consumersGroup = new ConsumersGroup(consumersGroupName)
    ConfigureUtil.configure(closure, consumersGroup)
    setupConsumerListFromPactFiles(consumersGroup)
  }

  List hasPactsFromPactBroker(Map options = [:], String pactBrokerUrl, Closure closure) {
    def fromPactBroker = super.hasPactsFromPactBroker(options, pactBrokerUrl)
    fromPactBroker.each {
      ConfigureUtil.configure(closure, it)
    }
    fromPactBroker
  }

  List hasPactsFromPactBrokerWithSelectors(Map options = [:], String pactBrokerUrl,
                                           List<ConsumerVersionSelector> selectors, Closure closure) {
    def fromPactBroker = super.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
    fromPactBroker.each {
      ConfigureUtil.configure(closure, it)
    }
    fromPactBroker
  }

  def url(String path) {
    new URL(path)
  }

  @SuppressWarnings('LineLength')
  def fromPactBroker(Closure closure) {
    brokerConfig = new PactBrokerConsumerConfig()
    ConfigureUtil.configure(closure, brokerConfig)

    if (brokerConfig.enablePending && (!brokerConfig.providerTags ||
      brokerConfig.providerTags.findAll { !it.trim().empty }.empty)) {
      throw new GradleScriptException(
        '''
        |No providerTags: To use the pending pacts feature, you need to provide the list of provider names for the provider application version that will be published with the verification results.
        |
        |For instance:
        |
        |fromPactBroker {
        |    selectors = latestTags('test')
        |    enablePending = true
        |    providerTags = ['master']
        |}
        '''.stripMargin(), null)
    }
  }
}
