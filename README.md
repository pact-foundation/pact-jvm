pact-jvm
========

[![Join the chat at https://gitter.im/DiUS/pact-jvm](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/DiUS/pact-jvm?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=v3-spec)](https://travis-ci.org/DiUS/pact-jvm)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/172049m2sa57takc?svg=true)](https://ci.appveyor.com/project/uglyog/pact-jvm)
[![Codefresh build status]( https://g.codefresh.io/api/badges/build?repoOwner=DiUS&repoName=pact-jvm&branch=v3-spec&pipelineName=pact-jvm&accountName=uglyog&type=cf-1)]( https://g.codefresh.io/repositories/DiUS/pact-jvm/builds?filter=trigger:build;branch:v3-spec;service:59182913f2383500056367d7~pact-jvm)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-consumer_2.11/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.com.dius/pact-jvm-consumer_2.11)

JVM implementation of the consumer driven contract library [pact](https://github.com/bethesque/pact_specification).

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


Read [Getting started with Pact](http://dius.com.au/2016/02/03/microservices-pact/) for more information on
how to get going.


## Contact

* Twitter: [@pact_up](https://twitter.com/pact_up)
* Gitter: [![Join the chat at https://gitter.im/DiUS/pact-jvm](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/DiUS/pact-jvm?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* Stack Overflow: https://stackoverflow.com/questions/tagged/pact

## Links

* For an example of using pact-jvm with spring boot, have a look at https://github.com/mstine/microservices-pact

## Documentation

Additional documentation can be found at [docs.pact.io](http://docs.pact.io), in the [Pact Wiki](https://github.com/realestate-com-au/pact/wiki),
and in the [Pact-JVM wiki](https://github.com/DiUS/pact-jvm/wiki). [Stack Overflow](https://stackoverflow.com/questions/tagged/pact) is also a good source of help.

## Note about artifact names and versions

Pact-JVM is partially written in Scala. As Scala does not provide binary compatibility between major versions, all the Pact-JVM
artifacts have the version of Scala they were built with in the artifact name. So, for example, the pact-jvm-consumer-junit
module has a Jar file named pact-jvm-consumer_2.10. The full name of the file is pact-jvm-consumer_2.10-2.0.x.jar.

## Supported JDK and specification versions: 

| Branch | Specification | Min JDK | Scala Versions | Latest Version |
| ------ | ------------- | ------- | -------------- | -------------- |
| 3.4.x (v3.4.x)| V2 | 8 | 2.11 | 3.4.1 |
| 3.5.x (v3-spec) | V3 | 8 | 2.11 | 3.5.0-rc.3 |
| 2.5.x (v2.5.x) | V3 | 7 | 2.11 | 2.5.0-beta.0 |
| 2.4.x (v2.x) | V2 | 6 | 2.10, 2.11 | 2.4.18 |

## Service Consumers

Pact-JVM has a number of ways you can write your service consumer tests.

### I Use Scala

You want to look at: [scala-pact](https://github.com/ITV/scala-pact) or [pact-jvm-consumer-specs2](pact-jvm-consumer-specs2)

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
  implementation of a Groovy consumer test with a message pact, have a look at [PactMessageBuilderSpec.groovy](pact-jvm-consumer-groovy/src/test/groovy/au/com/dius/pact/consumer/groovy/messaging/PactMessageBuilderSpec.groovy).

## Service Providers

Once you have run your consumer tests, you will have generated some Pact files. You can then verify your service providers
with these files.

### I am writing a provider and want to ...

#### verify pacts with SBT

You want to look at: [scala-pact](https://github.com/ITV/scala-pact) or [pact sbt plugin](pact-jvm-provider-sbt)

#### verify pacts with Gradle

You want to look at: [pact gradle plugin](pact-jvm-provider-gradle)

#### verify pacts with Maven [version 2.1.9+]

You want to look at: [pact maven plugin](pact-jvm-provider-maven)

For publishing pacts to a pact broker, have a look at https://github.com/warmuuh/pactbroker-maven-plugin

#### verify pacts with JUnit tests [version 2.3.3+, 3.1.3+]

You want to look at: [junit provider support](pact-jvm-provider-junit)

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
The JUnit pact library also supports verification of V3 message pacts, have a look at [pact-jvm-provider-junit](pact-jvm-provider-junit#verifying-a-message-provider).

### I Use Ruby or Go or something else
The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.
You'll want to look at: [Ruby Pact](https://github.com/realestate-com-au/pact).

For .Net, there is [Pact-net](https://github.com/SEEK-Jobs/pact-net).

For Go, there is [Pact-go](https://github.com/seek-jobs/pact-go).

Have a look at [implementations in other languages](https://github.com/realestate-com-au/pact/wiki#implementations-in-other-languages).

### I Use something completely different

There's a limit to how much we can help, however check out [pact-jvm-server](pact-jvm-server)

## How do I transport my pacts from consumers to providers?

You want to look at:
[Pact Broker](https://github.com/bethesque/pact_broker)

Which is a project that aims at providing tooling to coordinate pact generation and delivery between projects.

## I want to contribute

[Documentation for contributors is on the wiki](https://github.com/DiUS/pact-jvm/wiki/How-to-contribute-to-Pact-JVM).
