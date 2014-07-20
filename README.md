pact-jvm
========

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=master)](https://travis-ci.org/DiUS/pact-jvm)

JVM implementation of the consumer driven contract library (pact)[https://github.com/bethesque/pact_specification]

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

## Note about artifact names and versions

Pact-JVM is written in Scala. As Scala does not provide binary compatibility between major versions, all the Pact-JVM
artifacts have the version of Scala they were built with in the artifact name. So, for example, the pact-jvm-consumer-junit
module has a Jar file named pact-jvm-consumer_2.10. The full name of the file is pact-jvm-consumer_2.10-2.0.0.jar.

We currently cross-compile all the artifacts against 2.10 and 2.11 versions of Scala, except for the SBT modules.

## Service Consumers

Pact-JVM has a number of ways you can write your service consumer tests.

### I Use Scala and Specs 2

You want to look at: [pact-jvm-consumer-specs2](pact-jvm-consumer-specs2)

### I Use Java

You want to look at: [pact-jvm-consumer-junit](pact-jvm-consumer-junit)

### I Use Groovy or Grails

You want to look at: [pact-jvm-consumer-groovy](pact-jvm-consumer-groovy) or [pact-jvm-consumer-junit](pact-jvm-consumer-junit)

### I Use some other jvm language or test framework (clojure etc)

You want to look at: [Pact Consumer](pact-jvm-consumer)

## Service Providers

Once you have run your consumer tests, you will have generated some Pact files. You can then verify your service providers
with these files.

### I am writing a provider and want to verify pacts

You want to look at: [pact sbt plugin](pact-jvm-provider-sbt)

### I want to run pacts but don't want to use sbt

You want to look at: [pact-jvm-provider](pact-jvm-provider)

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

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## Building the library

Most of Pact-JVM is written in Scala. You can build it using SBT (the old way). We are moving the build to Gradle,
so you can build it with that too.

To build the libraries:

    $ sbt clean test install

or

    $ ./gradlew clean build

You can publish pacts to your local maven repo using:

    $ sbt clean test publishLocal

or

    $ ~/.gradlew clean install

To publish to a nexus repo, change the url in project/Build.scala then run:

    $ sbt clean test publish

or

    $ ./gradlew clean check uploadArchives

You will need to be added to the nexus project to be able to do this.
