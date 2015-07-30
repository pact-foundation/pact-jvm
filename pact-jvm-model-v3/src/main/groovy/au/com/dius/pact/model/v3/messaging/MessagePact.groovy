package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import groovy.transform.Canonical

/**
 * Pact for a sequences of messages
 */
@Canonical
class MessagePact {
    Consumer consumer
    Provider provider
    List messages = []

    @SuppressWarnings('EmptyMethod')
    void write() {

    }
}
