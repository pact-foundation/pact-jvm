package au.com.dius.pact.model.v3.messaging

import groovy.transform.Canonical

/**
 * Message in a Message Pact
 */
@Canonical
class Message {
    String description
    String providerState
    def contents
    Map matchingRules = [:]
    Map metaData = [:]

    byte[] contentsAsBytes() {
        if (contents) {
            contents.toString().bytes
        } else {
            []
        }
    }
}
