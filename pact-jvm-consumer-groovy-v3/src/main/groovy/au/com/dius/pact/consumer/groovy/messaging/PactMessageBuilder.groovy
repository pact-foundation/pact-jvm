package au.com.dius.pact.consumer.groovy.messaging

@SuppressWarnings('UnusedImport')
import au.com.dius.pact.consumer.PactConsumerConfig$
import au.com.dius.pact.consumer.groovy.BaseBuilder
import au.com.dius.pact.consumer.groovy.InvalidPactException
import au.com.dius.pact.consumer.groovy.PactBodyBuilder
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact

/**
 * Pact builder for consumer tests for messaging
 */
class PactMessageBuilder extends BaseBuilder {
    Consumer consumer
    Provider provider
    String providerState = ''
    List messages = []

    PactMessageBuilder serviceConsumer(String consumer) {
        this.consumer = new Consumer(consumer)
        this
    }

    PactMessageBuilder hasPactWith(String provider) {
        this.provider = new Provider(provider)
        this
    }

    PactMessageBuilder given(String providerState) {
        this.providerState = providerState
        this
    }

    PactMessageBuilder expectsToReceive(String description) {
        messages << new Message(description, providerState)
        this
    }

    PactMessageBuilder withMetaData(Map metaData) {
        if (messages.empty) {
            throw new InvalidPactException('expectsToReceive is required before withMetaData')
        }
        messages.last().metaData = metaData
        this
    }

    PactMessageBuilder withContent(String contentType = null, Closure closure) {
        if (messages.empty) {
            throw new InvalidPactException('expectsToReceive is required before withContent')
        }
        if (contentType) {
            messages.last().metaData.contentType = contentType
        }

        def body = new PactBodyBuilder()
        closure.delegate = body
        closure.call()
        messages.last().contents = body.body
        messages.last().matchingRules.putAll(body.matchers)

        this
    }

    void run(Closure closure) {
        def pact = new MessagePact(consumer, provider, messages)
        def results = messages.collect {
            try {
                closure.call(it)
            } catch (ex) {
                ex
            }
        }

        if (results.any { it instanceof Throwable }) {
            throw new MessagePactFailedException(results.findAll { it instanceof Throwable })
        } else {
            pact.write(PactConsumerConfig$.MODULE$.pactRootDir())
        }
    }

}
