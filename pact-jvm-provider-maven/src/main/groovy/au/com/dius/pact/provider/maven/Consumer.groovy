package au.com.dius.pact.provider.maven

import groovy.transform.ToString

/**
 * Consumer Info
 */
@ToString
class Consumer {
    String name = 'consumer'
    File pactFile
    URL pactUrl
    URL stateChangeUrl
    boolean stateChangeUsesBody = true
}
