package au.com.dius.pact.provider

import com.github.michaelbull.result.Result
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.VerifierReporter
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import groovy.lang.Closure
import io.github.classgraph.ClassGraph
import mu.KLogging
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.reflect.KMutableProperty1

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
  var providerVersion: Supplier<String?>

  /**
   * Callback to get the provider tag
   */
  var providerTag: Supplier<String?>?

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
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient
  ): VerificationResult

  /**
   * Verifies the response from the provider against the interaction
   */
  fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: Map<String, Any>,
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
    failures: MutableMap<String, Any>
  ): VerificationResult

  /**
   * Compares the expected and actual responses
   */
  fun verifyRequestResponsePact(
    expectedResponse: Response,
    actualResponse: Map<String, Any>,
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
}

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
@Suppress("TooManyFunctions")
open class ProviderVerifier @JvmOverloads constructor (
  override var pactLoadFailureMessage: Any? = null,
  override var checkBuildSpecificTask: Function<Any, Boolean> = Function { false },
  override var executeBuildSpecificTask: BiConsumer<Any, ProviderState> = BiConsumer { _, _ -> },
  override var projectClasspath: Supplier<List<URL>> = Supplier { emptyList<URL>() },
  override var reporters: List<VerifierReporter> = listOf(AnsiConsoleReporter("console", File("/tmp/"))),
  override var providerMethodInstance: Function<Method, Any> = Function { m -> m.declaringClass.newInstance() },
  override var providerVersion: Supplier<String?> = Supplier { System.getProperty(PACT_PROVIDER_VERSION) },
  override var providerTag: Supplier<String?>? = Supplier { System.getProperty(PACT_PROVIDER_TAG) }
) : IProviderVerifier {

  override var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  override var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }
  var verificationReporter: VerificationReporter = DefaultVerificationReporter
  var stateChangeHandler: StateChange = DefaultStateChange
  var pactReader: PactReader = DefaultPactReader

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  override fun publishingResultsDisabled(): Boolean {
    return when {
      !projectHasProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS) -> verificationReporter.publishingResultsDisabled()
      else -> projectGetProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS)?.toLowerCase() != "true"
    }
  }

  @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "SpreadOperator")
  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): VerificationResult {
    try {
      val urls = projectClasspath.get()
      logger.debug { "projectClasspath = $urls" }

      val classGraph = ClassGraph().enableAllInfo()
      if (System.getProperty("pact.verifier.classpathscan.verbose") != null) {
        classGraph.verbose()
      }

      if (urls.isNotEmpty()) {
        classGraph.overrideClassLoaders(URLClassLoader(urls.toTypedArray()))
      }

      val scan = ProviderUtils.packagesToScan(providerInfo, consumer)
      if (scan.isNotEmpty()) {
        classGraph.whitelistPackages(*scan.toTypedArray())
      }

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
        reporters.forEach { it.errorHasNoAnnotatedMethodsFoundForInteraction(interaction) }
        throw RuntimeException("No annotated methods were found for interaction " +
          "'${interaction.description}'. You need to provide a method annotated with " +
          "@PactVerifyProvider(\"${interaction.description}\") on the classpath that returns the message contents.")
      } else {
        return if (interaction is Message) {
          verifyMessagePact(methodsAnnotatedWith.toHashSet(), interaction, interactionMessage, failures,
            consumer.pending)
        } else {
          val expectedResponse = (interaction as RequestResponseInteraction).response
          var result: VerificationResult = VerificationResult.Ok
          methodsAnnotatedWith.forEach {
            val actualResponse = invokeProviderMethod(it, null) as Map<String, Any>
            result = result.merge(this.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage,
              failures, interaction.interactionId.orEmpty(), consumer.pending))
          }
          result
        }
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach { it.verificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)) }
      return VerificationResult.Failed(listOf(mapOf("message" to "Request to provider method failed with an exception",
        "exception" to e)),
        "Request to provider method failed with an exception", interactionMessage,
        listOf(VerificationFailureType.ExceptionFailure(e)), consumer.pending, interaction.interactionId)
    }
  }

  fun displayBodyResult(
    failures: MutableMap<String, Any>,
    comparison: Result<BodyComparisonResult, BodyTypeMismatch>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    return if (comparison is Ok && comparison.value.mismatches.isEmpty()) {
      reporters.forEach { it.bodyComparisonOk() }
      VerificationResult.Ok
    } else {
      reporters.forEach { it.bodyComparisonFailed(comparison) }
      when (comparison) {
        is Err -> {
          failures["$comparisonDescription has a matching body"] = comparison.error.description()
          VerificationResult.Failed(listOf(comparison.error.toMap() + ("type" to "body")),
            "Body had differences", comparisonDescription,
            listOf(VerificationFailureType.MismatchFailure(comparison.error)), pending, interactionId)
        }
        is Ok -> {
          failures["$comparisonDescription has a matching body"] = comparison.value
          VerificationResult.Failed(listOf(comparison.value.mismatches + ("type" to "body")),
            "Body had differences", comparisonDescription, comparison.value.mismatches.values.flatten()
              .map { VerificationFailureType.MismatchFailure(it) }, pending, interactionId)
        }
      }
    }
  }

  fun verifyMessagePact(
    methods: Set<Method>,
    message: Message,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    var result: VerificationResult = VerificationResult.Ok
    methods.forEach { method ->
      reporters.forEach { it.generatesAMessageWhich() }
      val messageResult = invokeProviderMethod(method, providerMethodInstance.apply(method))
      val actualMessage: ByteArray
      var messageMetadata: Map<String, Any>? = null
      var contentType = ContentType.JSON
      when (messageResult) {
        is MessageAndMetadata -> {
          messageMetadata = messageResult.metadata
          contentType = ContentType(Message.contentType(messageResult.metadata))
          actualMessage = messageResult.messageData
        }
        is Pair<*, *> -> {
          messageMetadata = messageResult.second as Map<String, Any>
          contentType = ContentType(Message.contentType(messageMetadata))
          actualMessage = messageResult.first.toString().toByteArray(contentType.asCharset())
        }
        is org.apache.commons.lang3.tuple.Pair<*, *> -> {
          messageMetadata = messageResult.right as Map<String, Any>
          contentType = ContentType(Message.contentType(messageMetadata))
          actualMessage = messageResult.left.toString().toByteArray(contentType.asCharset())
        }
        else -> {
          actualMessage = messageResult.toString().toByteArray()
        }
      }
      val comparison = ResponseComparison.compareMessage(message,
        OptionalBody.body(actualMessage, contentType), messageMetadata)
      val s = " generates a message which"
      result = result.merge(displayBodyResult(failures, comparison.bodyMismatches,
        interactionMessage + s, message.interactionId.orEmpty(), pending))
        .merge(displayMetadataResult(messageMetadata ?: emptyMap(), failures,
          comparison.metadataMismatches, interactionMessage + s, message.interactionId.orEmpty(),
          pending))
    }
    return result
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
      reporters.forEach { it.metadataComparisonOk() }
      VerificationResult.Ok
    } else {
      reporters.forEach { it.includesMetadata() }
      var result: VerificationResult = VerificationResult.Failed(emptyList(), "Metadata had differences",
        comparisonDescription, pending = pending, interactionId = interactionId)
      comparison.forEach { (key, metadataComparison) ->
        val expectedValue = expectedMetadata[key]
        if (metadataComparison.isEmpty()) {
          reporters.forEach { it.metadataComparisonOk(key, expectedValue) }
        } else {
          reporters.forEach { it.metadataComparisonFailed(key, expectedValue, metadataComparison) }
          failures["$comparisonDescription includes metadata \"$key\" with value \"$expectedValue\""] =
            metadataComparison
          result = result.merge(VerificationResult.Failed(listOf(mapOf(key to metadataComparison,
            "type" to "metadata")),
            verificationDescription = comparisonDescription,
            failures = metadataComparison.map { VerificationFailureType.MismatchFailure(it) }, pending = pending,
            interactionId = interactionId
          ))
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
    var interactionMessage = "Verifying a pact between ${consumer.name}"
    if (!consumer.name.contains(provider.name)) {
      interactionMessage += " and ${provider.name}"
    }
    interactionMessage += " - ${interaction.description}"

    val stateChangeResult = stateChangeHandler.executeStateChange(this, provider, consumer,
      interaction, interactionMessage, failures, providerClient)
    if (stateChangeResult.stateChangeResult is Ok) {
      interactionMessage = stateChangeResult.message
      reportInteractionDescription(interaction)

      val context = mapOf(
        "providerState" to stateChangeResult.stateChangeResult.value,
        "interaction" to interaction,
        "pending" to consumer.pending
      )

      val result = if (ProviderUtils.verificationType(provider, consumer) ==
        PactVerification.REQUEST_RESPONSE) {
        logger.debug { "Verifying via request/response" }
        verifyResponseFromProvider(provider, interaction as RequestResponseInteraction, interactionMessage, failures,
          providerClient, context, consumer.pending)
      } else {
        logger.debug { "Verifying via annotated test method" }
        verifyResponseByInvokingProviderMethods(provider, consumer, interaction, interactionMessage, failures)
      }

      if (provider.stateChangeTeardown) {
        stateChangeHandler.executeStateChangeTeardown(this, interaction, provider, consumer, providerClient)
      }

      return result
    } else {
      return VerificationResult.Failed(listOf(mapOf("message" to "State change request failed",
        "exception" to stateChangeResult.stateChangeResult.getError())), "State change request failed",
        stateChangeResult.message,
        listOf(VerificationFailureType.StateChangeFailure(stateChangeResult)),
        consumer.pending, interaction.interactionId
      )
    }
  }

  override fun reportInteractionDescription(interaction: Interaction) {
    reporters.forEach { it.interactionDescription(interaction) }
  }

  override fun generateErrorStringFromVerificationResult(result: List<VerificationResult.Failed>): String {
    val reporter = reporters.filterIsInstance<AnsiConsoleReporter>().firstOrNull()
    return reporter?.failuresToString(result) ?: "Test failed. Enable the console reporter to see the details"
  }

  override fun verifyRequestResponsePact(
    expectedResponse: Response,
    actualResponse: Map<String, Any>,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String,
    pending: Boolean
  ): VerificationResult {
    val comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
      actualResponse["statusCode"] as Int, actualResponse["headers"] as Map<String, List<String>>,
      actualResponse["data"] as String?)

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
      VerificationResult.Ok
    } else {
      reporters.forEach { it.statusComparisonFailed(status, mismatch.description()) }
      failures["$comparisonDescription has status code $status"] = mismatch.description()
      VerificationResult.Failed(listOf(mismatch.toMap() + ("type" to "status")),
        "Response status did not match", comparisonDescription,
        listOf(VerificationFailureType.MismatchFailure(mismatch)), pending, interactionId)
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
    return if (headers.isEmpty()) {
      VerificationResult.Ok
    } else {
      reporters.forEach { it.includesHeaders() }
      var result: VerificationResult = VerificationResult.Ok
      headers.forEach { (key, headerComparison) ->
        val expectedHeaderValue = expected[key]
        if (headerComparison.isEmpty()) {
          reporters.forEach { it.headerComparisonOk(key, expectedHeaderValue!!) }
        } else {
          reporters.forEach { it.headerComparisonFailed(key, expectedHeaderValue!!, headerComparison) }
          failures["$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""] =
            headerComparison.joinToString(", ") { it.description() }
          result = result.merge(VerificationResult.Failed(headerComparison.map { it.toMap() },
            "Headers had differences", comparisonDescription, headerComparison.map {
              VerificationFailureType.MismatchFailure(it)
            }, pending, interactionId
          ))
        }
      }
      result
    }
  }

  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient
  ) = verifyResponseFromProvider(provider, interaction, interactionMessage, failures, client, mapOf(), false)

  @Suppress("TooGenericExceptionCaught")
  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: Map<String, Any>,
    pending: Boolean
  ): VerificationResult {
    return try {
      val expectedResponse = interaction.response.generatedResponse(context)
      val actualResponse = client.makeRequest(interaction.request.generatedRequest(context))

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
        interaction.interactionId.orEmpty(), pending)
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      VerificationResult.Failed(listOf(mapOf("message" to "Request to provider failed with an exception",
        "exception" to e)),
        "Request to provider method failed with an exception", interactionMessage,
        listOf(VerificationFailureType.ExceptionFailure(e)), pending, interaction.interactionId)
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

  fun initialiseReporters(provider: IProviderInfo) {
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
    client: PactBrokerClient? = null
  ): VerificationResult {
    val pact = FilteredPact(loadPactFileForConsumer(consumer),
      Predicate { filterInteractions(it) })
    reportVerificationForConsumer(consumer, provider, pact.source)
    return if (pact.interactions.isEmpty()) {
      reporters.forEach { it.warnPactFileHasNoInteractions(pact as Pact<Interaction>) }
      VerificationResult.Ok
    } else {
      val result = pact.interactions.map {
        verifyInteraction(provider, consumer, failures, it)
      }.reduce { acc, result -> acc.merge(result) }
      when {
        pact.isFiltered() -> reporters.forEach { it.warnPublishResultsSkippedBecauseFiltered() }
        publishingResultsDisabled() -> reporters.forEach {
          it.warnPublishResultsSkippedBecauseDisabled(PACT_VERIFIER_PUBLISH_RESULTS)
        }
        else -> verificationReporter.reportResults(pact, result.toTestResult(), providerVersion.get() ?: "0.0.0",
          client, providerTag?.get())
      }
      result
    }
  }

  fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, pactSource: PactSource?) {
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
  fun loadPactFileForConsumer(consumer: IConsumerInfo): Pact<out Interaction> {
    var pactSource = consumer.pactSource
    if (pactSource is Callable<*>) {
      pactSource = pactSource.call()
    }

    if (projectHasProperty.apply(PACT_FILTER_PACTURL)) {
      val pactUrl = projectGetProperty.apply(PACT_FILTER_PACTURL)!!
      pactSource = if (pactSource is BrokerUrlSource) {
        pactSource.copy(url = pactUrl)
      } else {
        UrlSource<Interaction>(projectGetProperty.apply(PACT_FILTER_PACTURL)!!)
      }
      pactSource.encodePath = false
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
      is scala.Function1<*, *> -> (callback as scala.Function1<Any, Any>).apply(consumer).toString()
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
    const val PACT_FILTER_CONSUMERS = "pact.filter.consumers"
    const val PACT_FILTER_DESCRIPTION = "pact.filter.description"
    const val PACT_FILTER_PROVIDERSTATE = "pact.filter.providerState"
    const val PACT_FILTER_PACTURL = "pact.filter.pacturl"
    const val PACT_SHOW_STACKTRACE = "pact.showStacktrace"
    const val PACT_SHOW_FULLDIFF = "pact.showFullDiff"
    const val PACT_PROVIDER_VERSION = "pact.provider.version"
    const val PACT_PROVIDER_TAG = "pact.provider.tag"
    const val PACT_PROVIDER_VERSION_TRIM_SNAPSHOT = "pact.provider.version.trimSnapshot"

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    fun invokeProviderMethod(m: Method, instance: Any?): Any? {
      try {
        return m.invoke(instance)
      } catch (e: Throwable) {
        throw RuntimeException("Failed to invoke provider method '${m.name}'", e)
      }
    }
  }
}
