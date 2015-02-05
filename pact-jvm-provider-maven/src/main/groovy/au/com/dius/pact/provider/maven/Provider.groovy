package au.com.dius.pact.provider.maven

import groovy.transform.ToString

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
}
