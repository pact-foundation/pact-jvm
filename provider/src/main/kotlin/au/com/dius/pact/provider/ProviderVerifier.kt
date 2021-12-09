package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessageInteraction
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.ifNullOrEmpty
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.Event
import au.com.dius.pact.provider.reporters.VerifierReporter
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import groovy.lang.Closure
import io.github.classgraph.ClassGraph
import mu.KLogging
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KMutableProperty1

enum class PactVerification {
  REQUEST_RESPONSE, ANNOTATED_METHOD, RESPONSE_FACTORY
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

data class MessageAndMetadata(val messageData: ByteArray, val metadata: Map<String, Any>) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MessageAndMetadata

    if (!messageData.contentEquals(other.messageData)) return false
    if (metadata != other.metadata) return false

    return true
  }

  override fun hashCode(): Int {
    var result = messageData.contentHashCode()
    result = 31 * result + metadata.hashCode()
    return result
  }
}

/**
 * Interface to the provider verifier
 */
@Suppress("TooManyFunctions")
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
   * Callback to fetch a project property
   */
  var projectGetProperty: Function<String, String?>

  /**
   * Callback to return the instance for the provider method to invoke
   */
  var providerMethodInstance: Function<Method, Any>

  /**
   * Callback to return the project classloader to use for looking up methods
   */
  var projectClassLoader: Supplier<ClassLoader?>?

  /**
   * Callback to return the project classpath to use for looking up methods
   */
  var projectClasspath: Supplier<List<URL>>

  /**
   * Callback to display a pact load error
   */
  var pactLoadFailureMessage: Any?

  /**
   * Callback to get the provider version
   */
  var providerVersion: Supplier<String>

  /**
   * Callback to get the provider tag
   */
  @Deprecated("Use version that returns multiple tags", replaceWith = ReplaceWith("providerTags"))
  var providerTag: Supplier<String?>?

  /**
   * Callback to get the provider branch
   */
  var providerBranch: Supplier<String?>?

  /**
   * Callback to get the provider tags
   */
  var providerTags: Supplier<List<String>>?

  /** Callback which is given an interaction description and returns a response */
  var responseFactory: Function<String, Any>?

  /**
   * Run the verification for the given provider and return any failures
   */
  fun verifyProvider(provider: ProviderInfo): List<VerificationResult>

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
  fun displayFailures(failures: List<VerificationResult.Failed>)

  /**
   * Verifies the response from the provider against the interaction
   */
  fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient
  ): VerificationResult

  /**
   * Verifies the response from the provider against the interaction
   */
  fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult

  /**
   * Verifies the interaction by invoking a method on a provider test class
   */
  fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult

  fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult

  /**
   * Compares the expected and actual responses
   */
  fun verifyRequestResponsePact(
    expectedResponse: IResponse,
    actualResponse: ProviderResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String,
    pending: Boolean
  ): VerificationResult

  /**
   * If publishing of verification results has been disabled
   */
  fun publishingResultsDisabled(): Boolean

  /**
   * Display info about the interaction about to be verified
   */
  fun reportInteractionDescription(interaction: Interaction)

  fun generateErrorStringFromVerificationResult(result: List<VerificationResult.Failed>): String

  fun reportStateChangeFailed(providerState: ProviderState, error: Exception, isSetup: Boolean)

  fun initialiseReporters(provider: IProviderInfo)

  fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, pactSource: PactSource?)

  /**
   * Source of the verification (Gradle/Maven/Junit)
   */
  var verificationSource: String?
}

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
@Suppress("TooManyFunctions", "LongParameterList")
open class ProviderVerifier @JvmOverloads constructor (
  override var pactLoadFailureMessage: Any? = null,
  override var checkBuildSpecificTask: Function<Any, Boolean> = Function { false },
  override var executeBuildSpecificTask: BiConsumer<Any, ProviderState> = BiConsumer { _, _ -> },
  override var projectClasspath: Supplier<List<URL>> = Supplier { emptyList<URL>() },
  override var reporters: List<VerifierReporter> = listOf(AnsiConsoleReporter("console", File("/tmp/"))),
  override var providerMethodInstance: Function<Method, Any> = Function { m -> m.declaringClass.newInstance() },
  override var providerVersion: Supplier<String> = ProviderVersion {
    SystemPropertyResolver.resolveValue(PACT_PROVIDER_VERSION, "")
  },
  override var providerTag: Supplier<String?>? = Supplier {
    SystemPropertyResolver.resolveValue(PACT_PROVIDER_TAG, "")
  },
  override var providerTags: Supplier<List<String>>? = Supplier {
    SystemPropertyResolver.resolveValue(PACT_PROVIDER_TAG, "")
      .orEmpty()
      .split(',')
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  },
  override var providerBranch: Supplier<String?>? = Supplier {
    SystemPropertyResolver.resolveValue(PACT_PROVIDER_BRANCH, "")
  },
  override var projectClassLoader: Supplier<ClassLoader?>? = null,
  override var responseFactory: Function<String, Any>? = null
) : IProviderVerifier {

  override var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  override var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }
  var verificationReporter: VerificationReporter = DefaultVerificationReporter
  var stateChangeHandler: StateChange = DefaultStateChange
  var pactReader: PactReader = DefaultPactReader
  override var verificationSource: String? = null

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  override fun publishingResultsDisabled(): Boolean {
    return when {
      !projectHasProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS) ->
        verificationReporter.publishingResultsDisabled(SystemPropertyResolver)
      else -> projectGetProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS)?.toLowerCase() != "true"
    }
  }

  @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "SpreadOperator", "LongParameterList")
  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    val interactionId = interaction.interactionId
    try {
      val classGraph = setupClassGraph(providerInfo, consumer)

      val methodsAnnotatedWith = classGraph.scan().use { scanResult ->
        scanResult.getClassesWithMethodAnnotation(PactVerifyProvider::class.qualifiedName)
          .flatMap { classInfo ->
            logger.debug { "found class $classInfo" }
            val methodInfo = classInfo.methodInfo.filter {
              it.annotationInfo.any { info ->
                info.name == PactVerifyProvider::class.qualifiedName &&
                  info.parameterValues["value"].value == interaction.description
              }
            }
            logger.debug { "found method $methodInfo" }
            methodInfo.map { it.loadClassAndGetMethod() }
          }
      }

      logger.debug { "Found methods = $methodsAnnotatedWith" }
      if (methodsAnnotatedWith.isEmpty()) {
        emitEvent(Event.ErrorHasNoAnnotatedMethodsFoundForInteraction(interaction))
        throw RuntimeException("No annotated methods were found for interaction " +
          "'${interaction.description}'. You need to provide a method annotated with " +
          "@PactVerifyProvider(\"${interaction.description}\") on the classpath that returns the message contents.")
      } else {
        return if (interaction.isAsynchronousMessage()) {
          verifyMessage(methodsAnnotatedWith.toHashSet(), interaction as MessageInteraction, interactionMessage,
            failures, pending)
        } else {
          val expectedResponse = (interaction as SynchronousRequestResponse).response
          var result: VerificationResult = VerificationResult.Ok(interactionId)
          methodsAnnotatedWith.forEach {
            val response = invokeProviderMethod(it, null) as Map<String, Any>
            val actualResponse = ProviderResponse(response["statusCode"] as Int,
              response["headers"] as Map<String, List<String>>, ContentType.UNKNOWN, response["data"] as String?)
            result = result.merge(this.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage,
              failures, interactionId.orEmpty(), pending))
          }
          result
        }
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      emitEvent(Event.VerificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)))
      val errors = listOf(
        VerificationFailureType.ExceptionFailure("Request to provider method failed with an exception", e)
      )
      return VerificationResult.Failed(
        "Request to provider method failed with an exception", interactionMessage,
        mapOf(interactionId.orEmpty() to errors), pending)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  override fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    val interactionId = interaction.interactionId.orEmpty()
    try {
      val factory = responseFactory!!
      return if (interaction.isAsynchronousMessage()) {
        verifyMessage(
          factory,
          interaction as MessageInteraction,
          interactionId,
          interactionMessage,
          failures,
          pending
        )
      } else {
        val expectedResponse = (interaction as SynchronousRequestResponse).response
        val response = factory.apply(interaction.description) as Map<String, Any>
        val contentType = response["contentType"] as String?
        val actualResponse = ProviderResponse(
          response["statusCode"] as Int,
          response["headers"] as Map<String, List<String>>,
          if (contentType == null) ContentType.UNKNOWN else ContentType(contentType),
          response["data"] as String?
        )
        this.verifyRequestResponsePact(
          expectedResponse,
          actualResponse,
          interactionMessage,
          failures,
          interactionId,
          pending
        )
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      emitEvent(Event.VerificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)))
      val errors = listOf(
        VerificationFailureType.ExceptionFailure("Verification factory method failed with an exception", e)
      )
      return VerificationResult.Failed(
        "Verification factory method failed with an exception", interactionMessage,
        mapOf(interactionId to errors), pending)
    }
  }

  private fun setupClassGraph(providerInfo: IProviderInfo, consumer: IConsumerInfo): ClassGraph {
    val classGraph = ClassGraph().enableAllInfo()
    if (System.getProperty("pact.verifier.classpathscan.verbose") != null) {
      classGraph.verbose()
    }

    val classLoader = projectClassLoader?.get()
    if (classLoader == null) {
      val urls = projectClasspath.get()
      logger.debug { "projectClasspath = $urls" }
      if (urls.isNotEmpty()) {
        classGraph.overrideClassLoaders(URLClassLoader(urls.toTypedArray()))
      }
    } else {
      classGraph.overrideClassLoaders(classLoader)
    }

    val scan = ProviderUtils.packagesToScan(providerInfo, consumer)
    if (scan.isNotEmpty()) {
      classGraph.whitelistPackages(*scan.toTypedArray())
    }
    return classGraph
  }

  private fun emitEvent(event: Event) {
    reporters.forEach { it.receive(event) }
  }

  fun displayBodyResult(
    failures: MutableMap<String, Any>,
    comparison: Result<BodyComparisonResult, BodyTypeMismatch>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    return if (comparison is Ok && comparison.value.mismatches.isEmpty()) {
      emitEvent(Event.BodyComparisonOk)
      VerificationResult.Ok(interactionId)
    } else {
      emitEvent(Event.BodyComparisonFailed(comparison))
      val description = "$comparisonDescription has a matching body"
      when (comparison) {
        is Err -> {
          failures[description] = comparison.error.description()
          VerificationResult.Failed("Body had differences", description,
            mapOf(interactionId to listOf(VerificationFailureType.MismatchFailure(comparison.error))), pending)
        }
        is Ok -> {
          failures[description] = comparison.value
          VerificationResult.Failed("Body had differences", description,
            mapOf(interactionId to comparison.value.mismatches.values.flatten()
              .map { VerificationFailureType.MismatchFailure(it) }), pending)
        }
      }
    }
  }

  fun verifyMessage(
    methods: Set<Method>,
    message: MessageInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    val interactionId = message.interactionId
    var result: VerificationResult = VerificationResult.Ok(interactionId)
    methods.forEach { method ->
      val messageFactory: Function<String, Any> =
        Function { invokeProviderMethod(method, providerMethodInstance.apply(method))!! }
      result = result.merge(verifyMessage(
        messageFactory,
        message,
        interactionId.orEmpty(),
        interactionMessage,
        failures,
        pending
      ))
    }
    return result
  }

  private fun verifyMessage(
    messageFactory: Function<String, Any>,
    message: MessageInteraction,
    interactionId: String,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    emitEvent(Event.GeneratesAMessageWhich)
    val messageResult = messageFactory.apply(message.description)
    val actualMessage: ByteArray
    var messageMetadata: Map<String, Any>? = null
    var contentType = ContentType.JSON
    when (messageResult) {
      is MessageAndMetadata -> {
        messageMetadata = messageResult.metadata
        contentType = Message.contentType(messageResult.metadata)
        actualMessage = messageResult.messageData
      }
      is Pair<*, *> -> {
        messageMetadata = messageResult.second as Map<String, Any>
        contentType = Message.contentType(messageMetadata)
        actualMessage = messageResult.first.toString().toByteArray(contentType.asCharset())
      }
      is org.apache.commons.lang3.tuple.Pair<*, *> -> {
        messageMetadata = messageResult.right as Map<String, Any>
        contentType = Message.contentType(messageMetadata)
        actualMessage = messageResult.left.toString().toByteArray(contentType.asCharset())
      }
      else -> {
        actualMessage = messageResult.toString().toByteArray()
      }
    }
    val comparison = ResponseComparison.compareMessage(message,
      OptionalBody.body(actualMessage, contentType), messageMetadata)
    val s = ": generates a message which"
    return displayBodyResult(
      failures,
      comparison.bodyMismatches,
      interactionMessage + s,
      interactionId,
      pending
    ).merge(
      displayMetadataResult(
        messageMetadata ?: emptyMap(),
        failures,
        comparison.metadataMismatches,
        interactionMessage + s,
        interactionId,
        pending
      )
    )
  }

  private fun displayMetadataResult(
    expectedMetadata: Map<String, Any>,
    failures: MutableMap<String, Any>,
    comparison: Map<String, List<MetadataMismatch>>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    return if (comparison.isEmpty()) {
      emitEvent(Event.MetadataComparisonOk())
      VerificationResult.Ok(interactionId)
    } else {
      emitEvent(Event.IncludesMetadata)
      var result: VerificationResult = VerificationResult.Failed("Metadata had differences",
        comparisonDescription, pending = pending)
      comparison.forEach { (key, metadataComparison) ->
        val expectedValue = expectedMetadata[key]
        if (metadataComparison.isEmpty()) {
          emitEvent(Event.MetadataComparisonOk(key, expectedValue))
        } else {
          emitEvent(Event.MetadataComparisonFailed(key, expectedValue, metadataComparison))
          val description = "$comparisonDescription includes metadata \"$key\" with value \"$expectedValue\""
          failures[description] = metadataComparison
          result = result.merge(VerificationResult.Failed("", description,
            mapOf(interactionId to metadataComparison.map { VerificationFailureType.MismatchFailure(it) }), pending))
        }
      }
      result
    }
  }

  override fun displayFailures(failures: List<VerificationResult.Failed>) {
    reporters.forEach { it.displayFailures(failures) }
  }

  override fun finaliseReports() {
    reporters.forEach { it.finaliseReport() }
  }

  @JvmOverloads
  fun verifyInteraction(
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    failures: MutableMap<String, Any>,
    interaction: Interaction,
    providerClient: ProviderClient = ProviderClient(provider, HttpClientFactory())
  ): VerificationResult {
    Metrics.sendMetrics(MetricEvent.ProviderVerificationRan(1, verificationSource.ifNullOrEmpty { "unknown" }!!))

    var interactionMessage = "Verifying a pact between ${consumer.name}"
    if (!consumer.name.contains(provider.name)) {
      interactionMessage += " and ${provider.name}"
    }
    interactionMessage += " - ${interaction.description}"

    var pending = consumer.pending
    if (interaction.isV4() && interaction.asV4Interaction().pending) {
      interactionMessage += " [PENDING]"
      pending = true
    }

    val stateChangeResult = stateChangeHandler.executeStateChange(this, provider, consumer,
      interaction, interactionMessage, failures, providerClient)
    if (stateChangeResult.stateChangeResult is Ok) {
      interactionMessage = stateChangeResult.message
      reportInteractionDescription(interaction)

      val context = mutableMapOf(
        "providerState" to stateChangeResult.stateChangeResult.value,
        "interaction" to interaction,
        "pending" to consumer.pending,
        "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
      )

      val result = when (ProviderUtils.verificationType(provider, consumer)) {
        PactVerification.REQUEST_RESPONSE -> {
          logger.debug { "Verifying via request/response" }
          verifyResponseFromProvider(
            provider, interaction.asSynchronousRequestResponse()!!, interactionMessage, failures, providerClient,
            context, pending)
        }
        PactVerification.RESPONSE_FACTORY -> {
          logger.debug { "Verifying via response factory function" }
          verifyResponseByFactory(provider, consumer, interaction, interactionMessage, failures, pending)
        }
        else -> {
          logger.debug { "Verifying via provider methods" }
          verifyResponseByInvokingProviderMethods(
            provider, consumer, interaction, interactionMessage, failures, pending)
        }
      }

      if (provider.stateChangeTeardown) {
        stateChangeHandler.executeStateChangeTeardown(this, interaction, provider, consumer, providerClient)
      }

      return result
    } else {
      return VerificationResult.Failed("State change request failed",
        stateChangeResult.message,
        mapOf(interaction.interactionId.orEmpty() to
          listOf(VerificationFailureType.StateChangeFailure("Provider state change callback failed", stateChangeResult))
        ), pending)
    }
  }

  override fun reportInteractionDescription(interaction: Interaction) {
    emitEvent(Event.InteractionDescription(interaction))
    if (interaction.comments.isNotEmpty()) {
      emitEvent(Event.DisplayInteractionComments(interaction.comments))
    }
  }

  override fun generateErrorStringFromVerificationResult(result: List<VerificationResult.Failed>): String {
    val reporter = reporters.filterIsInstance<AnsiConsoleReporter>().firstOrNull()
    return reporter?.failuresToString(result) ?: "Test failed. Enable the console reporter to see the details"
  }

  override fun reportStateChangeFailed(providerState: ProviderState, error: Exception, isSetup: Boolean) {
    reporters.forEach { it.stateChangeRequestFailedWithException(providerState.name.toString(), isSetup,
      error, projectHasProperty.apply(PACT_SHOW_STACKTRACE)) }
  }

  override fun verifyRequestResponsePact(
    expectedResponse: IResponse,
    actualResponse: ProviderResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    val comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse)

    reporters.forEach { it.returnsAResponseWhich() }

    return displayStatusResult(failures, expectedResponse.status, comparison.statusMismatch,
        interactionMessage, interactionId, pending)
      .merge(displayHeadersResult(failures, expectedResponse.headers, comparison.headerMismatches,
        interactionMessage, interactionId, pending))
      .merge(displayBodyResult(failures, comparison.bodyMismatches,
        interactionMessage, interactionId, pending))
  }

  fun displayStatusResult(
    failures: MutableMap<String, Any>,
    status: Int,
    mismatch: StatusMismatch?,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    return if (mismatch == null) {
      reporters.forEach { it.statusComparisonOk(status) }
      VerificationResult.Ok(interactionId)
    } else {
      reporters.forEach { it.statusComparisonFailed(status, mismatch.description()) }
      val description = "$comparisonDescription: has status code $status"
      failures[description] = mismatch.description()
      VerificationResult.Failed("Response status did not match", description,
        mapOf(interactionId to listOf(VerificationFailureType.MismatchFailure(mismatch))), pending)
    }
  }

  fun displayHeadersResult(
    failures: MutableMap<String, Any>,
    expected: Map<String, List<String>>,
    headers: Map<String, List<HeaderMismatch>>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    val ok = VerificationResult.Ok(interactionId)
    return if (headers.isEmpty()) {
      ok
    } else {
      reporters.forEach { it.includesHeaders() }
      var result: VerificationResult = ok
      headers.forEach { (key, headerComparison) ->
        val expectedHeaderValue = expected[key]
        if (headerComparison.isEmpty()) {
          reporters.forEach { it.headerComparisonOk(key, expectedHeaderValue!!) }
        } else {
          reporters.forEach { it.headerComparisonFailed(key, expectedHeaderValue!!, headerComparison) }
          val description = "$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""
          failures[description] = headerComparison.joinToString(", ") { it.description() }
          result = result.merge(VerificationResult.Failed("Headers had differences", description,
            mapOf(interactionId to headerComparison.map {
              VerificationFailureType.MismatchFailure(it)
            }), pending))
        }
      }
      result
    }
  }

  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient
  ) = verifyResponseFromProvider(provider, interaction, interactionMessage, failures, client, mutableMapOf(), false)

  @Suppress("TooGenericExceptionCaught")
  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    return try {
      val expectedResponse = interaction.response.generatedResponse(context, GeneratorTestMode.Provider)
      val actualResponse = client.makeRequest(interaction.request.generatedRequest(context, GeneratorTestMode.Provider))

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
        interaction.interactionId.orEmpty(), pending)
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      VerificationResult.Failed("Request to provider endpoint failed with an exception", interactionMessage,
        mapOf(interaction.interactionId.orEmpty() to
          listOf(VerificationFailureType.ExceptionFailure("Request to provider endpoint failed with an exception", e))),
        pending)
    }
  }

  override fun verifyProvider(provider: ProviderInfo): List<VerificationResult> {
    initialiseReporters(provider)

    val consumers = provider.consumers.filter(::filterConsumers)
    if (consumers.isEmpty()) {
      reporters.forEach { it.warnProviderHasNoConsumers(provider) }
    }

    return consumers.map {
      runVerificationForConsumer(mutableMapOf(), provider, it)
    }
  }

  override fun initialiseReporters(provider: IProviderInfo) {
    reporters.forEach {
      if (it.hasProperty("displayFullDiff")) {
        (it.property("displayFullDiff") as KMutableProperty1<VerifierReporter, Boolean>)
          .set(it, projectHasProperty.apply(PACT_SHOW_FULLDIFF))
      }
      it.verifier = this
      it.initialise(provider)
    }
  }

  @JvmOverloads
  fun runVerificationForConsumer(
    failures: MutableMap<String, Any>,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    client: IPactBrokerClient? = null
  ): VerificationResult {
    val pact = FilteredPact(loadPactFileForConsumer(consumer)) { filterInteractions(it) }
    reportVerificationForConsumer(consumer, provider, pact.source)
    return if (pact.interactions.isEmpty()) {
      reporters.forEach { it.warnPactFileHasNoInteractions(pact as Pact) }
      VerificationResult.Ok()
    } else {
      val result = pact.interactions.map {
        verifyInteraction(provider, consumer, failures, it)
      }.reduce { acc, result -> acc.merge(result) }
      result.merge(when {
        pact.isFiltered() -> {
          reporters.forEach { it.warnPublishResultsSkippedBecauseFiltered() }
          VerificationResult.Ok()
        }
        publishingResultsDisabled() -> {
          reporters.forEach {
            it.warnPublishResultsSkippedBecauseDisabled(PACT_VERIFIER_PUBLISH_RESULTS)
          }
          VerificationResult.Ok()
        }
        else -> {
          val reportResults = verificationReporter.reportResults(pact,
            result.toTestResult(),
            providerVersion.get(),
            client,
            providerTags?.get().orEmpty(),
            providerBranch?.get().orEmpty())
          when (reportResults) {
            is Ok -> VerificationResult.Ok()
            is Err -> VerificationResult.Failed("Failed to publish results to the Pact broker", "",
              mapOf("" to listOf(VerificationFailureType.PublishResultsFailure(reportResults.error))))
          }
        }
      })
    }
  }

  override fun reportVerificationForConsumer(
    consumer: IConsumerInfo,
    provider: IProviderInfo,
    pactSource: PactSource?
  ) {
    when (pactSource) {
      is BrokerUrlSource -> reporters.forEach { reporter ->
        reporter.reportVerificationForConsumer(consumer, provider, pactSource.tag)
        val notices = consumer.notices.filter { it.`when` == "before_verification" }
        if (notices.isNotEmpty()) {
          reporter.reportVerificationNoticesForConsumer(consumer, provider, notices)
        }
        reporter.verifyConsumerFromUrl(pactSource, consumer)
      }
      is UrlPactSource -> reporters.forEach {
        it.reportVerificationForConsumer(consumer, provider, null)
        it.verifyConsumerFromUrl(pactSource, consumer)
      }
      else -> reporters.forEach {
        it.reportVerificationForConsumer(consumer, provider, null)
        if (pactSource != null) {
          it.verifyConsumerFromFile(pactSource, consumer)
        }
      }
    }
  }

  @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
  fun loadPactFileForConsumer(consumer: IConsumerInfo): Pact {
    var pactSource = consumer.resolvePactSource()

    if (projectHasProperty.apply(PACT_FILTER_PACTURL)) {
      val pactUrl = projectGetProperty.apply(PACT_FILTER_PACTURL)!!
      pactSource = if (pactSource is BrokerUrlSource) {
        val source = pactSource.copy(url = pactUrl)
        source.encodePath = false
        source
      } else {
        val source = UrlSource(projectGetProperty.apply(PACT_FILTER_PACTURL)!!)
        source.encodePath = false
        source
      }
    }

    return if (pactSource is UrlPactSource) {
      val options = mutableMapOf<String, Any>()
      if (consumer.pactFileAuthentication.isNotEmpty()) {
        options["authentication"] = consumer.pactFileAuthentication
      }
      pactReader.loadPact(pactSource, options)
    } else {
      try {
        pactReader.loadPact(pactSource!!)
      } catch (e: Exception) {
        logger.error(e) { "Failed to load pact file" }
        val message = generateLoadFailureMessage(consumer)
        reporters.forEach { it.pactLoadFailureForConsumer(consumer, message) }
        throw RuntimeException(message)
      }
    }
  }

  private fun generateLoadFailureMessage(consumer: IConsumerInfo): String {
    return when (val callback = pactLoadFailureMessage) {
      is Closure<*> -> callback.call(consumer).toString()
      is Function<*, *> -> (callback as Function<Any, Any>).apply(consumer).toString()
      else -> callback as String
    }
  }

  fun filterConsumers(consumer: IConsumerInfo): Boolean {
    return !projectHasProperty.apply(PACT_FILTER_CONSUMERS) ||
      consumer.name in projectGetProperty.apply(PACT_FILTER_CONSUMERS).toString().split(',').map { it.trim() }
  }

  fun filterInteractions(interaction: Interaction): Boolean {
    return if (projectHasProperty.apply(PACT_FILTER_DESCRIPTION) &&
      projectHasProperty.apply(PACT_FILTER_PROVIDERSTATE)) {
      matchDescription(interaction) && matchState(interaction)
    } else if (projectHasProperty.apply(PACT_FILTER_DESCRIPTION)) {
      matchDescription(interaction)
    } else if (projectHasProperty.apply(PACT_FILTER_PROVIDERSTATE)) {
      matchState(interaction)
    } else {
      true
    }
  }

  private fun matchState(interaction: Interaction): Boolean {
    return if (interaction.providerStates.isNotEmpty()) {
      interaction.providerStates.any {
        projectGetProperty.apply(PACT_FILTER_PROVIDERSTATE)?.toRegex()?.matches(it.name.toString()) ?: true }
    } else {
      projectGetProperty.apply(PACT_FILTER_PROVIDERSTATE).isNullOrEmpty()
    }
  }

  private fun matchDescription(interaction: Interaction): Boolean {
    return projectGetProperty.apply(PACT_FILTER_DESCRIPTION)?.toRegex()?.matches(interaction.description) ?: true
  }

  override fun reportStateForInteraction(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean
  ) {
    reporters.forEach { it.stateForInteraction(state, provider, consumer, isSetup) }
  }

  companion object : KLogging() {
    const val PACT_VERIFIER_PUBLISH_RESULTS = "pact.verifier.publishResults"
    const val PACT_VERIFIER_BUILD_URL = "pact.verifier.buildUrl"
    const val PACT_FILTER_CONSUMERS = "pact.filter.consumers"
    const val PACT_FILTER_DESCRIPTION = "pact.filter.description"
    const val PACT_FILTER_PROVIDERSTATE = "pact.filter.providerState"
    const val PACT_FILTER_PACTURL = "pact.filter.pacturl"
    const val PACT_SHOW_STACKTRACE = "pact.showStacktrace"
    const val PACT_SHOW_FULLDIFF = "pact.showFullDiff"
    const val PACT_PROVIDER_VERSION = "pact.provider.version"
    const val PACT_PROVIDER_TAG = "pact.provider.tag"
    const val PACT_PROVIDER_BRANCH = "pact.provider.branch"
    const val PACT_PROVIDER_VERSION_TRIM_SNAPSHOT = "pact.provider.version.trimSnapshot"

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    fun invokeProviderMethod(m: Method, instance: Any?): Any? {
      try {
        m.isAccessible = true
        return m.invoke(instance)
      } catch (e: Throwable) {
        throw RuntimeException("Failed to invoke provider method '${m.name}'", e)
      }
    }
  }
}
