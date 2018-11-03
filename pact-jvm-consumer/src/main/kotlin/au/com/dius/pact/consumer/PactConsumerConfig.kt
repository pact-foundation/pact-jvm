package au.com.dius.pact.consumer

object PactConsumerConfig {
  val pactDirectory: String = System.getProperty("pact.rootDir", detectedBuildToolPactDirectory())

  private fun detectedBuildToolPactDirectory(): String =
    if (runsInsideGradle()) "build/pacts" else "target/pacts"

  private fun runsInsideGradle() = System.getenv("org.gradle.test.worker") != null
}
