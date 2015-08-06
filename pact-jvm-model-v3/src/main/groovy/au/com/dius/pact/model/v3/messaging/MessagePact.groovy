package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.v3.V3Pact
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

/**
 * Pact for a sequences of messages
 */
@Slf4j
@ToString(includeSuperProperties = true)
@EqualsAndHashCode(callSuper = true)
@TupleConstructor(includeSuperProperties = true)
class MessagePact extends V3Pact {
    List messages = []

    Map toMap() {
        [
            consumer: [name: consumer.name()],
            provider: [name: provider.name()],
            messages: messages*.toMap(),
            metadata: metadata
        ]
    }

}
