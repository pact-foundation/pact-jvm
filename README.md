pact-jvm
========

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=master)](https://travis-ci.org/DiUS/pact-jvm)

JVM implementation of the consumer driven contract library (pact)[https://github.com/bethesque/pact_specification]

##I Use Scala and Specs 2

You want to look at: [pact-jvm-specs2](pact-jvm-specs2)

##I Use Java

You want to look at: [pact-jvm-junit](pact-jvm-junit)

##I Use some other jvm language or test framework (groovy, clojure etc)

You want to look at: [Pact Consumer](pact-jvm-consumer)

##I am writing a provider and want to run pacts

You want to look at: [pact sbt plugin](pact-jvm-provider-sbt)

##I want to run pacts but don't want to use sbt

You want to look at: [pact-jvm-provider](pact-jvm-provider)

##I Use Ruby
The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.

You'll want to look at: [pact](https://github.com/realestate-com-au/pact)

##I Use something completely different

There's a limit to how much we can help, however check out [pact-jvm-server](pact-jvm-server)

##How do I transport my pacts from consumers to providers?

You want to look at:
[Pact Broker](https://github.com/bethesque/pact_broker)

Which is a project that aims at providing tooling to coordinate pact generation and delivery between projects

##I want to contribute

You can publish pacts locally using:

```
sbt clean test publishLocal
```

To publish to a nexus repo, change the url in project/Build.scala then run:

```
sbt clean test publish
```

