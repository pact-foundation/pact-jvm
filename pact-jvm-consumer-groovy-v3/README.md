pact-jvm-consumer-groovy-v3
===========================

Groovy DSL for Pact JVM implementing V3 specification changes.

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-groovy-v3_2.11`
* version-id = `2.2.x` or `3.0.x`

##Usage

Add the `pact-jvm-consumer-groovy-v3` library to your test class path. This provides a `PactMessageBuilder` class for you to use
to define your pacts.

If you are using gradle for your build, add it to your `build.gradle`:

    dependencies {
        testCompile 'au.com.dius:pact-jvm-consumer-groovy-v3_2.11:2.2.12'
    }
  
## Consumer test for a message consumer

The `PactMessageBuilder` class provides a DSL for defining your message expectations. It works in much the same way as
the `PactBuilder` class for Request-Response interactions.

### Step 1 - define the message expectations

Create a test that uses the `PactMessageBuilder` to define a message expectation, and then call `run`. This will invoke
the given closure with a message for each one defined in the pact.

```groovy
def eventStream = new PactMessageBuilder().call {
    serviceConsumer 'messageConsumer'
    hasPactWith 'messageProducer'

    given 'order with id 10000004 exists'

    expectsToReceive 'an order confirmation message'
    withMetaData(type: 'OrderConfirmed') // Can define any key-value pairs here
    withContent('application/json') {
        type 'OrderConfirmed'
        audit {
            userCode 'messageService'
        }
        origin 'message-service'
        referenceId '10000004-2'
        timeSent: '2015-07-22T10:14:28+00:00'
        value {
            orderId '10000004'
            value '10.000000'
            fee '10.00'
            gst '15.00'
        }
    }
}
```

### Step 2 - call your message handler with the generated messages

This example tests a message handler that gets messages from a Kafka topic. In this case the Pact message is wrapped
as a Kafka `MessageAndMetadata`.

```groovy
eventStream.run { Message message ->
    messageHandler.handleMessage(new MessageAndMetadata('topic', 1,
        new kafka.message.Message(message.contentsAsBytes()), 0, null, valueDecoder))
}
```

### Step 3 - validate that the message was handled correctly

```groovy
def order = orderRepository.getOrder('10000004')
assert order.status == 'confirmed'
assert order.value == 10.0
```

### Step 4 - Publish the pact file

If the test was successful, a pact file would have been produced with the message from step 1.
