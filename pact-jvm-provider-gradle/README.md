pact-jvm-provider-gradle
========================

Gradle plugin for verifying pacts against a provider.

The Gradle plugin creates a task `pactVerify` to your build which will verify all configured pacts against your provider.

## To Use It

### 1. Add the pact-jvm-provider-gradle jar file to your build script class path:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'au.com.dius:pact-jvm-provider-gradle_2.10:2.0.1'
    }
}
```

### 2. Apply the pact plugin

```groovy
apply plugin: 'pact'
```

### 3. Define the pacts between your consumers and providers

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

                // currenty supports a file path, but soon will support any URL
                pactFile = file('path/to/provider1-consumer1-pact.json')

            }

        }

    }

}
```

### 4. Execute `gradle pactVerify`

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

Following typical Gradle behaviour, you can set the provider task propeties to the actual tasks, or to the task names
as a string (for the case when they haven't been defined yet).
