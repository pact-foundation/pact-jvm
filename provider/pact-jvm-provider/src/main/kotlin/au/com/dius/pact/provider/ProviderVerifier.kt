package au.com.dius.pact.provider

import arrow.core.Either
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.getError
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
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.VerifierReporter
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
   * Run the verification for the given provider and return an failures in a Map
   */
  fun verifyProvider(provider: ProviderInfo): MutableMap<String, Any>

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
    client: ProviderClient
  ): TestResult

  /**
   * Verifies the response from the provider against the interaction
   */
  fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: Map<String, Any>
  ): TestResult

  /**
   * Verifies the interaction by invoking a method on a provider test class
   */
  fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): TestResult

  /**
   * Compares the expected and actual responses
   */
  fun verifyRequestResponsePact(
    expectedResponse: Response,
    actualResponse: Map<String, Any>,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String
  ): TestResult

  /**
   * If publishing of verification results has been disabled
   */
  fun publishingResultsDisabled(): Boolean

  /**
   * Display info about the interaction about to be verified
   */
  fun reportInteractionDescription(interaction: Interaction)
}

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
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

  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): TestResult {
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
          verifyMessagePact(methodsAnnotatedWith.toHashSet(), interaction, interactionMessage, failures)
        } else {
          val expectedResponse = (interaction as RequestResponseInteraction).response
          var result: TestResult = TestResult.Ok
          methodsAnnotatedWith.forEach {
            val actualResponse = invokeProviderMethod(it, null) as Map<String, Any>
            result = result.merge(this.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage,
              failures, interaction.interactionId.orEmpty()))
          }
          result
        }
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach { it.verificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)) }
      return TestResult.Failed(listOf(mapOf("message" to "Request to provider method failed with an exception",
        "exception" to e, "interactionId" to interaction.interactionId)),
        "Request to provider method failed with an exception")
    }
  }

  fun displayBodyResult(
    failures: MutableMap<String, Any>,
    comparison: Either<BodyTypeMismatch, BodyComparisonResult>,
    comparisonDescription: String,
    interactionId: String
  ): TestResult {
    return if (comparison is Either.Right && comparison.b.mismatches.isEmpty()) {
      reporters.forEach { it.bodyComparisonOk() }
      TestResult.Ok
    } else {
      reporters.forEach { it.bodyComparisonFailed(comparison) }
      when (comparison) {
        is Either.Left -> {
          failures["$comparisonDescription has a matching body"] = comparison.a.description()
          TestResult.Failed(listOf(comparison.a.toMap() + mapOf("interactionId" to interactionId, "type" to "body")),
            "Body had differences")
        }
        is Either.Right -> {
          failures["$comparisonDescription has a matching body"] = comparison.b
          TestResult.Failed(listOf(comparison.b.mismatches + mapOf("interactionId" to interactionId, "type" to "body")),
            "Body had differences")
        }
      }
    }
  }

  fun verifyMessagePact(
    methods: Set<Method>,
    message: Message,
    interactionMessage: String,
    failures: MutableMap<String, Any>
  ): TestResult {
    var result: TestResult = TestResult.Ok
    methods.forEach { method ->
      reporters.forEach { it.generatesAMessageWhich() }
      val messageResult = invokeProviderMethod(method, providerMethodInstance.apply(method))
      val actualMessage: ByteArray
      var messageMetadata: Map<String, Any>? = null
      var contentType = ContentType.JSON
      when (messageResult) {
        is MessageAndMetadata -> {
          messageMetadata = messageResult.metadata
          contentType = ContentType(Message.getContentType(messageResult.metadata))
          actualMessage = messageResult.messageData
        }
        is Pair<*, *> -> {
          messageMetadata = messageResult.second as Map<String, Any>
          contentType = ContentType(Message.getContentType(messageMetadata))
          actualMessage = messageResult.first.toString().toByteArray(contentType.asCharset())
        }
        is org.apache.commons.lang3.tuple.Pair<*, *> -> {
          messageMetadata = messageResult.right as Map<String, Any>
          contentType = ContentType(Message.getContentType(messageMetadata))
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
        interactionMessage + s, message.interactionId.orEmpty()))
        .merge(displayMetadataResult(messageMetadata ?: emptyMap(), failures,
          comparison.metadataMismatches, interactionMessage + s, message.interactionId.orEmpty()))
    }
    return result
  }

  private fun displayMetadataResult(
    expectedMetadata: Map<String, Any>,
    failures: MutableMap<String, Any>,
    comparison: Map<String, List<MetadataMismatch>>,
    comparisonDescription: String,
    interactionId: String
  ): TestResult {
    return if (comparison.isEmpty()) {
      reporters.forEach { it.metadataComparisonOk() }
      TestResult.Ok
    } else {
      reporters.forEach { it.includesMetadata() }
      var result: TestResult = TestResult.Failed(emptyList(), "Metadata had differences")
      comparison.forEach { (key, metadataComparison) ->
        val expectedValue = expectedMetadata[key]
        if (metadataComparison.isEmpty()) {
          reporters.forEach { it.metadataComparisonOk(key, expectedValue) }
        } else {
          reporters.forEach { it.metadataComparisonFailed(key, expectedValue, metadataComparison) }
          failures["$comparisonDescription includes metadata \"$key\" with value \"$expectedValue\""] =
            metadataComparison
          result = result.merge(TestResult.Failed(listOf(mapOf(key to metadataComparison,
            "interactionId" to interactionId, "type" to "metadata"))))
        }
      }
      result
    }
  }

  override fun displayFailures(failures: Map<String, Any>) {
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
  ): TestResult {
    var interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name}" +
    " - ${interaction.description} "

    val stateChangeResult = stateChangeHandler.executeStateChange(this, provider, consumer,
      interaction, interactionMessage, failures, providerClient)
    if (stateChangeResult.stateChangeResult is Ok) {
      interactionMessage = stateChangeResult.message
      reportInteractionDescription(interaction)

      val context = mapOf(
        "providerState" to stateChangeResult.stateChangeResult.value,
        "interaction" to interaction
      )

      val result = if (ProviderUtils.verificationType(provider, consumer) ==
        PactVerification.REQUEST_RESPONSE) {
        logger.debug { "Verifying via request/response" }
        verifyResponseFromProvider(provider, interaction as RequestResponseInteraction, interactionMessage, failures,
          providerClient, context)
      } else {
        logger.debug { "Verifying via annotated test method" }
        verifyResponseByInvokingProviderMethods(provider, consumer, interaction, interactionMessage, failures)
      }

      if (provider.stateChangeTeardown) {
        stateChangeHandler.executeStateChangeTeardown(this, interaction, provider, consumer, providerClient)
      }

      return result
    } else {
      return TestResult.Failed(listOf(mapOf("message" to "State change request failed",
        "exception" to stateChangeResult.stateChangeResult.getError(),
        "interactionId" to interaction.interactionId)), "State change request failed")
    }
  }

  override fun reportInteractionDescription(interaction: Interaction) {
    reporters.forEach { it.interactionDescription(interaction) }
  }

  override fun verifyRequestResponsePact(
    expectedResponse: Response,
    actualResponse: Map<String, Any>,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String
  ): TestResult {
    val comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
      actualResponse["statusCode"] as Int, actualResponse["headers"] as Map<String, List<String>>,
      actualResponse["data"] as String?)

    reporters.forEach { it.returnsAResponseWhich() }

    val s = " returns a response which"
    return displayStatusResult(failures, expectedResponse.status, comparison.statusMismatch,
      interactionMessage + s, interactionId)
      .merge(displayHeadersResult(failures, expectedResponse.headers, comparison.headerMismatches,
        interactionMessage + s, interactionId))
      .merge(displayBodyResult(failures, comparison.bodyMismatches,
        interactionMessage + s, interactionId))
  }

  fun displayStatusResult(
    failures: MutableMap<String, Any>,
    status: Int,
    mismatch: StatusMismatch?,
    comparisonDescription: String,
    interactionId: String
  ): TestResult {
    return if (mismatch == null) {
      reporters.forEach { it.statusComparisonOk(status) }
      TestResult.Ok
    } else {
      reporters.forEach { it.statusComparisonFailed(status, mismatch.description()) }
      failures["$comparisonDescription has statusResult code $status"] = mismatch.description()
      TestResult.Failed(listOf(mismatch.toMap() + mapOf("interactionId" to interactionId,
        "type" to "status")), "Response status did not match")
    }
  }

  fun displayHeadersResult(
    failures: MutableMap<String, Any>,
    expected: Map<String, List<String>>,
    headers: Map<String, List<HeaderMismatch>>,
    comparisonDescription: String,
    interactionId: String
  ): TestResult {
    return if (headers.isEmpty()) {
      TestResult.Ok
    } else {
      reporters.forEach { it.includesHeaders() }
      var result: TestResult = TestResult.Ok
      headers.forEach { (key, headerComparison) ->
        val expectedHeaderValue = expected[key]
        if (headerComparison.isEmpty()) {
          reporters.forEach { it.headerComparisonOk(key, expectedHeaderValue!!) }
        } else {
          reporters.forEach { it.headerComparisonFailed(key, expectedHeaderValue!!, headerComparison) }
          failures["$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""] =
            headerComparison.joinToString(", ") { it.description() }
          result = result.merge(TestResult.Failed(headerComparison.map { it.toMap() },
            "Headers had differences"))
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
  ) = verifyResponseFromProvider(provider, interaction, interactionMessage, failures, client, mapOf())

  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: RequestResponseInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: Map<String, Any>
  ): TestResult {
    return try {
      val expectedResponse = interaction.response.generatedResponse(context)
      val actualResponse = client.makeRequest(interaction.request.generatedRequest(context))

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
        interaction.interactionId.orEmpty())
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      TestResult.Failed(listOf(mapOf("message" to "Request to provider failed with an exception",
        "exception" to e, "interactionId" to interaction.interactionId)),
        "Request to provider method failed with an exception")
    }
  }

  override fun verifyProvider(provider: ProviderInfo): MutableMap<String, Any> {
    val failures = mutableMapOf<String, Any>()

    initialiseReporters(provider)

    val consumers = provider.consumers.filter(::filterConsumers)
    if (consumers.isEmpty()) {
      reporters.forEach { it.warnProviderHasNoConsumers(provider) }
    }

    consumers.forEach {
      runVerificationForConsumer(failures, provider, it)
    }

    return failures
  }

  fun initialiseReporters(provider: ProviderInfo) {
    reporters.forEach {
      if (it.hasProperty("displayFullDiff")) {
        (it.property("displayFullDiff") as KMutableProperty1<VerifierReporter, Boolean>)
          .set(it, projectHasProperty.apply(PACT_SHOW_FULLDIFF))
      }
      it.initialise(provider)
    }
  }

  @JvmOverloads
  fun runVerificationForConsumer(
    failures: MutableMap<String, Any>,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    client: PactBrokerClient? = null
  ) {
    val pact = FilteredPact(loadPactFileForConsumer(consumer),
      Predicate { filterInteractions(it) })
    reportVerificationForConsumer(consumer, provider, pact.source)
    if (pact.interactions.isEmpty()) {
      reporters.forEach { it.warnPactFileHasNoInteractions(pact as Pact<Interaction>) }
    } else {
      val result = pact.interactions.map {
        verifyInteraction(provider, consumer, failures, it)
      }.reduce { acc, result -> acc.merge(result) }
      when {
        pact.isFiltered() -> logger.warn {
          "Skipping publishing of verification results as the interactions have been filtered"
        }
        publishingResultsDisabled() -> logger.warn {
          "Skipping publishing of verification results as it has been disabled " +
          "($PACT_VERIFIER_PUBLISH_RESULTS is not 'true')"
        }
        else -> verificationReporter.reportResults(pact, result, providerVersion.get() ?: "0.0.0", client,
          providerTag?.get())
      }
    }
  }

  fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo, pactSource: PactSource?) {
    when (pactSource) {
      is BrokerUrlSource -> reporters.forEach {
        it.reportVerificationForConsumer(consumer, provider, pactSource.tag)
        val notices = consumer.notices.filter { it.`when` == "before_verification" }
        if (notices.isNotEmpty()) {
          it.reportVerificationNoticesForConsumer(consumer, provider, notices)
        }
        it.verifyConsumerFromUrl(pactSource, consumer)
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

    fun invokeProviderMethod(m: Method, instance: Any?): Any? {
      try {
        return m.invoke(instance)
      } catch (e: Throwable) {
        throw RuntimeException("Failed to invoke provider method '${m.name}'", e)
      }
    }
  }
}
