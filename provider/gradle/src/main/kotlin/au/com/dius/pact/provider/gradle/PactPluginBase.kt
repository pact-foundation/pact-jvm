package au.com.dius.pact.provider.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class PactPluginBase : Plugin<Project> {
  companion object {
    const val GROUP = "Pact"
    const val PACT_VERIFY = "pactVerify"
    const val TEST_CLASSES = "testClasses"
  }
}
