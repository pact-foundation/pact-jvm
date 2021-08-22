# Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## Building the library

Most of Pact-JVM is written in Kotlin and is built with Gradle. Tests are written using [Spock](https://spockframework.org/).

Before you build, install java 11, `Gradle` and `Maven` (Maven is required to build the Maven plugin).

#### To build the libraries:

    $ ./gradlew clean build

You can publish pact-jvm to your local maven repo using:

    $ ./gradlew publishToMavenLocal

If the build fails due to JVM memory issues, these are the settings reported to work:
>  set `org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8` in ~/.gradle/gradle.properties to fix metaspace problems with JDK 11

To publish to a nexus repo:

    $ ./gradlew clean check uploadArchives

You will have to change the nexus URL and username/password in build.gradle and you must be added to the nexus project
to be able to do this

## Project structure

The project is in 3 basic parts (core, consumer and provider). 

The core modules (model, matchers, pactbroker and support) provide the main Pact implementation and deal
with reading and writing the pact file format, how to match pacts and interacting with the Pact broker. 

The consumer modules (consumer\*) deal with providing support for writing consumer tests for different test frameworks. 

The provider modules (provider\*) deal with validating pacts against a provider with support for a number of build tools. 

Finally, pact-jvm-server is a standalone mock server and pact-specification-test tests pact-jvm against the specification test cases.
