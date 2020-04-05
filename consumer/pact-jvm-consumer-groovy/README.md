pact-jvm-consumer-groovy
=========================

Groovy DSL for Pact JVM

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-groovy`
* version-id = `4.0.x`

## Usage

Add the `pact-jvm-consumer-groovy` library to your test class path. This provides a `PactBuilder` class for you to use
to define your pacts. For a full example, have a look at the example JUnit `ExampleGroovyConsumerPactTest`.

If you are using gradle for your build, add it to your `build.gradle`:

    dependencies {
        testCompile 'au.com.dius:pact-jvm-consumer-groovy:4.0.0'
    }

Then create an instance of the `PactBuilder` in your test.

```groovy
    import au.com.dius.pact.consumer.PactVerificationResult
    import au.com.dius.pact.consumer.groovy.PactBuilder
    import groovyx.net.http.RESTClient
    import org.junit.Test

    class AliceServiceConsumerPactTest {

        @Test
        void "A service consumer side of a pact goes a little something like this"() {

            def alice_service = new PactBuilder() // Create a new PactBuilder
            alice_service {
                serviceConsumer "Consumer" 	// Define the service consumer by name
                hasPactWith "Alice Service"   // Define the service provider that it has a pact with
                port 1234                       // The port number for the service. It is optional, leave it out to
                                                // to use a random one

                given('there is some good mallory') // defines a provider state. It is optional.
                uponReceiving('a retrieve Mallory request') // upon_receiving starts a new interaction
                withAttributes(method: 'get', path: '/mallory')		// define the request, a GET request to '/mallory'
                willRespondWith(						// define the response we want returned
                    status: 200,
                    headers: ['Content-Type': 'text/html'],
                    body: '"That is some good Mallory."'
                )
            }

            // Execute the run method to have the mock server run.
            // It takes a closure to execute your requests and returns a PactVerificationResult.
            PactVerificationResult result = alice_service.runTest {
                def client = new RESTClient('http://localhost:1234/')
                def alice_response = client.get(path: '/mallory')

                assert alice_response.status == 200
                assert alice_response.contentType == 'text/html'

                def data = alice_response.data.text()
                assert data == '"That is some good Mallory."'
            }
            assert result == PactVerificationResult.Ok.INSTANCE  // This means it is all good

        }
    }
```

After running this test, the following pact file is produced:

    {
      "provider" : {
        "name" : "Alice Service"
      },
      "consumer" : {
        "name" : "Consumer"
      },
      "interactions" : [ {
        "provider_state" : "there is some good mallory",
        "description" : "a retrieve Mallory request",
        "request" : {
          "method" : "get",
          "path" : "/mallory",
          "requestMatchers" : { }
        },
        "response" : {
          "status" : 200,
          "headers" : {
            "Content-Type" : "text/html"
          },
          "body" : "That is some good Mallory.",
          "responseMatchers" : { }
        }
      } ]
    }

### DSL Methods

#### serviceConsumer(String consumer)

This names the service consumer for the pact.

#### hasPactWith(String provider)

This names the service provider for the pact.

#### port(int port)

Sets the port that the mock server will run on. If not supplied, a random port will be used.

#### given(String providerState)

Defines a state that the provider needs to be in for the request to succeed. For more info, see
https://github.com/realestate-com-au/pact/wiki/Provider-states. Can be called multiple times.

#### given(String providerState, Map params)

Defines a state that the provider needs to be in for the request to succeed. For more info, see
https://github.com/realestate-com-au/pact/wiki/Provider-states. Can be called multiple times, and the params
map can contain the data required for the state.

#### uponReceiving(String requestDescription)

Starts the definition of a of a pact interaction.

#### withAttributes(Map requestData)

Defines the request for the interaction. The request data map can contain the following:

| key                           |  Description                               | Default Value             |
|----------------------------|-------------------------------------------|-----------------------------|
| method | The HTTP method to use | get |
| path | The Path for the request | / |
| query | Query parameters as a Map<String, List> |  |
| headers | Map of key-value pairs for the request headers | |
| body | The body of the request. If it is not a string, it will be converted to JSON. Also accepts a PactBodyBuilder. | |
| prettyPrint | Boolean value to control if the body is pretty printed. See note on Pretty Printed Bodies below |

For the path, header attributes and query parameters (version 2.2.2+ for headers, 3.3.7+ for query parameters), 
you can use regular expressions to match. You can either provide a regex `Pattern` class or use the `regexp` method 
to construct a `RegexpMatcher` (you can use any of the defined matcher methods, see DSL methods below).
If you use a `Pattern`, or the `regexp` method but don't provide a value, a random one will be generated from the
regular expression. This value is used when generating requests.

For example:

```groovy
    .withAttributes(path: ~'/transaction/[0-9]+') // This will generate a random path for requests

    // or

    .withAttributes(path: regexp('/transaction/[0-9]+', '/transaction/1234567890'))
