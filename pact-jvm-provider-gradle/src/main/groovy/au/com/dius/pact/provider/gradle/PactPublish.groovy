package au.com.dius.pact.provider.gradle

import groovy.transform.ToString

@ToString
class PactPublish {
    def pactDirectory
    String pactBrokerUrl
}
