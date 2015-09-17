pact-jvm-consumer-specs2
========================

## Specs2 Bindings for the pact-jvm library

## Dependency

In the root folder of your project in build.sbt add the line:

```scala
libraryDependencies += "au.com.dius" %% "pact-jvm-consumer-specs2" % "3.0.4"
```

or if you are using Gradle:

```groovy
dependencies {
    testCompile "au.com.dius:pact-jvm-consumer-specs2:3.0.4"
}

```

*Note:* `PactSpec` requires spec2 3.x

## Usage

To author a test, mix `PactSpec` into your spec

Here is a simple example:

```
import au.com.dius.pact.consumer.PactSpec

class ExamplePactSpec extends Specification with PactSpec {

  val consumer = "My Consumer"
  val provider = "My Provider"

  override def is = uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(body = "{}")
    .withConsumerTest { providerConfig =>
      Await.result(ConsumerService(providerConfig.url).simpleGet("/foo"), Duration(1000, MILLISECONDS)) must beEqualTo(200, Some("{}"))
    }
}

```

This spec will be run along with the rest of your specs2 unit tests and will output your pact json to

```
/target/pacts/<Consumer>_<Provider>.json
```
