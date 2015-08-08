package au.com.dius.pact.provider

import groovy.transform.ToString

/**
 * Consumers grouped by pacts in a directory
 */
@ToString
class ConsumersGroup {
    def name
    File pactFileLocation
    def stateChange
    boolean stateChangeUsesBody = false

    def url(String path) {
        stateChange = new URL(path)
    }

}
