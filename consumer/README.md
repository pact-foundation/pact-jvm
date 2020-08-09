Pact consumer
=============

Pact Consumer is used by projects that are consumers of an API.

Most projects will want to use pact-consumer via one of the test framework specific projects. If your favourite
framework is not implemented, this module should give you all the hooks you need.

Provides a DSL for use with Java to build consumer pacts.

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius.pact`
* artifact-id = `consumer`

## DSL Usage

Example in a JUnit test:

```java
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.junit.Assert.assertEquals;

public class PactTest {

  @Test
  public void testPact() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving("a request to say Hello")
      .path("/hello")
      .method("POST")
      .body("{\"name\": \"harry\"}")
      .willRespondWith()
      .status(200)
      .body("{\"hello\": \"harry\"}")
      .toPact();

    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun() {
      @Override
      public void run(@NotNull MockServer mockServer) throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("hello", "harry");
        assertEquals(expectedResponse, new ConsumerClient(mockServer.getUrl()).post("/hello",
            "{\"name\": \"harry\"}", ContentType.APPLICATION_JSON));
      }
    });

    if (result instanceof PactVerificationResult.Error) {
      throw new RuntimeException(((PactVerificationResult.Error)result).getError());
    }

    assertEquals(PactVerificationResult.Ok.INSTANCE, result);
  }

}
```

The DSL has the following pattern:

```java
.consumer("Some Consumer")
.hasPactWith("Some Provider")
.given("a certain state on the provider")
    .uponReceiving("a request for something")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .uponReceiving("another request for something")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .
    .
    .
.toPact()
```

You can define as many interactions as required. Each interaction starts with `uponReceiving` followed by `willRespondWith`.
The test state setup with `given` is a mechanism to describe what the state of the provider should be in before the provider
is verified. It is only recorded in the consumer tests and used by the provider verification tasks.

### Building JSON bodies with PactDslJsonBody DSL

The body method of the ConsumerPactBuilder can accept a PactDslJsonBody, which can construct a JSON body as well as
define regex and type matchers.

For example:

```java
PactDslJsonBody body = new PactDslJsonBody()
    .stringType("name")
    .booleanType("happy")
    .hexValue("hexCode")
    .id()
    .ipAddress("localAddress")
    .numberValue("age", 100)
    .timestamp();
```

#### DSL Matching methods

The following matching methods are provided with the DSL. In most cases, they take an optional value parameter which
will be used to generate example values (i.e. when returning a mock response). If no example value is given, a random
one will be generated.

| method | description |
|--------|-------------|
| string, stringValue | Match a string value (using string equality) |
| number, numberValue | Match a number value (using Number.equals)\* |
| booleanValue | Match a boolean value (using equality) |
| stringType | Will match all Strings |
| numberType | Will match all numbers\* |
| integerType | Will match all numbers that are integers (both ints and longs)\* |
| decimalType | Will match all real numbers (floating point and decimal)\* |
| booleanType | Will match all boolean values (true and false) |
| stringMatcher | Will match strings using the provided regular expression |
| timestamp | Will match string containing timestamps. If a timestamp format is not given, will match an ISO timestamp format |
| date | Will match string containing dates. If a date format is not given, will match an ISO date format |
| time | Will match string containing times. If a time format is not given, will match an ISO time format |
| ipAddress | Will match string containing IP4 formatted address. |
| id | Will match all numbers by type |
| hexValue | Will match all hexadecimal encoded strings |
| uuid | Will match strings containing UUIDs |
| includesStr | Will match strings containing the provided string |
| equalsTo | Will match using equals |
| matchUrl | Defines a matcher for URLs, given the base URL path and a sequence of path fragments. The path fragments could be
             strings or regular expression matchers |

_\* Note:_ JSON only supports double precision floating point values. Depending on the language implementation, they
may parsed as integer, floating point or decimal numbers.

#### Ensuring all items in a list match an example

Lots of the time you might not know the number of items that will be in a list, but you want to ensure that the list
has a minimum or maximum size and that each item in the list matches a given example. You can do this with the `arrayLike`,
`minArrayLike` and `maxArrayLike` functions.

| function | description |
|----------|-------------|
| `eachLike` | Ensure that each item in the list matches the provided example |
| `maxArrayLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `minArrayLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
    DslPart body = new PactDslJsonBody()
        .minArrayLike("users")
            .id()
            .stringType("name")
        .closeObject()
        .closeArray();
```

This will ensure that the users list is never empty and that each user has an identifier that is a number and a name that is a string.


#### Matching JSON values at the root

For cases where you are expecting basic JSON values (strings, numbers, booleans and null) at the root level of the body
and need to use matchers, you can use the `PactDslJsonRootValue` class. It has all the DSL matching methods for basic
values that you can use.

For example:

```java
.consumer("Some Consumer")
.hasPactWith("Some Provider")
    .uponReceiving("a request for a basic JSON value")
        .path("/hello")
    .willRespondWith()
        .status(200)
        .body(PactDslJsonRootValue.integerType())
```

