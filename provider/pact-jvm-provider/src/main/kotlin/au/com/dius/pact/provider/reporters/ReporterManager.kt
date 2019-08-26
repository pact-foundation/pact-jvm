package au.com.dius.pact.provider.reporters

import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * Manages the available verifier reporters
 */
object ReporterManager {
  private val REPORTERS = mapOf(
    "console" to AnsiConsoleReporter::class,
    "markdown" to MarkdownReporter::class,
    "json" to JsonReporter::class
  )

  @JvmStatic
  fun reporterDefined(name: String) = REPORTERS.containsKey(name)

  @JvmStatic
  fun createReporter(name: String, reportDir: File): VerifierReporter {
    val reporter: VerifierReporter = if (reporterDefined(name)) {
      try {
        REPORTERS[name]!!.constructors.first { it.parameters.size == 2 }.call(name, reportDir)
      } catch (e: Exception) {
        throw RuntimeException("Verifier reporters must have a constructor that accepts two parameters: " +
          "(name: String, reportDir: File)", e)
      }
    } else {
      // maybe name is a fully qualified name
      try {
        val loader = ReporterManager::class.java.classLoader
        val instance = loader.loadClass(name)?.kotlin?.constructors
          ?.first { it.parameters.size == 2 }?.call(name, reportDir)
          ?: throw IllegalArgumentException("No reporter with name '$name' found in classpath")

        require(instance is VerifierReporter) { "Reporter with name '$name' does not implement VerifierReporter" }

        instance
      } catch (e: Exception) {
        throw IllegalArgumentException("No reporter with class '$name' defined. Verifier reporters must have a " +
          "constructor that accepts two parameters: (name: String, reportDir: File)", e)
      } as VerifierReporter
    }

    val nameProp = reporter::class.memberProperties.find { it.name == "name" }
    if (nameProp is KMutableProperty1<*, *>) {
      (nameProp as KMutableProperty1<Any, String>).set(reporter, name)
    }
    return reporter
  }

  @JvmStatic
  fun availableReporters() = REPORTERS.keys.toList()
}