```

#### withBody(Closure closure)

Constructs the body of the request or response by invoking the supplied closure in the context of a PactBodyBuilder.

##### Pretty Printed Bodies

An optional Map can be supplied to control how the body is generated. The option values are available:

| Option | Description |
|--------|-------------|
| mimeType | The mime type of the body. Defaults to `application/json` |
| prettyPrint | Boolean value controlling whether to pretty-print the body or not. Defaults to true |

If the prettyPrint option is not specified, the bodies will be pretty printed unless the mime type corresponds to one
 that requires compact bodies. Currently only `application/x-thrift+json` is classed as requiring a compact body.

For an example of turning off pretty printing:

```groovy
service {
    uponReceiving('a request')
    withAttributes(method: 'get', path: '/')
    withBody(prettyPrint: false) {
      name 'harry'
      surname 'larry'
    }
}
```

#### willRespondWith(Map responseData)

Defines the response for the interaction. The response data map can contain the following:

| key                           |  Description                               | Default Value             |
|----------------------------|-------------------------------------------|-----------------------------|
| status | The HTTP status code to return | 200 |
| headers | Map of key-value pairs for the response headers | |
| body | The body of the response. If it is not a string, it will be converted to JSON. Also accepts a PactBodyBuilder. | |
| prettyPrint | Boolean value to control if the body is pretty printed. See note on Pretty Printed Bodies above |

For the headers (version 2.2.2+), you can use regular expressions to match. You can either provide a regex `Pattern` class or use
the `regexp` method to construct a `RegexpMatcher` (you can use any of the defined matcher methods, see DSL methods below).
If you use a `Pattern`, or the `regexp` method but don't provide a value, a random one will be generated from the
regular expression. This value is used when generating responses.

For example:

```groovy
    .willRespondWith(headers: [LOCATION: ~'/transaction/[0-9]+']) // This will generate a random location value

    // or

    .willRespondWith(headers: [LOCATION: regexp('/transaction/[0-9]+', '/transaction/1234567890')])
```

#### PactVerificationResult runTest(Closure closure)

The `runTest` method starts the mock server, and then executes the provided closure. It then returns the pact verification
result for the pact run. If you require access to the mock server configuration for the URL, it is passed into the
closure, e.g.,

```groovy

