Pact-provider-jvm
=================

sub project of https://github.com/DiUS/pact-jvm

The pact provider is responsible for verifying that an API provider adheres to a number of pacts authored by its clients

This library provides the basic tools required to automate the process, and should be usable on its own in many instances.

Framework and build tool specific bindings will be provided in separate libraries that build on top of this core functionality.

Running Pacts
=============

Main takes 2 arguments:

The first is the root folder of your pact files
(all .json files in root and subfolders are assumed to be pacts)

The second is the location of your pact config json file.

Pact config
===========

The pact config is a simple mapping of provider names to endpoint url's
paths will be appended to endpoint url's when interactions are attempted

for an example see: https://github.com/DiUS/pact-provider-jvm/blob/master/src/test/resources/pact-config.json

Provider State
==============

Before each interaction is executed, the provider under test will have the opportunity to enter a state.
Generally the state maps to a set of fixture data for mocking out services that the provider is a consumer of (they will have their own pacts)

The pact framework will instruct the test server to enter that state by sending:

    POST "${config.get(providerName)}/setup" { "state" : "${interaction.stateName}" }

