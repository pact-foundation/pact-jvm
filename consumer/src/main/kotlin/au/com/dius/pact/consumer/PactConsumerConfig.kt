package au.com.dius.pact.consumer

import au.com.dius.pact.core.support.BuiltToolConfig.detectedBuildToolPactDirectory

@Deprecated("Use BuiltToolConfig")
object PactConsumerConfig {
  val pactDirectory: String = System.getProperty("pact.rootDir", detectedBuildToolPactDirectory())
}
