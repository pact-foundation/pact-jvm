pact-jvm
========

Aggregation Project for Pact sub-modules:
*  consumer
*  provider
*  model

JVM specific port of the ruby project:
*  https://github.com/realestate-com-au/pact


How to Release Pacts
====================

You can publish pacts locally using:

    sbt clean test publishLocal

To publish to a nexus repo, change the url in project/Build.scala then run:
    sbt clean test publish

publishing to maven central is coming, but not yet passed the paperwork.


Pact consumer
=============

Pact Consumer is used by projects that are consumers of an API.

### Example

https://github.com/DiUS/pact-jvm/blob/master/consumer/src/test/scala/com/dius/pact/consumer/ConsumerPactSpec.scala

Is an example of how you should write your integration tests so that pact json files are generated.

The generated files should then be delivered to the provider project for running with pact-provider-jvm.


### Ruby Compatibility


The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.


### Pact broker

https://github.com/bethesque/pact_broker

Is a project that aims at providing tooling to coordinate pact generation and delivery between projects

Pact sbt plugin
===============

The sbt plugin adds an sbt task for running all provider pacts against a running server.

To use the pact sbt plugin, add the following to your project/plugins.sbt

    addSbtPlugin("com.dius" %% "pact-jvm-sbt" % "1.0")

and the following to your build.sbt

    PactJvmPlugin.pactSettings

The new task added is verifyPacts

Two new keys are added to configure this task:

pactConfig is the location of your pact-config json file (defaults to "pact-config.json" in the classpath root)

pactRoot is the root folder of your pact json files (defaults to "pacts"), all .json files in root and sub folders will be executed


Pact provider
=============

sub project of https://github.com/DiUS/pact-jvm

The pact provider is responsible for verifying that an API provider adheres to a number of pacts authored by its clients

This library provides the basic tools required to automate the process, and should be usable on its own in many instances.

Framework and build tool specific bindings will be provided in separate libraries that build on top of this core functionality.

### Running Pacts

Main takes 2 arguments:

The first is the root folder of your pact files
(all .json files in root and subfolders are assumed to be pacts)

The second is the location of your pact config json file.

### Pact config


The pact config is a simple mapping of provider names to endpoint url's
paths will be appended to endpoint url's when interactions are attempted

for an example see: https://github.com/DiUS/pact-jvm/blob/master/provider/src/test/resources/pact-config.json

### Provider State

Before each interaction is executed, the provider under test will have the opportunity to enter a state.
Generally the state maps to a set of fixture data for mocking out services that the provider is a consumer of (they will have their own pacts)

The pact framework will instruct the test server to enter that state by sending:

    POST "${config.get(providerName)}/setup" { "state" : "${interaction.stateName}" }


Pact model
==========

The model project is responsible for providing:
*  a model to represent pacts
*  serialization and deserialization
*  comparison between two parts of the pact model
*  conversion between the pact model and whatever third party libraries used by the pact-consumer and pact-provider requires

You should never need to include this project directly