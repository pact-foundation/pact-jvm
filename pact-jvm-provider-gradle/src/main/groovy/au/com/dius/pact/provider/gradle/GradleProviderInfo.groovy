package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import org.gradle.util.ConfigureUtil

class GradleProviderInfo extends ProviderInfo {

  GradleProviderInfo(String name) {
    super(name)
  }

  @Override
  ConsumerInfo hasPactWith(String consumer, Closure closure) {
    def consumerInfo = new ConsumerInfo(name: consumer)
    consumers << consumerInfo
    ConfigureUtil.configure(closure, consumerInfo)
    consumerInfo
  }

}
