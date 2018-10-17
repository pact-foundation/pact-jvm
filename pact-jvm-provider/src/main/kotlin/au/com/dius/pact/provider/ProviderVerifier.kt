package au.com.dius.pact.provider

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.VerifierReporter
import groovy.lang.GroovyObjectSupport
import mu.KLogging
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

interface VerificationReporter {
  fun <I> reportResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient? = null, testResults: String? = null)
    where I: Interaction
}

@JvmOverloads
@Deprecated("Use the VerificationReporter instead of this function",
  ReplaceWith("DefaultVerificationReporter.reportResults(pact, result, version, client)"))
fun <I> reportVerificationResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient? = null)
  where I: Interaction = DefaultVerificationReporter.reportResults(pact, result, version, client, null)

object DefaultVerificationReporter : VerificationReporter {
  override fun <I> reportResults(pact: Pact<I>, result: Boolean, version: String, client: PactBrokerClient?, testResults: String?)
    where I: Interaction {
    val source = pact.source
    when (source) {
      is BrokerUrlSource -> {
        val brokerClient = client ?: PactBrokerClient(source.pactBrokerUrl, source.options)
        publishResult(brokerClient, source, result, testResults, version, pact)
      }
      else -> logger.info { "Skipping publishing verification results for source $source" }
    }
  }

  private fun <I> publishResult(brokerClient: PactBrokerClient, source: BrokerUrlSource, result: Boolean, testResults: String?, version: String, pact: Pact<I>) where I : Interaction {
    val publishResult = brokerClient.publishVerificationResults(source.attributes, result, version, testResults)
    if (publishResult is Err) {
      logger.error { "Failed to publish verification results - ${publishResult.error.localizedMessage}" }
      logger.debug(publishResult.error) {}
    } else {
      logger.info { "Published verification result of '$result' for consumer '${pact.consumer}'" }
    }
  }
}

enum class PactVerification {
  REQUEST_RESPONSE, ANNOTATED_METHOD
}

/**
 * Exception indicating failure to setup pact verification
 */
