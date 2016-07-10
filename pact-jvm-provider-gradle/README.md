pact-jvm-provider-gradle
========================

Gradle plugin for verifying pacts against a provider.

The Gradle plugin creates a task `pactVerify` to your build which will verify all configured pacts against your provider.

## To Use It

### For Gradle versions prior to 2.1

#### 1.1. Add the pact-jvm-provider-gradle jar file to your build script class path:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'au.com.dius:pact-jvm-provider-gradle_2.10:3.2.4'
    }
}
```

#### 1.2. Apply the pact plugin

```groovy
apply plugin: 'au.com.dius.pact'
```

### For Gradle versions 2.1+

```groovy
plugins {
  id "au.com.dius.pact" version "3.2.4"
}
```

### 2. Define the pacts between your consumers and providers

```groovy

pact {

    serviceProviders {

        // You can define as many as you need, but each must have a unique name
        provider1 {
            // All the provider properties are optional, and have sensible defaults (shown below)
            protocol = 'http'
            host = 'localhost'
            port = 8080
            path = '/'

            // Again, you can define as many consumers for each provider as you need, but each must have a unique name
            hasPactWith('consumer1') {

                // currently supports a file path using file() or a URL using url()
                pactFile = file('path/to/provider1-consumer1-pact.json')

            }
            
            // Or if you have many pact files in a directory
            hasPactsWith('manyConsumers') {

                // Will define a consumer for each pact file in the directory. 
                // Consumer name is read from contents of pact file 
                pactFileLocation = file('path/to/pacts')
                               
            }

        }

    }

}
```

### 3. Execute `gradle pactVerify`

## Specifying the provider hostname at runtime

If you need to calculate the provider hostname at runtime, you can give a Closure as the provider host.

```groovy
pact {

    serviceProviders {

        provider1 {
            host = { lookupHostName() }

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }
        }

    }

}
```

## Specifying the pact file or URL at runtime [versions 3.2.7/2.4.9+]

If you need to calculate the pact file or URL at runtime, you can give a Closure as the provider host.

```groovy
pact {

    serviceProviders {

        provider1 {
            host = 'localhost'

            hasPactWith('consumer1') {
                pactFile = { lookupPactFile() }
            }
        }

    }

}
```

## Starting and shutting down your provider

If you need to start-up or shutdown your provider, you can define a start and terminate task for each provider.
You could use the jetty tasks here if you provider is built as a WAR file.

```groovy

// This will be called before the provider task
task('startTheApp') << {
  // start up your provider here
}

// This will be called after the provider task
task('killTheApp') << {
  // kill your provider here
}