PactVerificationResult result = alice_service.runTest() { mockServer ->
  def client = new RESTClient(mockServer.url)
  def alice_response = client.get(path: '/mallory')
}
```

### Note on HTTP clients and persistent connections

Some HTTP clients may keep the connection open, based on the live connections settings or if they use a connection cache. This could
cause your tests to fail if the client you are testing lives longer than an individual test, as the mock server will be started
and shutdown for each test. This will result in the HTTP client connection cache having invalid connections. For an example of this where
the there was a failure for every second test, see [Issue #342](https://github.com/DiUS/pact-jvm/issues/342).

### Body DSL

For building JSON bodies there is a `PactBodyBuilder` that provides as DSL that includes matching with regular expressions
and by types. For a more complete example look at `PactBodyBuilderTest`.

For an example:

```groovy
service {
    uponReceiving('a request')
    withAttributes(method: 'get', path: '/')
    withBody {
      name(~/\w+/, 'harry')
      surname regexp(~/\w+/, 'larry')
      position regexp(~/staff|contractor/, 'staff')
      happy(true)
    }
}
```

This will return the following body:

```json
{
       "name": "harry",
       "surname": "larry",
       "position": "staff",
       "happy": true
}
```

and add the following matchers:

```json
{
    "$.body.name": {"regex": "\\w+"},
    "$.body.surname": {"regex": "\\w+"},
    "$.body.position": {"regex": "staff|contractor"}
}
```

#### DSL Methods

The DSL supports the following matching methods:

* regexp(Pattern re, String value = null), regexp(String regexp, String value = null)

Defines a regular expression matcher. If the value is not provided, a random one will be generated.

* hexValue(String value = null)

Defines a matcher that accepts hexidecimal values. If the value is not provided, a random hexidcimal value will be
generated.

* identifier(def value = null)

Defines a matcher that accepts integer values.  If the value is not provided, a random value will be generated.

* ipAddress(String value = null)

Defines a matcher that accepts IP addresses.  If the value is not provided, a 127.0.0.1 will be used.

* numeric(Number value = null)

Defines a matcher that accepts any numerical values. If the value is not provided, a random integer will be used.

* integer(def value = null)

Defines a matcher that accepts any integer values. If the value is not provided, a random integer will be used.

* decimal(def value = null)

Defines a matcher that accepts any decimal numbers. If the value is not provided, a random decimal will be used.

* timestamp(String pattern = null, def value = null)

If pattern is not provided the ISO_DATETIME_FORMAT is used ("yyyy-MM-dd'T'HH:mm:ss") . If the value is not provided, the current date and time is used.

* time(String pattern = null, def value = null)

If pattern is not provided the ISO_TIME_FORMAT is used ("'T'HH:mm:ss") . If the value is not provided, the current date and time is used.

* date(String pattern = null, def value = null)

If pattern is not provided the ISO_DATE_FORMAT is used ("yyyy-MM-dd") . If the value is not provided, the current date and time is used.

* uuid(String value = null)

Defines a matcher that accepts UUIDs. A random one will be generated if no value is provided.

* equalTo(def value)

Defines an equality matcher that always matches the provided value using `equals`. This is useful for resetting cascading
type matchers.

* includesStr(def value)

Defines a matcher that accepts any value where its string form includes the provided string.

* nullValue()

Defines a matcher that accepts only null values.

* url(String basePath, Object... pathFragments)

Defines a matcher for URLs, given the base URL path and a sequence of path fragments. The path fragments could be
strings or regular expression matchers. For example:

```groovy
  url('http://localhost:8080', 'pacticipants', regexp('[^\\/]+', 'Activity%20Service'))
```

Defines a matcher that accepts only null values.

#### What if a field matches a matcher name in the DSL?

When using the body DSL, if there is a field that matches a matcher name (e.g. a field named 'date') then you can do the following:

```groovy
  withBody {
    date = date()
  }
```

### Ensuring all items in a list match an example

Lots of the time you might not know the number of items that will be in a list, but you want to ensure that the list
has a minimum or maximum size and that each item in the list matches a given example. You can do this with the `eachLike`,
`minLike` and `maxLike` functions.

| function | description |
|----------|-------------|
| `eachLike()` | Ensure that each item in the list matches the provided example |
| `maxLike(integer max)` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `minLike(integer min)` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```groovy
    withBody {
        users minLike(1) {
            id identifier
            name string('Fred')
        }
    }
```

This will ensure that the user list is never empty and that each user has an identifier that is a number and a name that is a string.

You can specify the number of example items to generate in the array. The default is 1.

```groovy
    withBody {
        users minLike(1, 3) {
            id identifier
            name string('Fred')
        }
    }
