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
