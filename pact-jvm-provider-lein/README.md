# Leiningen plugin to verify a provider [version 2.2.14+, 3.0.3+]

Leiningen plugin for verifying pacts against a provider. The plugin provides a `pact-verify` task which will verify all
configured pacts against your provider.

## To Use It

### 1. Add the plugin to your project plugins, preferably in it's own profile.

```clojure
  :profiles {
             :pact {
                    :plugins [[au.com.dius/pact-jvm-provider-lein_2.11 "3.0.3" :exclusions [commons-logging]]]
                    :dependencies [[ch.qos.logback/logback-core "1.1.3"]
                                   [ch.qos.logback/logback-classic "1.1.3"]
                                   [org.apache.httpcomponents/httpclient "4.4.1"]]
                    }}}
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

## Modifying the HTTP Client Used

The default HTTP client is used for all requests to providers (created with a call to `HttpClients.createDefault()`).
This can be changed by specifying a function assigned to `:create-client` on the provider that returns a `CloseableHttpClient`.
The function will receive the provider info as a parameter.

## Plugin Properties

The following plugin options can be specified on the command line:

|Property|Description|
|--------|-----------|
|:pact.showStacktrace|This turns on stacktrace printing for each request. It can help with diagnosing network errors|
|:pact.filter.consumers|Comma seperated list of consumer names to verify|
|:pact.filter.description|Only verify interactions whose description match the provided regular expression|
|:pact.filter.providerState|Only verify interactions whose provider state match the provided regular expression. An empty string matches interactions that have no state|

Example, to run verification only for a particular consumer:

```
  $ lein with-profile pact pact-verify :pact.filter.consumers=consumer2
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

## Filtering the interactions that are verified

You can filter the interactions that are run using three properties: `:pact.filter.consumers`, `:pact.filter.description` and `:pact.filter.providerState`.
Adding `:pact.filter.consumers=consumer1,consumer2` to the command line will only run the pact files for those
consumers (consumer1 and consumer2). Adding `:pact.filter.description=a request for payment.*` will only run those interactions
whose descriptions start with 'a request for payment'. `:pact.filter.providerState=.*payment` will match any interaction that
has a provider state that ends with payment, and `:pact.filter.providerState=` will match any interaction that does not have a
provider state.
