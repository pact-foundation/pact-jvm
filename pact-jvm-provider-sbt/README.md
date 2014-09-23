Pact sbt plugin
===============

The sbt plugin adds an sbt task for running all provider pacts against a running server.

To use the pact sbt plugin, add the following to your project/plugins.sbt

    addSbtPlugin("au.com.dius" %% "pact-jvm-provider-sbt" % "2.0.6")

and the following to your build.sbt

    PactJvmPlugin.pactSettings

The new task added is verifyPacts

Two new keys are added to configure this task:

pactConfig is the location of your pact-config json file (defaults to "pact-config.json" in the classpath root)

pactRoot is the root folder of your pact json files (defaults to "pacts"), all .json files in root and sub folders will be executed
