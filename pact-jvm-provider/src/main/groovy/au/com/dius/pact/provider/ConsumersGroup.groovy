package au.com.dius.pact.provider

import groovy.transform.ToString

/**
 * Consumers grouped by pacts in a directory or an S3 bucket
 */
@ToString
class ConsumersGroup {
    def name
    def pactFileLocation
    def stateChange
    boolean stateChangeUsesBody = false
    boolean stateChangeTeardown = false
    def include = /.*\.json$/

    def url(String path) {
        stateChange = new URL(path)
    }

}
