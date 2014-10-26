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
        classpath 'au.com.dius:pact-jvm-provider-gradle_2.10:2.0.10'
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
  id "au.com.dius.pact" version "2.1.1"
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

                // currenty supports a file path using file() or a URL using url()
                pactFile = file('path/to/provider1-consumer1-pact.json')

            }

        }

    }

}
```

### 3. Execute `gradle pactVerify`

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

## Modifying the requests before they are sent

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The Pact Gradle plugin provides a request filter that can be
set to a closure on the provider that will be called before the request is made. This closure will receive a Map with
all the requests attributes defined on it.

```groovy
pact {

    serviceProviders {

        provider1 {

            requestFilter = { req ->
                // Add an authorization header to each request
                req.headers['Authorization] = 'OAUTH eyJhbGciOiJSUzI1NiIsImN0eSI6ImFw...'
            }

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
            }

        }

    }

}
```

## Project Properties

The following project properties can be specified with `-Pproperty=value` on the command line:

|Property|Description|
|--------|-----------|
|pact.showStacktrace|This turns on stacktrace printing for each request. It can help with diagnosing network errors|
|pact.filter.consumers|Comma seperated list of consumer names to verify|
|pact.filter.description|Only verify interactions whose description match the provided regular expression|
|pact.filter.providerState|Only verify interactions whose provider state match the provided regular expression. An empty string matches interactions that have no state|

## Provider States

For each provider you can specify a state change URL to use to switch the state of the provider. This URL will
receive the providerState description from the pact file before each interaction via a POST.

```
pact {

    serviceProviders {

        provider1 {

            hasPactWith('consumer1') {
                pactFile = file('path/to/provider1-consumer1-pact.json')
                stateChange = url('http://localhost:8001/tasks/pactStateChange')
                stateChangeUsesBody = false // defaults to true
            }

        }

    }

}
```

If the `stateChangeUsesBody` is not specified, or is set to true, then the provider state description will be sent as
 JSON in the body of the request. If it is set to false, it will passed as a query parameter.

## Filtering the interactions that are verified

You can filter the interactions that are run using two project properties: `pact.filter.consumers` and `pact.filter.interactions`.
Adding `-Ppact.filter.consumers=consumer1,consumer2` to the command line will only run the pact files for those
consumers (consumer1 and consumer2). Adding `-Ppact.filter.description=a request for payment.*` will only run those interactions
whose descriptions start with 'a request for payment'. `-Ppact.filter.providerState=.*payment` will match any interaction that
has a provider state that ends with payment, and `-Ppact.filter.providerState=` will match any interaction that does not have a
provider state.

## Publishing to the Gradle Community Portal

To publish the plugin to the community portal requires first publishing the plugin to jcenter via the bintray plugin.

    $ gradle :pact-jvm-provider-gradle_2.11:bintrayUpload

