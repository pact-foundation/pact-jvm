pact-jvm
========

Aggregation Project for Pact sub-modules:
*  https://github.com/DiUS/pact-model-jvm
*  https://github.com/DiUS/pact-consumer-jvm
*  https://github.com/DiUS/pact-provider-jvm

JVM specific port of the ruby project:
*  https://github.com/realestate-com-au/pact


How to Use Git Submodules
=========================

You cannot make changes to submodules from here.  Check them out independently and work on their repos separately

First time you check this aggregating repo out, init the submodules with:
    git submodule update --init

If you already have this repo checked out, and want to update all submodules to latest master run:
    git submodule foreach git pull


How to Release Pacts
====================

You can publish pacts locally using:
    sbt clean test publishLocal

