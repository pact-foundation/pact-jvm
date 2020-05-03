package au.com.dius.pact.consumer

import au.com.dius.pact.core.support.isNotEmpty

object PactConsumerConfig {
  val pactDirectory: String = System.getProperty("pact.rootDir", detectedBuildToolPactDirectory())

  private const val GRADLE_WORKER = "org.gradle.test.worker"

  private fun detectedBuildToolPactDirectory(): String =
    if (runsInsideGradle()) "build/pacts" else "target/pacts"

  private fun runsInsideGradle(): Boolean {
    return System.getenv(GRADLE_WORKER).isNotEmpty() || System.getProperty(GRADLE_WORKER).isNotEmpty()
  }
}