```

This will create an example user list with 3 users.

The each like matchers have been updated to work with primitive types.

```groovy
withBody {
        permissions eachLike(3, 'GRANT')
}
```

will generate the following JSON

```json
{
    "permissions": ["GRANT", "GRANT", "GRANT"]
}
```

and matchers

```json
{
    "$.body.permissions": {"match": "type"}
}
```

and now you can even get more fancy

```groovy
withBody {
        permissions eachLike(3, regexp(~/\w+/))
        permissions2 minLike(2, 3, integer())
        permissions3 maxLike(4, 3, ~/\d+/)
}
```

You can also match arrays at the root level, for instance, 

```groovy
withBody PactBodyBuilder.eachLike(regexp(~/\w+/))
```

or if you have arrays of arrays

```groovy
withBody PactBodyBuilder.eachLike([ regexp('[0-9a-f]{8}', 'e8cda07e'), regexp(~/\w+/, 'sony') ])
```

An `eachArrayLike` method has been added to handle matching of arrays of arrays.

```groovy
{
  answers minLike(1) {
    questionId string("books")
    answer eachArrayLike {
          questionId string("title")
          answer string("BBBB")
    }
}
```

This will generate an array of arrays for the `answer` attribute.

### Matching any key in a map

The DSL has been extended for cases where the keys in a map are IDs. For an example of this, see 
[#313](https://github.com/DiUS/pact-jvm/issues/313). In this case you can use the `keyLike` method, which takes an
example key as a parameter.

For example:

```groovy
withBody {
  example {
    one {
      keyLike '001', 'value'            // key like an id mapped to a value
    }
    two {
      keyLike 'ABC001', regexp('\\w+')  // key like an id mapped to a matcher
    }
    three {
      keyLike 'XYZ001', {               // key like an id mapped to a closure
        id identifier()
      }
    }
    four {
      keyLike '001XYZ', eachLike {      // key like an id mapped to an array where each item is matched by the following 
        id identifier()                 // example
      }
    }  
  }
}
```

For an example, have a look at [WildcardPactSpec](src/test/au/com/dius/pact/consumer/groovy/WildcardPactSpec.groovy).

**NOTE:** The `keyLike` method adds a `*` to the matching path, so the matching definition will be applied to all keys
 of the map if there is not a more specific matcher defined for a particular key. Having more than one `keyLike` condition
 applied to a map will result in only one being applied when the pact is verified (probably the last).
 
**Further Note: From version 3.5.22 onwards pacts with wildcards applied to map keys will require the Java system property 
"pact.matching.wildcard" set to value "true" when the pact file is verified.**

### Matching with an OR

The V3 spec allows multiple matchers to be combined using either AND or OR for a value. The main use of this would be to
 either be able to match a value or a null, or to combine different matchers.
 
For example:

```groovy
    withBody {
        valueA and('AB', includeStr('A'), includeStr('B')) // valueA must include both A and B
        valueB or('100', regex(~/\d+/), nullValue()) // valueB must either match a regular expression or be null
        valueC or('12345678', regex(~/\d{8}/), regex(~/X\d{13}/)) // valueC must match either 8 or X followed by 13 digits 
    }
```

## Changing the directory pact files are written to

By default, pact files are written to `target/pacts` (or `build/pacts` if you use Gradle), but this can be overwritten with the `pact.rootDir` system property.
This property needs to be set on the test JVM as most build tools will fork a new JVM to run the tests.

For Gradle, add this to your build.gradle:

```groovy
test {
    systemProperties['pact.rootDir'] = "$buildDir/custom-pacts-directory"
}
```

## Forcing pact files to be overwritten (3.6.5+)

By default, when the pact file is written, it will be merged with any existing pact file. To force the file to be 
overwritten, set the Java system property `pact.writer.overwrite` to `true`.

# Publishing your pact files to a pact broker

If you use Gradle, you can use the [pact Gradle plugin](https://github.com/DiUS/pact-jvm/tree/master/provider/pact-jvm-provider-gradle#publishing-pact-files-to-a-pact-broker) to publish your pact files.

# Pact Specification V3

Version 3 of the pact specification changes the format of pact files in the following ways:

* Query parameters are stored in a map form and are un-encoded (see [#66](https://github.com/DiUS/pact-jvm/issues/66)
and [#97](https://github.com/DiUS/pact-jvm/issues/97) for information on what this can cause).
* Introduces a new message pact format for testing interactions via a message queue.
* Multiple provider states can be defined with data parameters.

## Generating V3 spec pact files

To have your consumer tests generate V3 format pacts, you can pass an option into the `runTest` method. For example:

```groovy
PactVerificationResult result = service.runTest(specificationVersion: PactSpecVersion.V3) { config ->
  def client = new RESTClient(config.url)
  def response = client.get(path: '/')
}
```

## Consumer test for a message consumer

For testing a consumer of messages from a message queue, the `PactMessageBuilder` class provides a DSL for defining
your message expectations. It works in much the same way as the `PactBuilder` class for Request-Response interactions,
but will generate a V3 format message pact file.

The following steps demonstrate how to use it.

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
    withContent(contentType: 'application/json') {
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

# Having values injected from provider state callbacks (3.6.11+)

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

The DSL method `fromProviderState` allows you to set an expression that will be parsed with the values returned from the provider states.
For the body, you can use the key value instead of an expression.

For example, assume that an API call is made to get the details of a user by ID. A provider state can be defined that
specifies that the user must be exist, but the ID will be created when the user is created. So we can then define an
expression for the path where the ID will be replaced with the value returned from the provider state callback.

```groovy
service {
    given('User harry exists')
    uponReceiving('a request for user harry')
    withAttributes(method: 'get', path: fromProviderState('/api/user/${id}', '/api/user/100'))
    withBody {
      name(fromProviderState('userName', 'harry')) // looks up the value using the userName key
    }
}
```
