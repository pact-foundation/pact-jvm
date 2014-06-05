#!/bin/bash
pkgdiff pact-jvm-consumer-junit/target/scala-2.10/pact-jvm-consumer-junit_2.10-2.0-RC3.jar pact-jvm-consumer-junit/build/libs/pact-jvm-consumer-junit-2.0-RC3.jar
pkgdiff pact-jvm-consumer-sbt/target/scala-2.10/sbt-0.13/pact-jvm-consumer-sbt-2.0-RC3.jar pact-jvm-consumer-sbt/build/libs/pact-jvm-consumer-sbt-2.0-RC3.jar
pkgdiff pact-jvm-consumer-specs2/target/scala-2.10/pact-jvm-consumer-specs2_2.10-2.0-RC3.jar pact-jvm-consumer-specs2/build/libs/pact-jvm-consumer-specs2-2.0-RC3.jar
pkgdiff pact-jvm-consumer/target/scala-2.10/pact-jvm-consumer_2.10-2.0-RC3.jar pact-jvm-consumer/build/libs/pact-jvm-consumer-2.0-RC3.jar
pkgdiff pact-jvm-model/target/scala-2.10/pact-jvm-model_2.10-2.0-RC3.jar pact-jvm-model/build/libs/pact-jvm-model-2.0-RC3.jar
pkgdiff pact-jvm-provider-sbt/target/scala-2.10/sbt-0.13//pact-jvm-provider-sbt-2.0-RC3.jar pact-jvm-provider-sbt/build/libs/pact-jvm-provider-sbt-2.0-RC3.jar
pkgdiff pact-jvm-provider-specs2/target/scala-2.10/pact-jvm-provider-specs2_2.10-2.0-RC3.jar pact-jvm-provider-specs2/build/libs/pact-jvm-provider-specs2-2.0-RC3.jar
pkgdiff pact-jvm-provider/target/scala-2.10/pact-jvm-provider_2.10-2.0-RC3.jar pact-jvm-provider/build/libs/pact-jvm-provider-2.0-RC3.jar
pkgdiff pact-jvm-server/target/scala-2.10/pact-jvm-server_2.10-2.0-RC3.jar pact-jvm-server/build/libs/pact-jvm-server-2.0-RC3.jar
find pkgdiff_reports/ -name changes_report.html | xargs open
