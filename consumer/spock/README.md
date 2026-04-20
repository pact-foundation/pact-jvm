# pact-jvm-consumer-spock

Spock extension for writing Pact consumer tests in Groovy.

## Dependency

The library is available on Maven Central using:

* group-id = `au.com.dius.pact.consumer`
* artifact-id = `spock`
* version-id = `4.7.x`

### Gradle

```groovy
dependencies {
    testImplementation 'au.com.dius.pact.consumer:spock:4.7.x'
}
```

### Maven

```xml
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>spock</artifactId>
    <version>4.7.x</version>
    <scope>test</scope>
</dependency>
```

## Quick start

A Pact consumer Spock test has three parts:

1. Activate the extension with `@PactConsumerSpockTest` on the spec class.
2. Define a pact builder method annotated with `@Pact` that describes the expected interactions.
3. Write the feature method with `@PactSpecFor` that exercises your client code against the mock server.

```groovy
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.spock.PactConsumerSpockTest
import au.com.dius.pact.consumer.spock.PactSpecFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import spock.lang.Specification

@PactConsumerSpockTest
class ArticlesConsumerSpec extends Specification {

    MockServer mockServer   // injected by the extension before each pact test

    @Pact(provider = 'ArticlesService', consumer = 'ArticlesConsumer')
    RequestResponsePact articleList(PactDslWithProvider builder) {
        builder
            .given('articles exist')
            .uponReceiving('a request to list articles')
                .path('/articles')
                .method('GET')
            .willRespondWith()
                .status(200)
                .headers(['Content-Type': 'application/json'])
                .body('[{"id":1,"title":"Pact Testing"}]')
            .toPact()
    }

    @PactSpecFor(pactMethod = 'articleList', pactVersion = PactSpecVersion.V3)
    def 'fetches a list of articles'() {
        given:
        def client = new ArticlesClient(mockServer.url)

        when:
        def articles = client.list()

        then:
        articles.size() == 1
        articles[0].title == 'Pact Testing'
    }
}
```

After the test runs, a pact file is written to `build/pacts/ArticlesConsumer-ArticlesService.json`.

---

## Step-by-step guide

### 1. Activate the extension

Place `@PactConsumerSpockTest` on your spec class. This activates the Spock extension that manages
the mock server lifecycle and pact file writing for every `@PactSpecFor`-annotated feature.

```groovy
@PactConsumerSpockTest
class MyConsumerSpec extends Specification { ... }
```

### 2. Define the pact

Write a method annotated with `@Pact` that builds and returns the expected interactions. The method
must accept a `PactDslWithProvider` (for V3) or a `PactBuilder` (for V4) parameter and return the
corresponding pact type.

**V3 pact (RequestResponsePact):**

```groovy
@Pact(provider = 'UserService', consumer = 'MyConsumer')
RequestResponsePact getUser(PactDslWithProvider builder) {
    builder
        .given('user 42 exists')
        .uponReceiving('a request for user 42')
            .path('/users/42')
            .method('GET')
        .willRespondWith()
            .status(200)
            .headers(['Content-Type': 'application/json'])
            .body('{"id":42,"name":"Alice"}')
        .toPact()
}
```

**V4 pact (V4Pact):**

```groovy
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.V4Pact

@Pact(provider = 'UserService', consumer = 'MyConsumer')
V4Pact getUser(PactBuilder builder) {
    builder
        .expectsToReceiveHttpInteraction('a request for user 42') {
            it.withRequest { req -> req.path('/users/42').method('GET') }
              .willRespondWith { res -> res.status(200).body('{"id":42}') }
        }
        .toPact()
}
```

The `provider` and `consumer` values on `@Pact` set the names in the generated pact file. If `consumer` is
left blank, the system property `pact.consumer.name` is used.

### 3. Link the feature to a pact with `@PactSpecFor`

