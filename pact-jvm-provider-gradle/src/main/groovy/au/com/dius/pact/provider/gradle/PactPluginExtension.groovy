package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderInfo
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.util.ConfigureUtil

/**
 * Extension object for pact plugin
 */
class PactPluginExtension {

    final NamedDomainObjectContainer<ProviderInfo> serviceProviders

    PactPublish publish

    PactPluginExtension(serviceProviders) {
        this.serviceProviders = serviceProviders
    }

    @SuppressWarnings('ConfusingMethodName')
    def serviceProviders(Closure closure) {
        serviceProviders.configure(closure)
    }

    @SuppressWarnings('ConfusingMethodName')
    def publish(Closure closure) {
        publish = new PactPublish()
        ConfigureUtil.configure(closure, publish)
    }
}
