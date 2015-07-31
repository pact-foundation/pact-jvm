package au.com.dius.pact.model.v3.messaging

import groovy.json.JsonSlurper
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

    Map toMap() {
        def map = MessagePact.toMap(this)
        if (contents) {
            if (metaData.contentType == 'application/json') {
                map.contents = new JsonSlurper().parseText(contents.toString())
            } else {
                map.contents = contentsAsBytes().encodeBase64().toString()
            }
        }
        map
    }
}
