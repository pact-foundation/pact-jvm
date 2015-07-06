package au.com.dius.pact.provider.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.util.ConfigureUtil

class PactPluginExtension {

    final NamedDomainObjectContainer<ProviderInfo> serviceProviders

    PactPublish publish

    public PactPluginExtension(serviceProviders) {
        this.serviceProviders = serviceProviders
    }

    def serviceProviders(Closure closure) {
        serviceProviders.configure(closure)
    }

    def publish(Closure closure) {
        publish = new PactPublish()
        ConfigureUtil.configure(closure, publish)
    }
}
