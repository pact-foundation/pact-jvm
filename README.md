pact-jvm
========

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=master)](https://travis-ci.org/DiUS/pact-jvm)

JVM implementation of the consumer driven contract library (pact)[https://github.com/bethesque/pact_specification]

From the [Ruby Pact website](https://github.com/realestate-com-au/pact):

> Define a pact between service consumers and providers, enabling "consumer driven contract" testing.
>
>Pact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service provider and the HTTP responses they expect back. These expectations are used in the >consumers specs to provide a mock service provider. The interactions are recorded, and played back in the service provider specs to ensure the service provider actually does provide the response the >consumer expects.
>
>This allows testing of both sides of an integration point using fast unit tests.
>
>This gem is inspired by the concept of "Consumer driven contracts". See http://martinfowler.com/articles/consumerDrivenContracts.html for more information.

## Contact

* Twitter: [@pact_up](https://twitter.com/pact_up)
* Google users group: https://groups.google.com/forum/#!forum/pact-support

##I Use Scala and Specs 2

You want to look at: [pact-jvm-consumer-specs2](pact-jvm-consumer-specs2)

##I Use Java

You want to look at: [pact-jvm-consumer-junit](pact-jvm-consumer-junit)

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

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

You can publish pacts locally using:

```
sbt clean test publishLocal
```

To publish to a nexus repo, change the url in project/Build.scala then run:

```
sbt clean test publish
```

You will need to be added to the nexus project to be able to do this.
