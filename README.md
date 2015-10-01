pact-jvm
========

[![Join the chat at https://gitter.im/DiUS/pact-jvm](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/DiUS/pact-jvm?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=master)](https://travis-ci.org/DiUS/pact-jvm)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-logging_2.11/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-logging_2.11)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-consumer_2.11/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-consumer_2.11)

JVM implementation of the consumer driven contract library [pact](https://github.com/bethesque/pact_specification)

From the [Ruby Pact website](https://github.com/realestate-com-au/pact):

> Define a pact between service consumers and providers, enabling "consumer driven contract" testing.
>
>Pact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service provider and the HTTP responses they expect back. 
>These expectations are used in the consumers specs to provide a mock service provider. The interactions are recorded, and played back in the service provider 
>specs to ensure the service provider actually does provide the response the consumer expects.
>
>This allows testing of both sides of an integration point using fast unit tests.
>
>This gem is inspired by the concept of "Consumer driven contracts". See http://martinfowler.com/articles/consumerDrivenContracts.html for more information.

## Contact

* Twitter: [@pact_up](https://twitter.com/pact_up)
* Google users group: https://groups.google.com/forum/#!forum/pact-support

## Links

* For an example of using pact-jvm with spring boot, have a look at https://github.com/mstine/microservices-pact

## Documentation

Additional documentation can be found in the [Pact Wiki](https://github.com/realestate-com-au/pact/wiki),
and in the [Pact-JVM wiki](https://github.com/DiUS/pact-jvm/wiki).

## Note about artifact names and versions

Pact-JVM is written in Scala. As Scala does not provide binary compatibility between major versions, all the Pact-JVM
artifacts have the version of Scala they were built with in the artifact name. So, for example, the pact-jvm-consumer-junit
module has a Jar file named pact-jvm-consumer_2.10. The full name of the file is pact-jvm-consumer_2.10-2.0.x.jar.

### 3.x branch

3.x branch drops support for JDK 6 and 7 as well as Scala 2.10. It will be cross-compiled with Scala 2.12 when that is
released, but is currently build against Scala 2.11. All Groovy code is compiled with Invoke Dynamic instruction turned
on (indy version).

### 2.x branch

2.x versions of pact support JDK 6 and Scala 2.10. We currently cross-compile all the artifacts against 2.10 and
2.11 versions of Scala, except for the SBT modules.

## Service Consumers

Pact-JVM has a number of ways you can write your service consumer tests.

### I Use Scala and Specs 2

You want to look at: [pact-jvm-consumer-specs2](pact-jvm-consumer-specs2)

### I Use Java

You want to look at: [pact-jvm-consumer-junit](pact-jvm-consumer-junit)

### I Use Groovy or Grails

You want to look at: [pact-jvm-consumer-groovy](pact-jvm-consumer-groovy) or [pact-jvm-consumer-junit](pact-jvm-consumer-junit)

### (Use Clojure I)

Clojure can call out to Java, so have a look at [pact-jvm-consumer-junit](pact-jvm-consumer-junit). For an example
look at [example_clojure_consumer_pact_test.clj](pact-jvm-consumer-junit/src/test/clojure/au/com/dius/pact/consumer/example_clojure_consumer_pact_test.clj).

### I Use some other jvm language or test framework

You want to look at: [Pact Consumer](pact-jvm-consumer)

### My Consumer interacts with a Message Queue

As part of the V3 pact specification, we have defined a new pact file for interactions with message queues. For an
  implementation of a Groovy consumer test with a message pact, have a look at [pact-jvm-consumer-groovy-v3](pact-jvm-consumer-groovy-v3).

## Service Providers

Once you have run your consumer tests, you will have generated some Pact files. You can then verify your service providers
with these files.

### I am writing a provider and want to ...

#### verify pacts with SBT

You want to look at: [pact sbt plugin](pact-jvm-provider-sbt)

#### verify pacts with Gradle

You want to look at: [pact gradle plugin](pact-jvm-provider-gradle)

#### verify pacts with Maven [version 2.1.9+]

You want to look at: [pact maven plugin](pact-jvm-provider-maven)

For publishing pacts to a pact broker, have a look at https://github.com/warmuuh/pactbroker-maven-plugin

#### verify pacts with Leiningen [version 2.2.14+, 3.0.3+]

You want to look at: [pact leiningen plugin](pact-jvm-provider-lein)

#### verify pacts with Specs2

Have a look at [writing specs to validate a provider](https://github.com/realestate-com-au/pact-jvm-provider-specs2)

#### verify pacts with a Spring MVC project

Have a look at [Spring MVC Pact Test Runner](https://github.com/realestate-com-au/pact-jvm-provider-spring-mvc)

#### I want to verify pacts but don't want to use sbt or gradle or leiningen

You want to look at: [pact-jvm-provider](pact-jvm-provider)

#### verify interactions with a message queue

As part of the V3 pact specification, we have defined a new pact file for interactions with message queues. The Gradle
pact plugin supports a mechanism where you can verify V3 message pacts, have a look at [pact gradle plugin](pact-jvm-provider-gradle#verifying-a-message-provider).

### I Use Ruby
The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.

You'll want to look at: [pact](https://github.com/realestate-com-au/pact)

### I Use something completely different

There's a limit to how much we can help, however check out [pact-jvm-server](pact-jvm-server)

## How do I transport my pacts from consumers to providers?

You want to look at:
[Pact Broker](https://github.com/bethesque/pact_broker)

Which is a project that aims at providing tooling to coordinate pact generation and delivery between projects.

## I want to contribute

[Documentation for contributors is on the wiki](https://github.com/DiUS/pact-jvm/wiki/Hot-to-contribute-to-Pact-JVM).
