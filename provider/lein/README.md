# Leiningen plugin to verify a provider

Leiningen plugin for verifying pacts against a provider. The plugin provides a `pact-verify` task which will verify all
configured pacts against your provider.

## To Use It

### 1. Add the plugin to your project plugins, preferably in it's own profile.

```clojure
  :profiles {
             :pact {
                    :plugins [[au.com.dius.pact.provider/lein "4.1.20" :exclusions [commons-logging]]]
                    :dependencies [[ch.qos.logback/logback-core "1.2.3"]
                                   [ch.qos.logback/logback-classic "1.2.3"]]
                    }}
```

### 2. Define the pacts between your consumers and providers

You define all the providers and consumers within the `:pact` configuration element of your project.

```clojure
  :pact {
      :service-providers {
          ; You can define as many as you need, but each must have a unique name
          :provider1 {
              ; All the provider properties are optional, and have sensible defaults (shown below)
              :protocol "http"
              :host "localhost"
              :port 8080
              :path "/"

              :has-pact-with {
                  ; Again, you can define as many consumers for each provider as you need, but each must have a unique name
                  :consumer1 {
                    ; pact file can be either a path or an URL
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

### 3. Execute `lein with-profile pact pact-verify`

You will have to have your provider running for this to pass.

## Enabling insecure SSL

For providers that are running on SSL with self-signed certificates, you need to enable insecure SSL mode by setting
`:insecure true` on the provider.

```clojure
  :pact {
      :service-providers {
          :provider1 {
              :protocol "https"
              :host "localhost"
              :port 8443
              :insecure true

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

## Specifying a custom trust store

For environments that are running their own certificate chains:

```clojure
  :pact {
      :service-providers {
          :provider1 {
              :protocol "https"
              :host "localhost"
              :port 8443
              :trust-store "relative/path/to/trustStore.jks"
              :trust-store-password "changeme"

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

`:trust-store` is relative to the current working (build) directory. `:trust-store-password` defaults to `changeit`.

NOTE: The hostname will still be verified against the certificate.

## Modifying the requests before they are sent

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The Leiningen plugin provides a request filter that can be
set to an anonymous function on the provider that will be called before the request is made. This function will receive the HttpRequest
object as a parameter.

```clojure
  :pact {
      :service-providers {
          :provider1 {
              ; function that adds an Authorization header to each request
              :request-filter #(.addHeader % "Authorization" "oauth-token eyJhbGciOiJSUzI1NiIsIm...")

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

__*Important Note:*__ You should only use this feature for things that can not be persisted in the pact file. By modifying
the request, you are potentially modifying the contract from the consumer tests!

## Modifying the HTTP Client Used

The default HTTP client is used for all requests to providers (created with a call to `HttpClients.createDefault()`).
This can be changed by specifying a function assigned to `:create-client` on the provider that returns a `CloseableHttpClient`.
The function will receive the provider info as a parameter.

## Turning off URL decoding of the paths in the pact file

By default the paths loaded from the pact file will be decoded before the request is sent to the provider. To turn this
behaviour off, set the system property `pact.verifier.disableUrlPathDecoding` to `true`.

__*Important Note:*__ If you turn off the url path decoding, you need to ensure that the paths in the pact files are 
correctly encoded. The verifier will not be able to make a request with an invalid encoded path.

## Plugin Properties

The following plugin options can be specified on the command line:

|Property|Description|
|--------|-----------|
|:pact.showStacktrace|This turns on stacktrace printing for each request. It can help with diagnosing network errors|
|:pact.showFullDiff|This turns on displaying the full diff of the expected versus actual bodies|
|:pact.filter.consumers|Comma seperated list of consumer names to verify|
|:pact.filter.description|Only verify interactions whose description match the provided regular expression|
|:pact.filter.providerState|Only verify interactions whose provider state match the provided regular expression. An empty string matches interactions that have no state|
|:pact.verifier.publishResults|Publishing of verification results will be skipped unless this property is set to 'true'|

Example, to run verification only for a particular consumer:

```
  $ lein with-profile pact pact-verify :pact.filter.consumers=:consumer2
```

## Provider States

For each provider you can specify a state change URL to use to switch the state of the provider. This URL will
receive the `providerState` description from the pact file before each interaction via a POST. The `:state-change-uses-body`
controls if the state is passed in the request body or as a query parameter.

These values can be set at the provider level, or for a specific consumer. Consumer values take precedent if both are given.

```clojure
  :pact {
      :service-providers {
          :provider1 {
              :state-change-url "http://localhost:8080/tasks/pactStateChange"
              :state-change-uses-body false ; defaults to true

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

If the `:state-change-uses-body` is not specified, or is set to true, then the provider state description will be sent as
 JSON in the body of the request. If it is set to false, it will passed as a query parameter.

As for normal requests (see Modifying the requests before they are sent), a state change request can be modified before
it is sent. Set `:state-change-request-filter` to an anonymous function on the provider that will be called before the request is made.

#### Returning values that can be injected

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

There are methods on the consumer DSLs that can provider an expression that contains variables (like '/api/user/${id}'
for the path). The provider state callback can then return a map for values, and the `id` attribute from the map will
be expanded in the expression. For URL callbacks, the values need to be returned as JSON in the response body.

## Filtering the interactions that are verified

You can filter the interactions that are run using three properties: `:pact.filter.consumers`, `:pact.filter.description` and `:pact.filter.providerState`.
Adding `:pact.filter.consumers=:consumer1,:consumer2` to the command line will only run the pact files for those
consumers (consumer1 and consumer2). Adding `:pact.filter.description=a request for payment.*` will only run those interactions
whose descriptions start with 'a request for payment'. `:pact.filter.providerState=.*payment` will match any interaction that
has a provider state that ends with payment, and `:pact.filter.providerState=` will match any interaction that does not have a
provider state.

## Starting and shutting down your provider

For the pact verification to run, the provider needs to be running. Leiningen provides a `do` task that can chain tasks
together. So, by creating a `start-app` and `terminate-app` alias, you could so something like:

    $ lein with-profile pact do start-app, pact-verify, terminate-app

However, if the pact verification fails the build will abort without running the `terminate-app` task. To have the
start and terminate tasks always run regardless of the state of the verification, you can assign them to `:start-provider-task`
and `:terminate-provider-task` on the provider.

```clojure

  :aliases {"start-app" ^{:doc "Starts the app"}
                          ["tasks to start app ..."] ; insert tasks to start the app here

            "terminate-app" ^{:doc "Kills the app"}
                          ["tasks to terminate app ..."] ; insert tasks to stop the app here
            }

  :pact {
      :service-providers {
          :provider1 {
              :start-provider-task "start-app"
              :terminate-provider-task "terminate-app"

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```

Then you can just run:

    $ lein with-profile pact pact-verify

and the `start-app` and `terminate-app` tasks will run before and after the provider verification.

## Specifying the provider hostname at runtime

If you need to calculate the provider hostname at runtime (for instance it is run as a new docker container or
AWS instance), you can give an anonymous function as the provider host that returns the host name. The function
will receive the provider information as a parameter.

```clojure

  :pact {
      :service-providers {
          :provider1 {
              :host #(calculate-host-name %)

              :has-pact-with {
                  :consumer1 {
                    :pact-file "path/to/provider1-consumer1-pact.json"
                  }
              }
          }
      }
  }
```
