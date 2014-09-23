pact-jvm-consumer-specs2
========================

## Specs2 Bindings for the pact-jvm library

## Dependency

In the root folder of your project in build.sbt add the line:
```
libraryDependencies += "au.com.dius" %% "pact-jvm-consumer-specs2" % "2.0.6"
```

## Usage

To author a test, extend `PactSpec`

Here is a simple example:

```
import au.com.dius.pact.consumer.PactSpec

class ExamplePactSpec extends PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(body = "{}")
  .during { providerConfig =>
    ConsumerService(providerConfig.url).simpleGet("/foo") must beEqualTo(200, Some("{}")).await
  }
}

```

This spec will be run along with the rest of your specs2 unit tests and will output your pact json to

```
/target/pacts/<Consumer>_<Provider>.json
```
