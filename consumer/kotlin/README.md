# Kotlin consumer DSL for Pact-JVM

An idiomatic Kotlin DSL for writing consumer-side Pact contract tests. Wraps the
[`consumer`](../README.md) module's V4 Pact builder with Kotlin lambda receivers, `@DslMarker`
scoping, and named-parameter entry points.

## Gradle dependency

```groovy
testImplementation("au.com.dius.pact.consumer:kotlin:4.7.1")
```

## Quick start

Add these imports at the top of your file:
`import au.com.dius.pact.consumer.kotlin.pact`,
`import au.com.dius.pact.consumer.kotlin.runConsumerTest`,
`import au.com.dius.pact.consumer.dsl.newJsonObject`

```kotlin
val myPact = pact(consumer = "MyConsumer", provider = "MyProvider") {
    uponReceiving("a request for a user") {
        given("user 1 exists")
        withRequest {
            method("GET")
            path("/api/users/1")
            header("Accept", "application/json")
        }
        willRespondWith {
            status(200)
            header("Content-Type", "application/json")
            body(newJsonObject {
                stringType("id", "1")
                stringType("name", "Alice")
                numberType("age", 30)
            })
        }
    }
}

val result = runConsumerTest(myPact) {
    // `this` is MockServer — call getUrl() for the base URL
    val response = URL("${getUrl()}/api/users/1").readText()
    // make assertions here
}
```

The Pact file is written to `build/pacts/` on success.

---

## BDD style

The BDD style mirrors the Given/When/Then pattern. Provider states are declared with `given` at
the top level of the `pact` block (before the interaction), and the interaction is introduced
with `uponReceiving`.

```kotlin
val pact = pact(consumer = "OrderService", provider = "ProductService") {

    // -- interaction 1 --
    given("products exist")
    uponReceiving("a request for all products") {
        withRequest {
            method("GET")
            path("/products")
        }
        willRespondWith {
            status(200)
            body(newJsonArray {
                newObject {
                    stringType("id", "abc-123")
                    stringType("name", "Widget")
                    numberType("price", 9.99)
                }
            })
        }
    }

    // -- interaction 2: parametrised provider state --
    given("product with id 'abc-123' exists")
    uponReceiving("a request for a single product") {
        withRequest {
            method("GET")
            path("/products/abc-123")
        }
        willRespondWith {
            status(200)
            body(newJsonObject {
                stringType("id", "abc-123")
                stringType("name", "Widget")
            })
        }
    }

    // -- interaction 3: not found --
    given("no products exist")
    uponReceiving("a request for a product that does not exist") {
        withRequest {
            method("GET")
            path("/products/missing")
        }
        willRespondWith {
            status(404)
        }
    }
}
```

`given` at the pact level applies to the **next** `uponReceiving` block. It can be called
multiple times to attach multiple provider states to one interaction:

```kotlin
given("users exist")
given("the database is healthy")
uponReceiving("a request that needs two states") {
    withRequest { method("GET"); path("/api/data") }
    willRespondWith { status(200) }
}
```

---

## Declarative style

The declarative style uses `interaction` as the entry point and declares provider states
**inside** the interaction block with `given`. This keeps everything about one interaction
co-located:

```kotlin
val pact = pact(consumer = "OrderService", provider = "ProductService") {

    interaction("get all products") {
        given("products exist")
        withRequest {
            method("GET")
            path("/products")
        }
        willRespondWith {
            status(200)
            body(newJsonArray {
                newObject {
                    stringType("id", "abc-123")
                    stringType("name", "Widget")
                    numberType("price", 9.99)
                }
            })
        }
    }

    interaction("get a single product") {
        given("product with id 'abc-123' exists", "id" to "abc-123")
        withRequest {
            method("GET")
            path("/products/abc-123")
        }
        willRespondWith {
            status(200)
            body(newJsonObject {
                stringType("id", "abc-123")
                stringType("name", "Widget")
            })
        }
    }
}
```

`uponReceiving` and `interaction` are aliases — you can mix them freely.

---

## Request configuration

