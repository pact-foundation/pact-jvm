package au.com.dius.pact.provider.gradle

import org.gradle.api.NamedDomainObjectContainer

class PactPluginExtension {

    final NamedDomainObjectContainer<ProviderInfo> serviceProviders

    public PactPluginExtension(serviceProviders) {
        this.serviceProviders = serviceProviders
    }

    def serviceProviders(Closure closure) {
        serviceProviders.configure(closure)
    }
}
