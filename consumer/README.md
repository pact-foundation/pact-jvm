Pact-consumer-jvm
===============

sub project of https://github.com/DiUS/pact-jvm

Pact Consumer is used by projects that are consumers of an API.

Example
=======

https://github.com/DiUS/pact-consumer-jvm/blob/master/src/test/scala/com/dius/pact/consumer/ConsumerPactSpec.scala

Is an example of how you should write your integration tests so that pact json files are generated.

The generated files should then be delivered to the provider project for running with pact-provider-jvm.


Ruby Compatibility
==================

The pact-jvm libraries are pure jvm technologies and do not have any native dependencies.

However if you have a ruby provider, the json produced by this library is compatible with the ruby pact library.


Pact broker
===========

https://github.com/bethesque/pact_broker

Is a project that aims at providing tooling to coordinate pact generation and delivery between projects