#### Root level arrays that match all items

If the root of the body is an array, you can create PactDslJsonArray classes with the following methods:

| function | description |
|----------|-------------|
| `arrayEachLike` | Ensure that each item in the list matches the provided example |
| `arrayMinLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `arrayMaxLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
PactDslJsonArray.arrayEachLike()
    .date("clearedDate", "mm/dd/yyyy", date)
    .stringType("status", "STATUS")
    .decimalType("amount", 100.0)
.closeObject()
```

This will then match a body like:

```json
[ {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {

  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
} ]
```

#### Matching arrays of arrays

For the case where you have arrays of arrays (GeoJSON is an example), the following methods have been provided:

| function | description |
|----------|-------------|
| `eachArrayLike` | Ensure that each item in the array is an array that matches the provided example |
| `eachArrayWithMaxLike` | Ensure that each item in the array is an array that matches the provided example and the array is no bigger than the provided max |
| `eachArrayWithMinLike` | Ensure that each item in the array is an array that matches the provided example and the array is no smaller than the provided min |

For example (with GeoJSON structure):

```java
new PactDslJsonBody()
  .stringType("type","FeatureCollection")
  .eachLike("features")
    .stringType("type","Feature")
    .object("geometry")
      .stringType("type","Point")
      .eachArrayLike("coordinates") // coordinates is an array of arrays 
        .decimalType(-7.55717)
        .decimalType(49.766896)
      .closeArray()
      .closeArray()
    .closeObject()
    .object("properties")
      .stringType("prop0","value0")
    .closeObject()
  .closeObject()
  .closeArray()
```

This generated the following JSON:

```json
{
  "features": [
    {
      "geometry": {
        "coordinates": [[-7.55717, 49.766896]],
        "type": "Point"
      },
      "type": "Feature",
      "properties": { "prop0": "value0" }
    }
  ],
  "type": "FeatureCollection"
}
```

and will be able to match all coordinates regardless of the number of coordinates.

#### Matching any key in a map

The DSL has been extended for cases where the keys in a map are IDs. For an example of this, see 
[#313](https://github.com/DiUS/pact-jvm/issues/313). In this case you can use the `eachKeyLike` method, which takes an 
example key as a parameter.

For example:

```java
DslPart body = new PactDslJsonBody()
  .object("one")
    .eachKeyLike("001", PactDslJsonRootValue.id(12345L)) // key like an id mapped to a matcher
  .closeObject()
  .object("two")
    .eachKeyLike("001-A") // key like an id where the value is matched by the following example
      .stringType("description", "Some Description")
    .closeObject()
  .closeObject()
  .object("three")
    .eachKeyMappedToAnArrayLike("001") // key like an id mapped to an array where each item is matched by the following example
      .id("someId", 23456L)
      .closeObject()
    .closeArray()
  .closeObject();

```

For an example, have a look at [WildcardKeysTest](/consumer/junit/src/test/java/au/com/dius/pact/consumer/junit/WildcardKeysTest.java).

**NOTE:** The `eachKeyLike` method adds a `*` to the matching path, so the matching definition will be applied to all keys
 of the map if there is not a more specific matcher defined for a particular key. Having more than one `eachKeyLike` condition
 applied to a map will result in only one being applied when the pact is verified (probably the last).
 
**Further Note: From version 3.5.22 onwards pacts with wildcards applied to map keys will require the Java system property 
"pact.matching.wildcard" set to value "true" when the pact file is verified.**

### Matching on paths

You can use regular expressions to match incoming requests. The DSL has a `matchPath` method for this. You can provide
a real path as a second value to use when generating requests, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .matchPath("/transaction/[0-9]+") // or .matchPath("/transaction/[0-9]+", "/transaction/1234567890")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
```

### Matching on headers

You can use regular expressions to match request and response headers. The DSL has a `matchHeader` method for this. You can provide
an example header value to use when generating requests and responses, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .matchHeader("testreqheader", "test.*value")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
        .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234")
```

### Matching on query parameters

You can use regular expressions to match request query parameters. The DSL has a `matchQuery` method for this. You can provide
an example value to use when generating requests, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .matchQuery("a", "\\d+", "100")
        .matchQuery("b", "[A-Z]", "X")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
```

# Forcing pact files to be overwritten

By default, when the pact file is written, it will be merged with any existing pact file. To force the file to be 
overwritten, set the Java system property `pact.writer.overwrite` to `true`.

# Having values injected from provider state callbacks

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

The following DSL methods allow you to set an expression that will be parsed with the values returned from the provider states:

For JSON bodies, use `valueFromProviderState`.<br/>
For headers, use `headerFromProviderState`.<br/>
For query parameters, use `queryParameterFromProviderState`.<br/>
For paths, use `pathFromProviderState`.

For example, assume that an API call is made to get the details of a user by ID. A provider state can be defined that
specifies that the user must be exist, but the ID will be created when the user is created. So we can then define an
expression for the path where the ID will be replaced with the value returned from the provider state callback.

```java
    .pathFromProviderState("/api/users/${id}", "/api/users/100")
``` 

You can also just use the key instead of an expression:

```java
    .valueFromProviderState('userId', 'userId', 100) // will look value using userId as the key
```

# A Lambda DSL for Pact

This is an extension for the pact DSL. The difference between
the default pact DSL and this lambda DSL is, as the name suggests, the usage of lambdas. The use of lambdas makes the code much cleaner.

## Why a new DSL implementation?

The lambda DSL solves the following two main issues. Both are visible in the following code sample:
 
```java
new PactDslJsonArray()
    .array()                            # open an array
    .stringValue("a1")                  # choose the method that is valid for arrays
    .stringValue("a2")                  # choose the method that is valid for arrays
    .closeArray()                       # close the array
    .array()                            # open an array
    .numberValue(1)                     # choose the method that is valid for arrays
    .numberValue(2)                     # choose the method that is valid for arrays
    .closeArray()                       # close the array
    .array()                            # open an array
    .object()                           # now we work with an object
    .stringValue("foo", "Foo")          # choose the method that is valid for objects
    .closeObject()                      # close the object and we're back in the array
    .closeArray()                       # close the array
```

### The existing DSL is quite error-prone

Methods may only be called in certain states. For example `object()` may only be called when you're currently working on an array whereas `object(name)`
is only allowed to be called when working on an object. But both of the methods are available. You'll find out at runtime if you're using the correct method.

Finally, the need for opening and closing objects and arrays makes usage cumbersome.

The lambda DSL has no ambiguous methods and there's no need to close objects and arrays as all the work on such an object is wrapped in a lamda call.

### The existing DSL is hard to read

When formatting your source code with an IDE the code becomes hard to read as there's no indentation possible. Of course, you could do it by hand but we want auto formatting!
Auto formatting works great for the new DSL!

```java
array.object((o) -> {
  o.stringValue("foo", "Foo");          # an attribute
  o.stringValue("bar", "Bar");          # an attribute
  o.object("tar", (tarObject) -> {      # an attribute with a nested object
    tarObject.stringValue("a", "A");    # attribute of the nested object
    tarObject.stringValue("b", "B");    # attribute of the nested object
  })
});
```

## Usage

Start with a static import of `LambdaDsl`. This class contains factory methods for the lambda dsl extension. 
When you come accross the `body()` method of `PactDslWithProvider` builder start using the new extensions. 
The call to `LambdaDsl` replaces the call to instance `new PactDslJsonArray()` and `new PactDslJsonBody()` of the pact library.

```java
io.pactfoundation.consumer.dsl.LambdaDsl.*
```

### Response body as json array

```java

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;

...

PactDslWithProvider builder = ...
builder.given("some state")
        .uponReceiving("a request")
        .path("/my-app/my-service")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(newJsonArray((a) -> {
            a.stringValue("a1");
            a.stringValue("a2");
        }).build());
```

### Response body as json object

```java

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;

...

PactDslWithProvider builder = ...
builder.given("some state")
        .uponReceiving("a request")
        .path("/my-app/my-service")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(newJsonBody((o) -> {
            o.stringValue("foo", "Foo");
            o.stringValue("bar", "Bar");
        }).build());
```

### Examples

#### Simple Json object

When creating simple json structures the difference between the two approaches isn't big.

##### JSON

```json
{
    "bar": "Bar",
    "foo": "Foo"
}
```

##### Pact DSL

```java
new PactDslJsonBody()
    .stringValue("foo", "Foo")
    .stringValue("bar", "Bar")
```

##### Lambda DSL

```java
newJsonBody((o) -> {
    o.stringValue("foo", "Foo");
    o.stringValue("bar", "Bar");
}).build();
```

#### An array of arrays

When we come to more complex constructs with arrays and nested objects the beauty of lambdas become visible! 

##### JSON

```json
[
    ["a1", "a2"],
    [1, 2],
    [{"foo": "Foo"}]
]
```

##### Pact DSL

```java
new PactDslJsonArray()
    .array()
    .stringValue("a1")
    .stringValue("a2")
    .closeArray()
    .array()
    .numberValue(1)
    .numberValue(2)
    .closeArray()
    .array()
    .object()
    .stringValue("foo", "Foo")
    .closeObject()
    .closeArray();
```

##### Lambda DSL

```java
newJsonArray((rootArray) -> {
    rootArray.array((a) -> a.stringValue("a1").stringValue("a2"));
    rootArray.array((a) -> a.numberValue(1).numberValue(2));
    rootArray.array((a) -> a.object((o) -> o.stringValue("foo", "Foo")));
}).build();
```

##### Kotlin Lambda DSL

```kotlin
newJsonArray {
    newArray {
      stringValue("a1")
      stringValue("a2")
    }
    newArray {
      numberValue(1)
      numberValue(2)
    }
    newArray {
      newObject { stringValue("foo", "Foo") }
    }
 }
```
