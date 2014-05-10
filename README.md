pact-jvm
========

[![Build Status](https://travis-ci.org/DiUS/pact-jvm.svg?branch=master)](https://travis-ci.org/DiUS/pact-jvm)

JVM specific port of the ruby project:
*  https://github.com/realestate-com-au/pact


##I Use Scala and Specs 2

You want to look at: [pact-jvm-specs2](pact-jvm-specs2)


##I Use Java

You want to look at: [pact-jvm-junit](pact-jvm-junit)

##I want to contribute

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

    POST "${config.stateChangeUrl.url}/setup" { "state" : "${interaction.stateName}" }


Pact model
==========

The model project is responsible for providing:
*  a model to represent pacts
*  serialization and deserialization
*  comparison between two parts of the pact model
*  conversion between the pact model and whatever third party libraries used by the pact-consumer and pact-provider requires

You should never need to include this project directly

Pact server
===========

The pact server is a stand-alone interactions recorder and verifier, aimed at clients that are non-JVM or non-Ruby based.

The pact client for that platform will need to be implemented, but it only be responsible for generating the `JSON`
interactions, running the tests and communicating with the server.

The server implements a `JSON` `REST` Admin API with the following endpoints.

    /         -> For diagnostics, currently returns a list of ports of the running mock servers.
    /create   -> For initialising a test server and submitting the JSON interactions. It returns a port
    /complete -> For finalising and verifying the interactions with the server.  It writes the `JSON` pact file to disk.

## Running the server

    sbt pact-jvm-server/run

By default will run on port `29999` but a port number can be optionally supplied.

## Life cycle

The following actions are expected to occur

 * The client calls `/create` to initialise a server with the expected `JSON` interactions and state
 * The admin server will start a mock server on a random port and return the port number in the response
 * The client will execute its interaction tests against the mock server with the supplied port
 * Once finished, the client will call `/complete' on the Admin API, posting the port number
 * The pact server will verify the interactions and write the `JSON` `pact` file to disk under `/target`
 * The mock server running on the supplied port will be shutdown.

## Endpoints

### /create

The client will need `POST` to `/create` the generated `JSON` interactions, also providing a state as a query parameter.

For example:

    POST http://localhost:29999/create?state=NoUsers '{ "provider": { "name": "Animal_Service"}, ... }'

This will create a new running mock service provider on a randomly generated port.  The port will be returned in the
`201` response:

    { "port" : 34423 }

### /complete

Once the client has finished running its tests against the mock server on the supplied port (in this example port
`34423`) the client will need to `POST` to `/complete` the port number of the mock server that was used.

For example:

    POST http://localhost:29999/complete '{ "port" : 34423 }'

This will cause the Pact server to verify the interactions, shutdown the mock server running on that port and writing
the pact `JSON` file to disk under the `target` directory.

### /

The `/` endpoint is for diagnostics and to check that the pact server is running.  It will return all the currently
running mock servers port numbers.

For example:

    GET http://localhost:29999/

        '{ "ports": [23443,43232] }'