pact {

    serviceProviders {

        provider1 {

            startProviderTask = startTheApp
            terminateProviderTask = killTheApp

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```

Following typical Gradle behaviour, you can set the provider task properties to the actual tasks, or to the task names
as a string (for the case when they haven't been defined yet).

## Enabling insecure SSL [version 2.2.8+]

For providers that are running on SSL with self-signed certificates, you need to enable insecure SSL mode by setting
`insecure = true` on the provider.

```groovy
pact {

    serviceProviders {

        provider1 {
            insecure = true // allow SSL with a self-signed cert
            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```
## Specifying a custom trust store [version 2.2.8+]

For environments that are running their own certificate chains:

```groovy
pact {

    serviceProviders {

        provider1 {
            trustStore = new File('relative/path/to/trustStore.jks')
            trustStorePassword = 'changeit'
            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```

`trustStore` is either relative to the current working (build) directory. `trustStorePassword` defaults to `changeit`.

NOTE: The hostname will still be verified against the certificate.

## Modifying the HTTP Client Used [version 2.2.4+]

The default HTTP client is used for all requests to providers (created with a call to `HttpClients.createDefault()`).
This can be changed by specifying a closure assigned to createClient on the provider that returns a CloseableHttpClient. For example:

```groovy
pact {

    serviceProviders {

        provider1 {

            createClient = { provider ->
                // This will enable the client to accept self-signed certificates
                HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, { x509Certificates, s -> true })
                        .build())
                    .build()
            }

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```

## Modifying the requests before they are sent

**NOTE on breaking change: Version 2.1.8+ uses Apache HttpClient instead of HttpBuilder so the closure will receive a
HttpRequest object instead of a request Map.**

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The Pact Gradle plugin provides a request filter that can be
set to a closure on the provider that will be called before the request is made. This closure will receive the HttpRequest
prior to it being executed.

```groovy
pact {

    serviceProviders {

        provider1 {

            requestFilter = { req ->
                // Add an authorization header to each request
                req.addHeader('Authorization', 'OAUTH eyJhbGciOiJSUzI1NiIsImN0eSI6ImFw...')
            }

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```

__*Important Note:*__ You should only use this feature for things that can not be persisted in the pact file. By modifying
the request, you are potentially modifying the contract from the consumer tests!

## Project Properties

The following project properties can be specified with `-Pproperty=value` on the command line:

|Property|Description|
|--------|-----------|
|pact.showStacktrace|This turns on stacktrace printing for each request. It can help with diagnosing network errors|
|pact.filter.consumers|Comma seperated list of consumer names to verify|
|pact.filter.description|Only verify interactions whose description match the provided regular expression|
|pact.filter.providerState|Only verify interactions whose provider state match the provided regular expression. An empty string matches interactions that have no state|

## Provider States

For a description of what provider states are, see the pact documentations: http://docs.pact.io/documentation/provider_states.html

### Using a state change URL

For each provider you can specify a state change URL to use to switch the state of the provider. This URL will
receive the providerState description from the pact file before each interaction via a POST. As for normal requests,
a request filter (`stateChangeRequestFilter`) can also be set to manipulate the request before it is sent.

```groovy
pact {

    serviceProviders {

        provider1 {

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
                stateChange = url('http://localhost:8001/tasks/pactStateChange')
                stateChangeUsesBody = false // defaults to true
                stateChangeRequestFilter = { req ->
                    // Add an authorization header to each request
                    req.addHeader('Authorization', 'OAUTH eyJhbGciOiJSUzI1NiIsImN0eSI6ImFw...')
                }
            }
            
            // or
            hasPactsWith('consumers') {
                pactFileLocation = file('path/to/pacts')                
                stateChange = url('http://localhost:8001/tasks/pactStateChange')
                stateChangeUsesBody = false // defaults to true
            }

        }

    }

}
```

If the `stateChangeUsesBody` is not specified, or is set to true, then the provider state description will be sent as
 JSON in the body of the request. If it is set to false, it will passed as a query parameter.

#### Teardown calls for state changes [version 3.2.5/2.4.7+]

You can enable teardown state change calls by setting the property `stateChangeTeardown = true` on the provider. This
will add an `action` parameter to the state change call. The setup call before the test will receive `action=setup`, and
then a teardown call will be made afterwards to the state change URL with `action=teardown`.

### Using a Closure [version 2.2.2+]

You can set a closure to be called before each verification with a defined provider state. The closure will be
called with the state description from the pact file.

```groovy
pact {

    serviceProviders {

        provider1 {

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
                // Load a fixture file based on the provider state and then setup some database
                // data. Does not require a state change request so returns false
                stateChange = { providerState ->
                    def fixture = loadFixtuerForProviderState(providerState)
                    setupDatabase(fixture)
                }
            }

        }

    }

}
```

#### Teardown calls for state changes [version 3.2.5/2.4.7+]

You can enable teardown state change calls by setting the property `stateChangeTeardown = true` on the provider. This
will add an `action` parameter to the state change closure call. The setup call before the test will receive `setup`,
as the second parameter, and then a teardown call will be made afterwards with `teardown` as the second parameter.

```groovy
pact {

    serviceProviders {

        provider1 {

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
                // Load a fixture file based on the provider state and then setup some database
                // data. Does not require a state change request so returns false
                stateChange = { providerState, action ->
                    if (action == 'setup') {
                      def fixture = loadFixtuerForProviderState(providerState)
                      setupDatabase(fixture)
                    } else {
                      cleanupDatabase()
                    }
                    false
                }
            }

        }

    }

}
```

## Filtering the interactions that are verified

You can filter the interactions that are run using three project properties: `pact.filter.consumers`, `pact.filter.description` and `pact.filter.providerState`.
Adding `-Ppact.filter.consumers=consumer1,consumer2` to the command line will only run the pact files for those
consumers (consumer1 and consumer2). Adding `-Ppact.filter.description=a request for payment.*` will only run those interactions
whose descriptions start with 'a request for payment'. `-Ppact.filter.providerState=.*payment` will match any interaction that
has a provider state that ends with payment, and `-Ppact.filter.providerState=` will match any interaction that does not have a
provider state.

## Verifying pact files from a pact broker [version 3.1.1+/2.3.1+]

You can setup your build to validate against the pacts stored in a pact broker. The pact gradle plugin will query
the pact broker for all consumers that have a pact with the provider based on its name.

For example:

```groovy
pact {

    serviceProviders {
        provider1 {
            hasPactsFromPactBroker('http://pact-broker:5000/')
        }
    }

}
```

This will verify all pacts found in the pact broker where the provider name is 'provider1'. If you need to set any
values on the consumers from the pact broker, you can add a Closure to configure them.

```groovy
pact {

    serviceProviders {
        provider1 {
            hasPactsFromPactBroker('http://pact-broker:5000/') { consumer ->
                 stateChange = { providerState -> /* state change code here */ true }
            }
        }
    }

}
```

**NOTE: Currently the pacts are fetched from the broker during the configuration phase of the build. This means that
if the broker is not available, you will not be able to run any Gradle tasks.** This should be fixed in a forth coming
release.

In the mean time, to only load the pacts when running the validate task, you can do something like:

```groovy
pact {

    serviceProviders {
        provider1 {
            // Only load the pacts from the broker if the start tasks from the command line include pactVerify
            if ('pactVerify' in gradle.startParameter.taskNames) {
                hasPactsFromPactBroker('http://pact-broker:5000/') { consumer ->
                     stateChange = { providerState -> /* state change code here */ true }
                }
            }
        }
    }

}
```

# Publishing pact files to a pact broker [version 2.2.7+]

The pact gradle plugin provides a `pactPublish` task that can publish all pact files in a directory
to a pact broker. To use it, you need to add a publish configuration to the pact configuration that defines the
directory where the pact files are and the URL to the pact broker.

For example:

```groovy
pact {

    publish {
        pactDirectory = '/pact/dir' // defaults to $buildDir/pacts
        pactBrokerUrl = 'http://pactbroker:1234'
    }

}
```

_NOTE:_ The pact broker requires a version for all published pacts. The `pactPublish` task will use the version of the
gradle project by default. Make sure you have set one otherwise the broker will reject the pact files.

_Version 3.2.2/2.4.3+_ you can override the version in the publish block.

# Verifying a message provider [version 2.2.12+]

The Gradle plugin has been updated to allow invoking test methods that can return the message contents from a message
producer. To use it, set the way to invoke the verification to `ANNOTATED_METHOD`. This will allow the pact verification
 task to scan for test methods that return the message contents.

Add something like the following to your gradle build file:

```groovy
pact {

    serviceProviders {

        messageProvider {

            verificationType = 'ANNOTATED_METHOD'
            packagesToScan = ['au.com.example.messageprovider.*'] // This is optional, but leaving it out will result in the entire
                                                                  // test classpath being scanned

            hasPactWith('messageConsumer') {
                pactFile = url('url/to/messagepact.json')
            }

        }

    }

}
```

Now when the `pactVerify` task is run, will look for methods annotated with `@PactVerifyProvider` in the test classpath
that have a matching description to what is in the pact file.

```groovy
class ConfirmationKafkaMessageBuilderTest {

  @PactVerifyProvider('an order confirmation message')
  String verifyMessageForOrder() {
      Order order = new Order()
      order.setId(10000004)
      order.setExchange('ASX')
      order.setSecurityCode('CBA')
      order.setPrice(BigDecimal.TEN)
      order.setUnits(15)
      order.setGst(new BigDecimal('15.0'))
      odrer.setFees(BigDecimal.TEN)

      def message = new ConfirmationKafkaMessageBuilder()
              .withOrder(order)
              .build()

      JsonOutput.toJson(message)
  }

}
```

It will then validate that the returned contents matches the contents for the message in the pact file.

## Publishing to the Gradle Community Portal

To publish the plugin to the community portal:

    $ ./gradlew :pact-jvm-provider-gradle_2.11:publishPlugins

# Verification Reports [versions 3.2.7/2.4.9+]

The default behaviour is to display the verification being done to the console, and pass or fail the build via the normal
Gradle mechanism. From versions 3.2.7/2.4.9+, additional reports can be generated from the verification.

## Enabling additional reports

The verification reports can be controlled by adding a reports section to the pact configuration in the gradle build file.

For example:

```groovy
pact {

    reports {
      defaultReports() // adds the standard console output

      markdown // report in markdown format
      json // report in json format
    }
}
```

Any report files will be written to "build/reports/pact".

## Additional Reports

The following report types are available in addition to console output (which is enabled by default):
`markdown`, `json`.
