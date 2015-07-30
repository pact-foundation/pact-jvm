package au.com.dius.pact.provider.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.util.ConfigureUtil

/**
 * Extension object for pact plugin
 */
class PactPluginExtension {

    final NamedDomainObjectContainer<ProviderInfo> serviceProviders

    PactPublish pactPublish

    PactPluginExtension(serviceProviders) {
        this.serviceProviders = serviceProviders
    }

    @SuppressWarnings('ConfusingMethodName')
    def serviceProviders(Closure closure) {
        serviceProviders.configure(closure)
    }

    def publish(Closure closure) {
        pactPublish = new PactPublish()
        ConfigureUtil.configure(closure, pactPublish)
    }
}
