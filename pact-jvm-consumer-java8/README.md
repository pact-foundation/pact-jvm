# pact-jvm-consumer-java8
Provides a Java8 lambda based DSL for use with Junit to build consumer tests.

# A Lambda DSL for Pact

This is an extension for the pact DSL provided by [pact-jvm-consumer](../pact-jvm-consumer). The difference between
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

## Installation

### Maven

```
<dependency>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-consumer-java8_2.12</artifactId>
    <version>${pact.version}</version>
</dependency>
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
}).build()
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
    .closeArray()
```

##### Lambda DSL

```java
newJsonArray((rootArray) -> {
    rootArray.array((a) -> a.stringValue("a1").stringValue("a2"));
    rootArray.array((a) -> a.numberValue(1).numberValue(2));
    rootArray.array((a) -> a.object((o) -> o.stringValue("foo", "Foo"));
}).build()

```


