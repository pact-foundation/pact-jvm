package au.com.dius.pact.provider

import groovy.transform.Canonical
import groovy.transform.ToString

/**
 * Consumers grouped by pacts in a directory or an S3 bucket
 */
@ToString
@Canonical
class ConsumersGroup {
    def name
    File pactFileLocation
    def stateChange
    boolean stateChangeUsesBody = false
    boolean stateChangeTeardown = false
    def include = /.*\.json$/

    def url(String path) {
        stateChange = new URL(path)
    }

}
