package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import org.gradle.util.ConfigureUtil

/**
 * Extends the provider info to be setup in a gradle build
 */
class GradleProviderInfo extends ProviderInfo {

  GradleProviderInfo(String name) {
    super(name)
  }

  @Override
  ConsumerInfo hasPactWith(String consumer, Closure closure) {
    def consumerInfo = new ConsumerInfo(name: consumer)
    consumerInfo.verificationType = this.verificationType
    consumers << consumerInfo
    ConfigureUtil.configure(closure, consumerInfo)
    consumerInfo
  }

  List hasPactsFromPactBroker(Map options = [:], String pactBrokerUrl, Closure closure) {
    def fromPactBroker = super.hasPactsFromPactBroker(options, pactBrokerUrl)
    fromPactBroker.each {
      ConfigureUtil.configure(closure, it)
    }
    fromPactBroker
  }
}
