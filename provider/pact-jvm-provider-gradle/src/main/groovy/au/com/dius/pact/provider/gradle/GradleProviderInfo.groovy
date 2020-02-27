package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ConsumersGroup
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import org.gradle.util.ConfigureUtil

/**
 * Extends the provider info to be setup in a gradle build
 */
class GradleProviderInfo extends ProviderInfo {
  def providerVersion
  def providerTag

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

  List hasPactsFromPactBrokerWithTag(Map options = [:], String pactBrokerUrl, String tag, Closure closure) {
    def fromPactBroker = super.hasPactsFromPactBrokerWithTag(options, pactBrokerUrl, tag)
    fromPactBroker.each {
      ConfigureUtil.configure(closure, it)
    }
    fromPactBroker
  }

  def url(String path) {
    new URL(path)
  }
}
