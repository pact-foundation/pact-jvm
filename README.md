Pact-JVM
========

[![Pact-JVM Build](https://github.com/pact-foundation/pact-jvm/workflows/Pact-JVM%20Build/badge.svg)](https://github.com/pact-foundation/pact-jvm/actions?query=workflow%3A%22Pact-JVM+Build%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.com.dius.pact.core/model/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.com.dius.pact.core/model)

JVM implementation of the consumer driven contract library [pact](https://github.com/pact-foundation/pact-specification).

From the [Ruby Pact website](https://github.com/pact-foundation/pact-ruby):

> Define a pact between service consumers and providers, enabling "consumer driven contract" testing.
>
>Pact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service provider and the HTTP responses they expect back. 
>These expectations are used in the consumers specs to provide a mock service provider. The interactions are recorded, and played back in the service provider 
>specs to ensure the service provider actually does provide the response the consumer expects.
>
>This allows testing of both sides of an integration point using fast unit tests.
>
>This gem is inspired by the concept of "Consumer driven contracts". See https://martinfowler.com/articles/consumerDrivenContracts.html for more information.


Read [Getting started with Pact](https://dius.com.au/2016/02/03/pact-101-getting-started-with-pact-and-consumer-driven-contract-testing/) for more information on
how to get going.


## Contact

* Twitter: [@pact_up](https://twitter.com/pact_up)
* Slack: [Join the chat at https://slack.pact.io/](https://slack.pact.io/)
* Stack Overflow: https://stackoverflow.com/questions/tagged/pact

## Links

* For examples of using pact-jvm with spring boot, have a look at https://github.com/Mikuu/Pact-JVM-Example and https://github.com/mstine/microservices-pact

## Tutorial (60 minutes)

Learn everything in Pact in 60 minutes: https://github.com/pact-foundation/pact-workshop-jvm-spring.

The workshop takes you through all of the key concepts of consumer and provider testing using a Spring boot application.

## Documentation

Additional documentation can be found at [docs.pact.io](http://docs.pact.io), in the [Pact Wiki](https://github.com/pact-foundation/pact-ruby/wiki),
and in the [Pact-JVM wiki](https://github.com/pact-foundation/pact-jvm/wiki). [Stack Overflow](https://stackoverflow.com/questions/tagged/pact) is also a good source of help, as is the [Slack workspace](https://slack.pact.io).

## Supported JDK and specification versions:

| Branch                                                                            | Specification | JDK        | Kotlin Version | Latest Version | Notes |
|-----------------------------------------------------------------------------------|---------------|------------|----------------|----------------|-------|
| [4.7.x](https://github.com/pact-foundation/pact-jvm/blob/v4.7.x/README.md)        | V4 + plugins  | 17-19      | 1.8.22         | 4.7.0-beta.0   |       |
| [4.6.x](https://github.com/pact-foundation/pact-jvm/blob/v4.6.x/README.md) master | V4 + plugins  | 17-18      | 1.8.22         | 4.6.15         |       |
| [4.5.x](https://github.com/pact-foundation/pact-jvm/blob/v4.5.x/README.md)        | V4 + plugins  | 11+/17+(1) | 1.7.20         | 4.5.13         |       |
| [4.1.x](https://github.com/pact-foundation/pact-jvm/blob/v4.1.x/README.md)        | V3            | 8-12       | 1.3.72         | 4.1.43         |       |

**Notes:**
* **1:** Spring6 support library requires JDK 17+. The rest of Pact-JVM 4.5.x libs require 11+.

### Previous versions (not actively supported)

| Branch                                                                    | Specification | JDK       | Kotlin Version | Scala Versions | Latest Version |
|---------------------------------------------------------------------------|---------------|-----------|----------------|----------------|----------------|
| [4.4.x](https://github.com/pact-foundation/pact-jvm/blob/v4.4.x/README.md)           | V4 + plugins  | 11+       | 1.6.21         | N/A            | 4.4.9          |
| [4.3.x](https://github.com/pact-foundation/pact-jvm/blob/v4.3.x/README.md)           | V4            | 11+       | 1.6.21         | N/A            | 4.3.19         |
| [4.2.x](https://github.com/pact-foundation/pact-jvm/blob/v4.2.x/README.md)           | V4 (1)        | 11-15 (2) | 1.4.32         | N/A            | 4.2.21         |
| [4.0.x](https://github.com/pact-foundation/pact-jvm/blob/v4.x/README.md)             | V3            | 8-12      | 1.3.71         | N/A            | 4.0.10         |
| [3.6.x](https://github.com/pact-foundation/pact-jvm/blob/v3.6.x/README.md)           | V3            | 8         | 1.3.71         | 2.12           | 3.6.15         |
| [3.5.x](https://github.com/pact-foundation/pact-jvm/blob/v3.5.x/README.md)           | V3            | 8         | 1.1.4-2        | 2.12, 2.11     | 3.5.25         |
| [3.5.x-jre7](https://github.com/pact-foundation/pact-jvm/blob/v3.5.x-jre7/README.md) | V3            | 7         | 1.1.4-2        | 2.11           | 3.5.7-jre7.0   |
| [2.4.x](https://github.com/pact-foundation/pact-jvm/blob/v2.x/README.md)             | V2            | 6         | N/A            | 2.10, 2.11     | 2.4.20         |

**Notes:**
* **1:** V4 specification support is only partially implemented with 4.2.x
* **2:** v4.2.x may run on JDK 16, but the build for it does not.

**NOTE:** The JARs produced by this project have changed with 4.1.x to better align with Java 9 JPMS. The artefacts are now:

```
au.com.dius.pact:consumer
au.com.dius.pact.consumer:groovy
au.com.dius.pact.consumer:junit
au.com.dius.pact.consumer:junit5
au.com.dius.pact.consumer:java8
au.com.dius.pact.consumer:specs2_2.13
au.com.dius.pact:pact-jvm-server
au.com.dius.pact:provider
au.com.dius.pact.provider:scalatest_2.13
au.com.dius.pact.provider:spring
au.com.dius.pact.provider:maven
au.com.dius.pact:provider
au.com.dius.pact.provider:junit
au.com.dius.pact.provider:junit5
au.com.dius.pact.provider:scalasupport_2.13
au.com.dius.pact.provider:lein
au.com.dius.pact.provider:gradle
au.com.dius.pact.provider:specs2_2.13
au.com.dius.pact.provider:junit5spring
au.com.dius.pact.core:support
au.com.dius.pact.core:model
au.com.dius.pact.core:matchers
au.com.dius.pact.core:pactbroker
```

## Service Consumers

Pact-JVM has a number of ways you can write your service consumer tests.

### I Use Scala

You want to look at: [pact4s](https://github.com/jbwheatley/pact4s).

### I Use Java

You want to look at: [junit](consumer/junit) for JUnit 4 tests and
[junit5](consumer/junit5) for JUnit 5 tests. Also, if you are using Java 11 or above, there is [an
updated DSL for consumer tests](/consumer).

**NOTE:** If you are using Java 8, there is no separate Java 8 support library anymore, see the above library.


### I Use Groovy or Grails

You want to look at: [groovy](consumer/groovy) or [junit](consumer/junit)

### (Use Clojure I)

Clojure can call out to Java, so have a look at [junit](consumer/junit). For an example
look at [example_clojure_consumer_pact_test.clj](https://github.com/pact-foundation/pact-jvm/blob/master/consumer/junit/src/test/clojure/au/com/dius/pact/consumer/junit/example_clojure_consumer_pact_test.clj).

### I Use some other jvm language or test framework

You want to look at: [Consumer](consumer)

### My Consumer interacts with a Message Queue

As part of the V3 pact specification, we have defined a new pact file for interactions with message queues. For an
  implementation of a Groovy consumer test with a message pact, have a look at [PactMessageBuilderSpec.groovy](https://github.com/pact-foundation/pact-jvm/blob/master/consumer/groovy/src/test/groovy/au/com/dius/pact/consumer/groovy/messaging/PactMessageBuilderSpec.groovy).

## Service Providers

Once you have run your consumer tests, you will have generated some Pact files. You can then verify your service providers
with these files.

### I am writing a provider and want to ...

#### verify pacts with SBT

You want to look at: [pact4s](https://github.com/jbwheatley/pact4s) or [scala-pact](https://github.com/ITV/scala-pact)

#### verify pacts with Gradle

You want to look at: [pact gradle plugin](provider/gradle)

#### verify pacts with Maven

You want to look at: [pact maven plugin](provider/maven)

#### verify pacts with JUnit tests

You want to look at: [junit provider support](provider/junit) for JUnit 4 tests and 
 [junit5](provider/junit5) for JUnit 5 tests

#### verify pacts with Leiningen

You want to look at: [pact leiningen plugin](provider/lein)

#### verify pacts with a Spring MVC project

Have a look at [spring](provider/spring) or [Spring MVC Pact Test Runner](https://github.com/realestate-com-au/pact-jvm-provider-spring-mvc) (Not maintained).

#### I want to verify pacts but don't want to use sbt or gradle or leiningen

You want to look at: [provider](provider)

#### verify interactions with a message queue

As part of the V3 pact specification, we have defined a new pact file for interactions with message queues. The Gradle
pact plugin supports a mechanism where you can verify V3 message pacts, have a look at [pact gradle plugin](provider/gradle#verifying-a-message-provider).
The JUnit pact library also supports verification of V3 message pacts, have a look at [junit](provider/junit#verifying-a-message-provider).

### I Use Ruby or Go or something else
The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However, if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.
You'll want to look at: [Ruby Pact](https://github.com/pact-foundation/pact-ruby).

For .Net, there is [Pact-net](https://github.com/pact-foundation/pact-net).

For JS, there is [Pact-JS](https://github.com/pact-foundation/pact-js).

For Go, there is [Pact-go](https://github.com/pact-foundation/pact-go).

For Rust, there is [Pact-Rust](https://github.com/pact-foundation/pact-reference/tree/master/rust/pact_consumer).

Have a look at [implementations in other languages](https://github.com/realestate-com-au/pact/wiki#implementations-in-other-languages).

### I Use something completely different

There's a limit to how much we can help, however check out [pact-jvm-server](pact-jvm-server)

## How do I transport my pacts from consumers to providers?

You want to look at:
[Pact Broker](https://github.com/pact-foundation/pact_broker)

Which is a project that aims at providing tooling to coordinate pact generation and delivery between projects.

## I want to contribute

[Documentation for contributors is here](https://github.com/pact-foundation/pact-jvm/blob/master/CONTRIBUTING.md).

# Test Analytics

We are tracking anonymous analytics to gather important usage statistics like JVM version
and operating system. To disable tracking, set the 'pact_do_not_track' system property or environment
variable to 'true'.