The `@PactSpecFor` annotation connects a feature method to the pact it should run against. Place it on
the feature method, or on the class to apply it to every feature (see [Class-level annotation](#class-level-pactTestFor)).

```groovy
@PactSpecFor(pactMethod = 'getUser', pactVersion = PactSpecVersion.V3)
def 'retrieves user details'() {
    when:
    def user = new UserClient(mockServer.url).getUser(42)
    then:
    user.name == 'Alice'
}
```

| Attribute | Description | Default |
|---|---|---|
| `pactMethod` | Name of the `@Pact`-annotated method to use | first `@Pact` method found |
| `providerName` | Provider name — used to look up `@Pact` methods by provider when `pactMethod` is not set | `""` |
| `pactVersion` | Pact spec version (`V3`, `V4`, etc.) | `UNSPECIFIED` → `V4` for the check |
| `hostInterface` | Host interface the mock server binds to | `localhost` |
| `port` | Fixed port for the mock server | random port |
| `https` | Start the mock server with HTTPS | `false` |
| `keyStorePath` / `keyStoreAlias` / `keyStorePassword` / `privateKeyPassword` | Custom KeyStore for HTTPS | (none) |
| `providerType` | `SYNCH` (HTTP), `ASYNCH` (messages), `SYNCH_MESSAGE` | `UNSPECIFIED` → `SYNCH` |

> **Pact spec version**: if your `@Pact` method returns `RequestResponsePact`, set
> `pactVersion = PactSpecVersion.V3`. If it returns `V4Pact`, use `PactSpecVersion.V4` or
> leave it unset (V4 is the default).

### 4. Receive the mock server

Declare a field of type `MockServer` on your spec class. The extension injects the running mock server
into this field before each pact feature method executes.

```groovy
@PactConsumerSpockTest
class MyConsumerSpec extends Specification {

    MockServer mockServer   // ← injected automatically

    ...
}
```

Use `mockServer.url` to point your client at the mock server:

```groovy
def client = new MyClient(mockServer.url)
```

---

## Class-level `@PactSpecFor`

If all features in a spec test the same provider, put `@PactSpecFor` on the class instead of each method.
Every feature method in the spec will then use that annotation. A method-level `@PactSpecFor` always
overrides the class-level one for that specific feature.

```groovy
@PactConsumerSpockTest
@PactSpecFor(providerName = 'UserService', pactMethod = 'userPact', pactVersion = PactSpecVersion.V3)
class UserServiceConsumerSpec extends Specification {

    MockServer mockServer

    @Pact(provider = 'UserService', consumer = 'MyConsumer')
    RequestResponsePact userPact(PactDslWithProvider builder) {
        builder
            .uponReceiving('a request for a user')
                .path('/users/1').method('GET')
            .willRespondWith()
                .status(200).body('{"id":1}')
            .toPact()
    }

    def 'first test uses the class-level annotation'() {
        expect:
        new SimpleHttp(mockServer.url).get('/users/1').statusCode == 200
    }

    def 'second test also uses the class-level annotation'() {
        expect:
        new SimpleHttp(mockServer.url).get('/users/1').statusCode == 200
    }
}
```

---

## Multiple providers in a single spec

Each feature method can test against a different provider by using distinct `@Pact` methods and
`@PactSpecFor` annotations. A separate mock server is started for each feature.

```groovy
@PactConsumerSpockTest
class MultiProviderSpec extends Specification {

    MockServer mockServer

    @Pact(provider = 'UserService', consumer = 'MyConsumer')
    RequestResponsePact userPact(PactDslWithProvider builder) {
        builder.uponReceiving('get user').path('/users/1').method('GET')
               .willRespondWith().status(200).body('{"id":1}')
               .toPact()
    }

    @Pact(provider = 'OrderService', consumer = 'MyConsumer')
    RequestResponsePact orderPact(PactDslWithProvider builder) {
        builder.uponReceiving('get order').path('/orders/99').method('GET')
               .willRespondWith().status(200).body('{"id":99}')
               .toPact()
    }

    @PactSpecFor(pactMethod = 'userPact', pactVersion = PactSpecVersion.V3)
    def 'calls the user service'() {
        expect:
        new SimpleHttp(mockServer.url).get('/users/1').statusCode == 200
    }

    @PactSpecFor(pactMethod = 'orderPact', pactVersion = PactSpecVersion.V3)
    def 'calls the order service'() {
        expect:
        new SimpleHttp(mockServer.url).get('/orders/99').statusCode == 200
    }
}
```

Two pact files are generated — one per provider.

---

## HTTPS mock server

Set `https = true` on `@PactSpecFor` to start the mock server over HTTPS. The mock server uses a
self-signed certificate, so your client code needs to be configured to accept self-signed certificates
(or trust-all).

```groovy
@PactSpecFor(pactMethod = 'securePact', pactVersion = PactSpecVersion.V3, https = true)
def 'calls the HTTPS endpoint'() {
    ...
}
```

To use your own KeyStore:

```groovy
@PactSpecFor(
    pactMethod = 'securePact',
    pactVersion = PactSpecVersion.V3,
    https = true,
    keyStorePath = 'path/to/keystore.jks',
    keyStoreAlias = 'mykey',
    keyStorePassword = 'changeit',
    privateKeyPassword = 'changeit'
)
def 'calls the HTTPS endpoint with custom cert'() {
    ...
}
```

---

## Verifying that all `@Pact` methods are exercised

After all features in a spec have run, the extension checks that every method annotated with `@Pact`
was called at least once. If any pact method was never used, the test suite fails with an `AssertionError`
listing the unused methods.

To mark a `@Pact` method as work in progress (so it does not trigger this check), add Spock's `@Ignore`
to the corresponding feature method:

```groovy
@Pact(provider = 'FutureService', consumer = 'MyConsumer')
RequestResponsePact upcomingFeature(PactDslWithProvider builder) { ... }

@Ignore
@PactSpecFor(pactMethod = 'upcomingFeature', pactVersion = PactSpecVersion.V3)
def 'upcoming feature not yet implemented'() { ... }
```

---

## Pact file location

By default pact files are written to:

* `build/pacts` when building with Gradle
* `target/pacts` when building with Maven

### Override with a system property

```groovy
// build.gradle
test {
    systemProperties['pact.rootDir'] = "$buildDir/custom-pacts"
}
```

### Override with `@PactDirectory`

Annotate the spec class with `@PactDirectory` to write pacts for that spec to a specific directory:

```groovy
import au.com.dius.pact.core.model.annotations.PactDirectory

@PactConsumerSpockTest
@PactDirectory('src/test/resources/pacts')
class MyConsumerSpec extends Specification { ... }
```

### Force pact files to be overwritten

By default, pact files are merged with any existing file. To overwrite instead, set:

```groovy
// build.gradle
test {
    systemProperties['pact.writer.overwrite'] = 'true'
}
```

---

## Provider state expressions (V3+)

You can inject values returned from provider state callbacks into paths, headers, query parameters,
and body fields. Use the `fromProviderState` methods on the DSL:

```groovy
builder
    .given('user exists')
    .uponReceiving('a request for the created user')
        .pathFromProviderState('/users/${id}', '/users/100')
        .method('GET')
    .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().valueFromProviderState('id', 'id', 100))
    .toPact()
```

---

## Test analytics

Anonymous analytics are collected to track JVM version, OS, and usage statistics. To opt out, set the
`pact_do_not_track` system property or environment variable to `true`:

```groovy
// build.gradle
test {
    systemProperties['pact_do_not_track'] = 'true'
}
```

---

## Relationship to other Pact consumer modules

| Module | Use when |
|---|---|
| `consumer:spock` | Writing consumer tests in Groovy with Spock |
| `consumer:junit5` | Writing consumer tests in Java/Kotlin/Groovy with JUnit 5 |
| `consumer:junit` | Writing consumer tests in Java/Kotlin/Groovy with JUnit 4 |
| `consumer:groovy` | Using the Groovy `PactBuilder` DSL directly (framework-agnostic) |
| `consumer:kotlin` | Writing consumer tests in Kotlin with a Kotlin DSL |
