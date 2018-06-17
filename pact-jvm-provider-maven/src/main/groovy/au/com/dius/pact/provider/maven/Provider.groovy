package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ProviderInfo
import groovy.transform.ToString

/**
 * Provider Info
 */
@ToString(includeSuperProperties = true)
class Provider extends ProviderInfo {
    def requestFilter
    File pactFileDirectory
    URL pactBrokerUrl
    PactBroker pactBroker
    List<File> pactFileDirectories = []
}
