package au.com.dius.pact.provider.maven

import groovy.transform.ToString

/**
 * Provider Info
 */
@ToString
class Provider {
    String protocol = 'http'
    String host = 'localhost'
    Integer port = 8080
    String path = '/'
    String name = 'provider'

    List<Consumer> consumers = []
    def requestFilter
    File pactFileDirectory
    URL stateChangeUrl
    boolean stateChangeUsesBody = true
    def stateChangeRequestFilter
    def createClient
    boolean insecure = false
    File trustStore
    String trustStorePassword = 'changeit'
}