The `withRequest` block receives an
[`HttpRequestBuilder`](../src/main/kotlin/au/com/dius/pact/consumer/dsl/HttpRequestBuilder.kt)
as its receiver.

### Method and path

```kotlin
withRequest {
    method("POST")
    path("/api/orders")
}
```

Match any path by regex (with an example):

```kotlin
withRequest {
    method("GET")
    path(regexp("\\/products\\/[0-9]+", "/products/1"))
}
```

### Headers

```kotlin
withRequest {
    method("GET")
    path("/api/data")
    header("Accept", "application/json")
    header("Authorization", regexp("Bearer .+", "Bearer token123"))
}
```

### Query parameters

```kotlin
withRequest {
    method("GET")
    path("/api/users")
    queryParameter("page", "1")
    queryParameter("size", "20")
    queryParameter("sort", regexp("[a-z]+,(asc|desc)", "name,asc"))
}
```

### Request body

Plain string (content type is inferred or defaults to `text/plain`):

```kotlin
withRequest {
    method("POST")
    path("/api/echo")
    body("hello")
}
```

JSON string with explicit content type:

```kotlin
withRequest {
    method("POST")
    path("/api/orders")
    body("""{"productId": "abc-123", "quantity": 2}""", "application/json")
}
```

JSON body with matchers (see [Body matchers](#body-matchers)):

```kotlin
withRequest {
    method("POST")
    path("/api/orders")
    body(newJsonObject {
        stringType("productId", "abc-123")
        numberType("quantity", 1)
    })
}
```

---

## Response configuration

The `willRespondWith` block receives an
[`HttpResponseBuilder`](../src/main/kotlin/au/com/dius/pact/consumer/dsl/HttpResponseBuilder.kt)
as its receiver.

### Status codes

Exact code:

```kotlin
willRespondWith { status(201) }
```

Semantic matchers (the example value is set automatically):

```kotlin
willRespondWith { successStatus() }       // matches 200–299
willRespondWith { clientErrorStatus() }   // matches 400–499
willRespondWith { serverErrorStatus() }   // matches 500–599
willRespondWith { nonErrorStatus() }      // matches < 400
willRespondWith { errorStatus() }         // matches >= 400
willRespondWith { redirectStatus() }      // matches 300–399
willRespondWith { informationStatus() }   // matches 100–199
willRespondWith { statusCodes(listOf(200, 201, 204)) }
```

### Response headers

```kotlin
willRespondWith {
    status(200)
    header("Content-Type", "application/json")
    header("X-Request-Id", uuid())
}
```

Match a Set-Cookie header by regex:

```kotlin
willRespondWith {
    status(200)
    matchSetCookie("session", "[a-z0-9]+", "abc123")
}
```

### Response body

See [Body matchers](#body-matchers) below.

---

## Body matchers

Body matchers use the DSL functions from the `consumer` module. Import them alongside the
Kotlin DSL:

```text
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newObject   // extension on LambdaDslJsonArray
import au.com.dius.pact.consumer.dsl.newArray    // extension on LambdaDslObject / LambdaDslJsonArray
```

### JSON objects

```kotlin
body(newJsonObject {
    stringType("name", "Alice")          // any string; example "Alice"
    numberType("age", 30)                // any number
    integerType("score", 100)            // any integer
    decimalType("balance", 9.99)         // any decimal
    booleanType("active", true)          // any boolean
    datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss")
    date("dob", "yyyy-MM-dd")
    time("startTime", "HH:mm")
    uuid("id")
    stringMatcher("postcode", "[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}", "SW1A 1AA")
    nullValue("deletedAt")
})
```

### Nested objects

```kotlin
body(newJsonObject {
    stringType("name", "Alice")
    `object`("address") {
        stringType("street", "123 Main St")
        stringType("city", "Springfield")
    }
})
```

Or using the Kotlin-friendly extension (avoids the backtick); import
`au.com.dius.pact.consumer.dsl.newObject` alongside the other DSL imports:

```kotlin
body(newJsonObject {
    stringType("name", "Alice")
    newObject("address") {
        stringType("street", "123 Main St")
        stringType("city", "Springfield")
    }
})
```

### JSON arrays

An array of objects where each element matches the same structure:

```kotlin
body(newJsonArray {
    newObject {
        stringType("id", "abc-123")
        stringType("name", "Widget")
    }
})
```

A nested array inside an object:

```kotlin
body(newJsonObject {
    stringType("name", "Alice")
    newArray("roles") {
        newObject {
            stringType("name", "admin")
        }
    }
})
```

### Minimum/maximum array length

```kotlin
body(newJsonObject {
    minArrayLike("tags", 1) {
        stringType("value", "kotlin")
    }
    maxArrayLike("aliases", 5) {
        stringType("value", "ali")
    }
})
```

### Path/header/query matchers

Use `PM` (Pact Matcher) functions when attaching matchers to path, header, or query parameter
values instead of a JSON body. Import `au.com.dius.pact.consumer.dsl.Matchers.regexp` (and
similar) or use the `PM` object:

```kotlin
withRequest {
    method("GET")
    path(regexp("\\/orders\\/[0-9]+", "/orders/42"))
    header("X-Trace-Id", PM.uuid())
    queryParameter("status", PM.stringMatcher("active|pending", "active"))
}
```

---

## Provider states

### No parameters

```kotlin
given("users exist")
```

### Key/value parameters

```kotlin
given("a user exists", "id" to "42", "role" to "admin")
```

Or pass a map:

```kotlin
given("a user exists", mapOf("id" to "42", "role" to "admin"))
```

### Multiple states

Call `given` multiple times to attach more than one state to an interaction:

```kotlin
interaction("a privileged request") {
    given("user 42 exists")
    given("user 42 has admin role")
    withRequest { method("GET"); path("/api/admin") }
    willRespondWith { status(200) }
}
```

---

## Pending interactions

A pending interaction will not fail provider verification if the provider hasn't implemented it
yet. Useful during API-design-first workflows:

```kotlin
interaction("a future endpoint") {
    pending(true)
    withRequest {
        method("GET")
        path("/api/v2/future")
    }
    willRespondWith {
        status(200)
    }
}
```

---

## Running tests with a mock server

`runConsumerTest` starts a local mock HTTP server, runs the test block, validates that all
expected interactions were called, then writes the Pact file.

```kotlin
val result = runConsumerTest(myPact) {
    // `this` is MockServer
    val baseUrl = getUrl()

    val connection = URL("$baseUrl/api/users/1").openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", "application/json")
    assertThat(connection.responseCode, equalTo(200))
}

assertThat(result, instanceOf(PactVerificationResult.Ok::class.java))
```

### Custom mock server configuration

```kotlin
val result = runConsumerTest(
    pact = myPact,
    config = MockProviderConfig.createDefault(PactSpecVersion.V4)
) {
    // test block
}
```

### JUnit 5 integration

For annotation-driven tests without manual `runConsumerTest` calls, use the
[`consumer:junit5`](../junit5/README.md) module alongside this one. The `@PactConsumerTest` and
`@Pact` annotations work with `V4Pact` objects produced by this DSL:

```text
@PactConsumerTest
@PactTestFor(providerName = "ProductService")
class ProductServiceConsumerTest {

    @Pact(consumer = "OrderService")
    fun getProductPact(builder: PactBuilder): V4Pact =
        pact(consumer = "OrderService", provider = "ProductService") {
            uponReceiving("a request for a product") {
                withRequest { method("GET"); path("/products/1") }
                willRespondWith { status(200) }
            }
        }

    @Test
    @PactTestFor(pactMethod = "getProductPact")
    fun `should get a product`(mockServer: MockServer) {
        val response = URL("${mockServer.getUrl()}/products/1").readText()
        // assertions
    }
}
```

---

## Pact file location

By default, Pact files are written to `build/pacts/`. Override with the system property:

```
-Dpact.rootDir=/path/to/pacts
```

Or in your Gradle test configuration:

```gradle
tasks.test {
    systemProperty("pact.rootDir", layout.buildDirectory.dir("pacts").get().asFile.path)
}
```
