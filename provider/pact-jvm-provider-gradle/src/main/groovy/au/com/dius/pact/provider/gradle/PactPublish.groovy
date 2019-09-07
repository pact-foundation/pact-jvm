package au.com.dius.pact.provider.gradle

import groovy.transform.ToString

/**
 * Config for pact publish task
 */
@ToString
class PactPublish {
    def pactDirectory
    String pactBrokerUrl
    /**
     * @deprecated Use providerVersion instead
     */
    @Deprecated
    String version
    def providerVersion
    String pactBrokerToken
    String pactBrokerUsername
    String pactBrokerPassword
    String pactBrokerAuthenticationScheme
    List<String> tags = []
    List<String> excludes = []
}
