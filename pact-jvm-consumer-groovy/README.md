pact-jvm-consumer-groovy
=========================

Groovy DSL for Pact JVM

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-groovy_2.11`
* version-id = `2.2.x` or `3.0.x`

##Usage

Add the `pact-jvm-consumer-groovy` library to your test class path. This provides a `PactBuilder` class for you to use
to define your pacts. For a full example, have a look at the example JUnit `ExampleGroovyConsumerPactTest`.

If you are using gradle for your build, add it to your `build.gradle`:

    dependencies {
        testCompile 'au.com.dius:pact-jvm-consumer-groovy_2.11:3.0.4'
    }
  
Then create an instance of the `PactBuilder` in your test.

```groovy
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
	      // It takes a closure to execute your requests and returns a Pact VerificationResult.
	      VerificationResult result = alice_service.run() {
            def client = new RESTClient('http://localhost:1234/')
            def alice_response = client.get(path: '/mallory')

            assert alice_response.status == 200
            assert alice_response.contentType == 'text/html'

            def data = alice_response.data.text()
            assert data == '"That is some good Mallory."'
        }
        assert result == PactVerified$.MODULE$  // This means it is all good in weird Scala speak.
        
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
https://github.com/realestate-com-au/pact/wiki/Provider-states

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

For the path and header attributes (version 2.2.2+ for headers), you can use regular expressions to match.
You can either provide a regex `Pattern` class or use the `regexp` method to construct a `RegexpMatcher`
(you can use any of the defined matcher methods, see DSL methods below).
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

##### Pretty Printed Bodies [Version 2.2.15+, 3.0.4+]

An optional Map can be supplied to control how the body is generated. The option values are available:

| Option | Description |
|--------|-------------|
| mimetype | The mimetype of the body. Defaults to `application/json` |
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

#### VerificationResult run(Closure closure)

The `run` method starts the mock server, and then executes the provided closure. It then returns the pact verification
result for the pact run. If you require access to the mock server configuration for the URL, it is passed into the
closure, e.g.,

```groovy

VerificationResult result = alice_service.run() { config ->
  def client = new RESTClient(config.url())
  def alice_response = client.get(path: '/mallory')
}
```

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

* real(def value = null)

Defines a matcher that accepts any real numbers. If the value is not provided, a random double will be used.

* timestamp(def value = null)

Defines a matcher that accepts ISO and SMTP timestamps. If the value is not provided, the current date and time is used.

* uuid(String value = null)

Defines a matcher that accepts UUIDs. A random one will be generated if no value is provided.

### Ensuring all items in a list match an example (2.2.0+)

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

## Changing the directory pact files are written to (2.1.9+)

By default, pact files are written to `target/pacts`, but this can be overwritten with the `pact.rootDir` system property.
This property needs to be set on the test JVM as most build tools will fork a new JVM to run the tests.

For Gradle, add this to your build.gradle:

```groovy
test {
    systemProperties['pact.rootDir'] = "$buildDir/pacts"
}
```

# Publishing your pact files to a pact broker

If you use Gradle, you can use the [pact Gradle plugin](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-provider-gradle#publishing-pact-files-to-a-pact-broker) to publish your pact files.
