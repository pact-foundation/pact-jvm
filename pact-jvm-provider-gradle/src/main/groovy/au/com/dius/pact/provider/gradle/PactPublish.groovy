package au.com.dius.pact.provider.gradle

import groovy.transform.ToString

/**
 * Config for pact publish task
 */
@ToString
class PactPublish {
    def pactDirectory
    String pactBrokerUrl
    String version
}
