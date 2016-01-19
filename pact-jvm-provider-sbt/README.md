Pact sbt plugin
===============

The sbt plugin adds an sbt task for running all provider pacts against a running server.

To use the pact sbt plugin, add the following to your project/plugins.sbt

    addSbtPlugin("au.com.dius" %% "pact-jvm-provider-sbt" % "2.4.4")

## Using the old verifyPacts task

The pact plugin adds a task called `verifyPacts`. To use it you need to add the following to your build.sbt

    PactJvmPlugin.pactSettings

Two new keys are added to configure this task:

`pactConfig` is the location of your pact-config json file (defaults to "pact-config.json" in the classpath root)

`pactRoot` is the root folder of your pact json files (defaults to "pacts"), all .json files in root and sub folders will be executed

## Using the newer task [version 2.4.4+]

The pact SBT is being updated to bring it inline with the functionality available in the other build plugins. A new
task is added called `pactVerify`. To use it, add config to your build.sbt that configures `SbtProviderPlugin.config`
with the providers and consumers.

For example:

```scala
import au.com.dius.pact.provider.sbt._
// This defines a single provider and two consumers. The pact files are stored in the src/test/resources directory.
SbtProviderPlugin.config ++ Seq(
  providers := Seq(
    ProviderConfig(name = "Our Service", port = 5050)
        .hasPactWith(ConsumerConfig(name = "sampleconsumer", pactFile = file("src/test/resources/sample-pact.json")))
        .hasPactWith(ConsumerConfig(name = "sampleconsumer2", pactFile = file("src/test/resources/sample-pact2.json")))
  )
)
```

and then execute `verifyPacts`.

### Enabling insecure SSL

For providers that are running on SSL with self-signed certificates, you need to enable insecure SSL mode by setting
`insecure` to true on the provider.

```scala
    ProviderConfig(name = "Our Service", protocol = "https", insecure = true)
```

### Specifying a custom trust store

For environments that are running their own certificate chains:

```scala
    ProviderConfig(name = "Our Service", protocol = "https", trustStore = file("relative/path/to/trustStore.jks"),
        trustStorePassword = "securePassword")
```

`trustStore` is relative to the current working directory. `trustStorePassword` defaults to `changeme`.

NOTE: The hostname will still be verified against the certificate.

### Provider States

For each provider you can specify a state change URL to use to switch the state of the provider. This URL will
receive the providerState description from the pact file before each interaction via a POST. The stateChangeUsesBody
controls if the state is passed in the request body or as a query parameter.

These values can be set at the provider level, or for a specific consumer. Consumer values take precedent if both are given.

```scala
        ProviderConfig(name = "Our Service", stateChangeUrl = Some(new java.net.URL("http://localhost:8080/tasks/pactStateChange")))
```

If the `stateChangeUsesBody` value is not specified, or is set to true, then the provider state description will be sent as
 JSON in the body of the request. If it is set to false, it will passed as a query parameter.

### Verifying all pact files in a directory for a provider

You can specify a directory that contains pact files, and the Pact plugin will scan for all pact files that match that
provider and define a consumer for each pact file in the directory. Consumer name is read from contents of pact file.

For example:

```scala
import au.com.dius.pact.provider.sbt._
// This defines a single provider and all the consumers from the src/test/resources directory.
SbtProviderPlugin.config ++ Seq(
  providers := Seq(
    ProviderConfig(name = "Our Service", port = 5050)
        .hasPactsInDirectory(file("src/test/resources")))
)
```

The `hasPactsInDirectory` has the following optional parameters:

| Parameter Name | Parameter Type | Default | Description |
|----------------|----------------|-------- | ------------|
| stateChange | Option[URL] | None | State change URL |
| stateChangeUsesBody | Boolean | false | If state is passed in the body or query parameters |
| verificationType | PactVerification | PactVerification.REQUST_RESPONSE | Whether the provider interacts via request/response or messages |
| packagesToScan | List[String] | List() | Packages to scan for implementations for message pacts |

These will be applied to all consumers configured from the files in the directory.