class PactVerifierException(
  override val message: String = "PactVerifierException",
  override val cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Annotation to mark a test method for provider verification
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class PactVerifyProvider(
  /**
   * the tested provider name.
   */
  val value: String
)

/**
 * Interface to the provider verifier
 */
interface IProviderVerifier {
  /**
   * List of the all reporters to report the results of the verification to
   */
  var reporters: List<VerifierReporter>

  /**
   * Callback to determine if something is a build specific task
   */
  var checkBuildSpecificTask: Function<Any, Boolean>

  /**
   * Consumer SAM to execute the build specific task
   */
  var executeBuildSpecificTask: BiConsumer<Any, ProviderState>

  /**
   * Callback to determine is the project has a particular property
   */
  var projectHasProperty: Function<String, Boolean>

  /**
   * Callback to return the instance for the provider method to invoke
   */
  var providerMethodInstance: Function<Method, Any>

  /**
   * Callback to return the project classpath to use for looking up methods
   */
  var projectClasspath: Supplier<List<URL>>

  /**
   * Reports the state of the interaction to all the registered reporters
   */
  fun reportStateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean)

  /**
   * Finalise all the reports after verification is complete
   */
  fun finaliseReports()

  /**
   * Displays all the failures from the verification run
   */
  fun displayFailures(failures: Map<String, Any>)

  /**
   * Verifies the response from the provider against the interaction
   */
  fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: Map<String, Any?> = emptyMap()
  ): Boolean

  /**
   * Verifies the interaction by invoking a method on a provider test class
   */
  fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): Boolean

  /**
   * Compares the expected and actual responses
   */
  fun verifyRequestResponsePact(
    expectedResponse: Response,
    actualResponse: Map<String, Any>,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): Boolean

  /**
   * If publishing of verification results has been disabled
   */
  fun publishingResultsDisabled(): Boolean
}

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
abstract class ProviderVerifierBase @JvmOverloads constructor (
  var pactLoadFailureMessage: Any? = null,
  override var checkBuildSpecificTask: Function<Any, Boolean> = Function { false },
  override var executeBuildSpecificTask: BiConsumer<Any, ProviderState> = BiConsumer { _, _ -> },
  override var projectClasspath: Supplier<List<URL>> = Supplier { emptyList<URL>() },
  override var reporters: List<VerifierReporter> = listOf(AnsiConsoleReporter()),
  override var providerMethodInstance: Function<Method, Any> = Function { m -> m.declaringClass.newInstance() },
  var providerVersion: Supplier<String> = Supplier { System.getProperty(PACT_PROVIDER_VERSION) }
) : GroovyObjectSupport(), IProviderVerifier {

  override var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }
  var verificationReporter: VerificationReporter = DefaultVerificationReporter

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  override fun publishingResultsDisabled(): Boolean {
    return !projectHasProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS) ||
      projectGetProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS)?.toLowerCase() != "true"
  }

  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): Boolean {
    try {
      val urls = projectClasspath.get()
      val loader = URLClassLoader(urls.toTypedArray(), ProviderVerifierBase::class.java.classLoader)
      val configurationBuilder = ConfigurationBuilder()
        .setScanners(MethodAnnotationsScanner())
        .addClassLoader(loader)
        .addUrls(urls)

      val scan = ProviderUtils.packagesToScan(providerInfo, consumer)
      if (scan.isNotEmpty()) {
        val filterBuilder = FilterBuilder()
        scan.forEach { filterBuilder.include(it) }
        configurationBuilder.filterInputsBy(filterBuilder)
      }

      val reflections = Reflections(configurationBuilder)
      val methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(PactVerifyProvider::class.java)
      val providerMethods = methodsAnnotatedWith.filter { m ->
        logger.debug { "Found annotated method $m" }
        val annotation = m.getAnnotation(PactVerifyProvider::class.java)
        logger.debug { "Found annotation $annotation" }
        annotation?.value == interaction.description
      }

      if (providerMethods.isEmpty()) {
        reporters.forEach { it.errorHasNoAnnotatedMethodsFoundForInteraction(interaction) }
        throw RuntimeException("No annotated methods were found for interaction " +
          "'${interaction.description}'. You need to provide a method annotated with " +
          "@PactVerifyProvider(\"${interaction.description}\") that returns the message contents.")
      } else {
        return if (interaction is Message) {
          verifyMessagePact(providerMethods.toHashSet(), interaction, interactionMessage, failures)
        } else {
          val expectedResponse = (interaction as RequestResponseInteraction).response
          var result = true
          providerMethods.forEach {
            val actualResponse = invokeProviderMethod(it, null) as Map<String, Any>
            result = result && this.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
          }
          result
        }
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach { it.verificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)) }
      return false
    }
  }

  fun displayBodyResult(failures: MutableMap<String, Any>, comparison: Map<String, Any>, comparisonDescription: String): Boolean {
    return if (comparison.isEmpty()) {
      reporters.forEach { it.bodyComparisonOk() }
      true
    } else {
      reporters.forEach { it.bodyComparisonFailed(comparison) }
      failures["$comparisonDescription has a matching body"] = comparison
      false
    }
  }

  fun verifyMessagePact(methods: Set<Method>, message: Message, interactionMessage: String, failures: MutableMap<String, Any>): Boolean {
    var result = true
    methods.forEach { method ->
      reporters.forEach { it.generatesAMessageWhich() }
      val actualMessage = OptionalBody.body(invokeProviderMethod(method, providerMethodInstance.apply(method)).toString())
      val comparison = ResponseComparison.compareMessage(message, actualMessage)
      val s = " generates a message which"
      result = result && displayBodyResult(failures, comparison, interactionMessage + s)
    }
    return result
  }

  override fun displayFailures(failures: Map<String, Any>) {
    reporters.forEach { it.displayFailures(failures) }
  }

  override fun finaliseReports() {
    reporters.forEach { it.finaliseReport() }
  }

  companion object : KLogging() {
    const val PACT_VERIFIER_PUBLISH_RESULTS = "pact.verifier.publishResults"
    const val PACT_FILTER_CONSUMERS = "pact.filter.consumers"
    const val PACT_FILTER_DESCRIPTION = "pact.filter.description"
    const val PACT_FILTER_PROVIDERSTATE = "pact.filter.providerState"
    const val PACT_SHOW_STACKTRACE = "pact.showStacktrace"
    const val PACT_SHOW_FULLDIFF = "pact.showFullDiff"
    const val PACT_PROVIDER_VERSION = "pact.provider.version"

    fun invokeProviderMethod(m: Method, instance: Any?): Any? {
      try {
        return m.invoke(instance)
      } catch (e: Throwable) {
        throw RuntimeException("Failed to invoke provider method '${m.name}'", e)
      }
    }
  }
}
