package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.consumer.groovy.BaseBuilder
import au.com.dius.pact.consumer.groovy.InvalidPactException
import au.com.dius.pact.consumer.groovy.PactBodyBuilder
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.PactFragment
import au.com.dius.pact.model.Provider
import scala.collection.mutable.ListBuffer

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
        messages << [description: description, metaData: [:], matchers: [:]]
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
        messages.last().matchers.putAll(body.matchers)

        this
    }

    void run(Closure closure) {
        def fragment = new PactFragment(consumer, provider, new ListBuffer())
        def results = messages.collect {
            invokeTest(fragment, it, closure)
        }

        if (results.any { it != PACTVERIFIED }) {
            throw new MessagePactFailedException(results.findAll { it != PACTVERIFIED })
        }
    }

    private invokeTest(PactFragment fragment, message, Closure closure) {

    }
}
