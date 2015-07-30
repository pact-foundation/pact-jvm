pact-jvm-consumer-groovy-v3
===========================

Groovy DSL for Pact JVM implementing V3 specification changes (WIP)

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-groovy-v3_2.11`
* version-id = `2.2.x`

##Usage

Add the `pact-jvm-consumer-groovy-v3` library to your test class path. This provides a `PactMessageBuilder` class for you to use
to define your pacts.

If you are using gradle for your build, add it to your `build.gradle`:

    dependencies {
        testCompile 'au.com.dius:pact-jvm-consumer-groovy-v3_2.11:2.2.11'
    }
  
