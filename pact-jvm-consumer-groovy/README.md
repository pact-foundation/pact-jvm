pact-jvm-consumer-groovy
=========================

Groovy DSL for Pact JVM

##Dependency

The library is available on maven central using:

group-id = `au.com.dius`

artifact-id = `pact-jvm-consumer-groovy_2.10`

version-id = `2.0.1`

##Usage

Add the `pact-jvm-consumer-groovy` library to your test class path. This provides a `PactBuilder` class for you to use to define your pacts. For a full example, have a look at the example JUnit `ExampleGroovyConsumerPactTest`.

If you are using gradle for your build, add it to your `build.gradle`:

    dependencies {
        testCompile 'au.com.dius:pact-jvm-consumer-groovy_2.10:2.0.1'
    }
  
Then create an instance of the `PactBuilder` in your test.

```groovy
    @Test
    void "A service consumer side of a pact goes a little something like this"() {

        def alice_service = new PactBuilder() // Create a new PactBuilder
        alice_service {
            service_consumer "Consumer" 	// Define the service consumer by name
            has_pact_with "Alice Service"   	// Define the service provider that it has a pact with
            port 1234						// The port number for the service. It is optional

            given('there is some good mallory') // defines a provider state. It is optional.
            upon_receiving('a retrieve Mallory request') // upon_receiving starts a new interaction
            with(method: 'get', path: '/mallory')		// define the request, a GET request to '/mallory'
            will_respond_with(						// define the response we want returned
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
      } ],
      "metadata" : {
        "pact_gem" : {
          "version" : "1.0.9"
        },
        "pact-jvm" : {
          "version" : "2.0-RC6"
        }
      }
    }

### DSL Methods

#### service_consumer(String consumer)

This names the service consumer for the pact.

#### has_pact_with(String provider)

This names the service provider for the pact.

#### port(int port)

Sets the port that the mock server will run on. If not supplied, a random port will be used.

#### given(String providerState)

Defines a state that the provider needs to be in for the request to succeed. For more info, see
https://github.com/realestate-com-au/pact/wiki/Provider-states

#### upon_receiving(String requestDescription)

Starts the definition of a of a pact interaction.

#### with(Map requestData)

Defines the request for the interaction. The request data map can contain the following:

| key                           |  Description                               | Default Value             |
|----------------------------|-------------------------------------------|-----------------------------|
| method | The HTTP method to use | get |
| path | The Path for the request | / |
| query | Query parameters as a Map<String, List> |  |
| headers | Map of key-value pairs for the request headers | |
| body | The body of the request. If it is not a string, it will be converted to JSON | |

#### will_respond_with(Map responseData)

Defines the response for the interaction. The response data map can contain the following:

| key                           |  Description                               | Default Value             |
|----------------------------|-------------------------------------------|-----------------------------|
| status | The HTTP status code to return | 200 |
| headers | Map of key-value pairs for the response headers | |
| body | The body of the response. If it is not a string, it will be converted to JSON | |

#### VerificationResult run(Closure closure)

The `run` method starts the mock server, and then executes the provided closure. It then returns the pact verification result for the pact run.
