1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## Building the library

Most of Pact-JVM is written in Scala and is built with Gradle.

Before you build, install java 1.8 and `maven`.

#### To build the libraries:

    $ ./gradlew clean build

You can publish pact-jvm to your local maven repo using:

    $ ./gradlew publishToMavenLocal

To publish to a nexus repo:

    $ ./gradlew clean check uploadArchives

You will have to change the nexus URL and username/password in build.gradle and you must be added to the nexus project
to be able to do this

### Building the SBT modules

The SBT need to be build using SBT on the v2.x branch. First check that branch out:

    $ git checkout v2.x

Then build all the other modules using Gradle and publish to your local maven repo.

    $ ./gradlew install

Once that is done, you can now build the SBT modules.

#### Note on building pact JVM with Java 6 or 7

Scala requires a lot of permgen space to compile. If you're using Java 6 or 7, use the following java and sbt options:

    export JAVA_OPTS='-Xmx2048m -XX:MaxPermSize=1024m -XX:PermSize=1024m'
    export SBT_OPTS='-Xmx2048m -XX:MaxPermSize=1024m -XX:PermSize=1024m'

To build the SBT modules:

    $ sbt clean test publishM2

To publish to a nexus repo, change the url in project/Build.scala then run:

    $ sbt clean test publish

## Project structure

The project is in 3 basic parts (model, consumer and provider). 

The model modules (pact-jvm-model, pact-jvm-model-v3, pact-jvm-matchers) deal with reading and writing the pact file format and how to match pacts and provides the pact mock server. 

The consumer modules (pact-jvm-consumer\*) deal with providing support for writing consumer tests for different test frameworks. 

The provider modules (pact-jvm-provider\*) deal with validating pacts against a provider with support for a number of build tools. 

Finally, pact-jvm-server is a standalone mock server and pact-specification-test tests pact-jvm against the specification test cases.
