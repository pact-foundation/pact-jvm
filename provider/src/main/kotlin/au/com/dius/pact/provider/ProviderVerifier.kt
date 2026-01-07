package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.matchers.generators.DefaultResponseGenerator
import au.com.dius.pact.core.matchers.interactionCatalogueEntries
import au.com.dius.pact.core.matchers.matcherCatalogueEntries
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
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessageInteraction
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.Result.Err
import au.com.dius.pact.core.support.Result.Ok
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.hasProperty
import au.com.dius.pact.core.support.ifNullOrEmpty
import au.com.dius.pact.core.support.property
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.Event
import au.com.dius.pact.provider.reporters.VerifierReporter
import groovy.lang.Closure
import io.github.classgraph.ClassGraph
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.InteractionVerificationDetails
import io.pact.plugins.jvm.core.PluginConfiguration
import io.pact.plugins.jvm.core.PluginManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KMutableProperty1

private val logger = KotlinLogging.logger {}

/**
 * Type of verification being preformed
 */
enum class PactVerification {
  /**
   * Standard HTTP request/response
   */
  REQUEST_RESPONSE,

  /**
   * Annotated method that will return the response (for message interactions)
   */
  ANNOTATED_METHOD,

  /**
   * Factory facade used to get the response
   */
  RESPONSE_FACTORY,

  /**
   * Verification is provided by a plugin
   */
  PLUGIN
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
  fun verifyProvider(provider: IProviderInfo): List<VerificationResult>

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
  @Suppress("LongParameterList")
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
  @Suppress("LongParameterList")
  @Deprecated("Use the version that passes in any plugin configuration")
  fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult

  /**
   * Verifies the interaction by invoking a method on a provider test class
   */
  @Suppress("LongParameterList")
  fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult

  @Deprecated("Use the version that passes in any plugin configuration")
  @Suppress("LongParameterList")
  fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult

  @Suppress("LongParameterList")
  fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult

  /**
   * Compares the expected and actual responses
   */
  @Suppress("LongParameterList")
  fun verifyRequestResponsePact(
    expectedResponse: IResponse,
    actualResponse: ProviderResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult

  /**
   * Compares the expected and actual responses
   */
  @Suppress("LongParameterList")
  @Deprecated("Use the version that passes in any plugin configuration")
  fun verifyRequestResponsePact(
    expectedResponse: IResponse,
    actualResponse: ProviderResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    interactionId: String,
    pending: Boolean
  ): VerificationResult = verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
    interactionId, pending, emptyMap())

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
   * Verification executed by a plugin
   */
  fun verifyInteractionViaPlugin(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    pact: V4Pact,
    interaction: V4Interaction,
    client: Any?,
    request: Any?,
    context: Map<String, Any>
  ): VerificationResult

  /**
   * Display any output to the user
   */
  fun displayOutput(output: List<String>)

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
  @Deprecated("Use version that returns multiple tags", replaceWith = ReplaceWith("providerTags"))
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
  override var responseFactory: Function<String, Any>? = null // TODO: This does not support sync message needs
) : IProviderVerifier {

  override var projectHasProperty = Function<String, Boolean> { name -> !System.getProperty(name).isNullOrEmpty() }
  override var projectGetProperty = Function<String, String?> { name -> System.getProperty(name) }
  var verificationReporter: VerificationReporter = DefaultVerificationReporter
  var stateChangeHandler: StateChange = DefaultStateChange
  var pactReader: PactReader = DefaultPactReader
  override var verificationSource: String? = null
  var pluginManager: PluginManager = DefaultPluginManager
  var responseComparer: IResponseComparison = ResponseComparison.Companion

  private var currentInteraction: Interaction? = null

  /**
   * This will return true unless the pact.verifier.publishResults property has the value of "true"
   */
  override fun publishingResultsDisabled(): Boolean {
    return when {
      !projectHasProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS) ->
        verificationReporter.publishingResultsDisabled(SystemPropertyResolver)
      else -> projectGetProperty.apply(PACT_VERIFIER_PUBLISH_RESULTS)?.lowercase() != "true"
    }
  }

  @Deprecated("Use the version that passes in any plugin configuration")
  @Suppress("LongParameterList")
  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ) = verifyResponseByInvokingProviderMethods(providerInfo, consumer, interaction, interactionMessage, failures,
    pending, emptyMap())

  @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "SpreadOperator", "LongParameterList")
  override fun verifyResponseByInvokingProviderMethods(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = interaction
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
        if (interaction.isSynchronousMessages()) {
          throw RuntimeException(
            "No annotated methods were found for interaction " +
              "'${interaction.description}'. You need to provide a method annotated with " +
              "@PactVerifyProvider(\"${interaction.description}\") on the classpath that receives the request message" +
              " and returns the response message contents."
          )
        } else {
          throw RuntimeException(
            "No annotated methods were found for interaction " +
              "'${interaction.description}'. You need to provide a method annotated with " +
              "@PactVerifyProvider(\"${interaction.description}\") on the classpath that returns the message contents."
          )
        }
      } else {
        return if (interaction.isAsynchronousMessage()) {
          verifyMessage(
            methodsAnnotatedWith.toHashSet(), interaction as MessageInteraction, interactionMessage,
            failures, pending, pluginConfiguration
          )
        } else if (interaction.isSynchronousMessages()) {
          verifySynchronousMessage(
            methodsAnnotatedWith.toHashSet(),
            interaction as V4Interaction.SynchronousMessages,
            interactionId,
            interactionMessage,
            failures,
            pending,
            pluginConfiguration
          )
        } else {
          val synchronousRequestResponse = interaction as SynchronousRequestResponse
          val expectedResponse = synchronousRequestResponse.response
          var result: VerificationResult = VerificationResult.Ok(interactionId, emptyList())
          methodsAnnotatedWith.forEach {
            val response = invokeProviderMethod(synchronousRequestResponse.description, synchronousRequestResponse,
              it, null) as Map<String, Any>
            val body = OptionalBody.body(response["data"] as String?)
            val actualResponse = ProviderResponse(response["statusCode"] as Int,
              response["headers"] as Map<String, List<String>>, ContentType.UNKNOWN, body
            )
            result = result.merge(this.verifyRequestResponsePact(
              expectedResponse,
              actualResponse,
              interactionMessage,
              failures,
              interactionId.orEmpty(),
              pending,
              pluginConfiguration
            ))
          }
          result
        }
      }
    } catch (e: Exception) {
      failures[interactionMessage] = e
      emitEvent(Event.VerificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)))
      val errors = listOf(
        VerificationFailureType.ExceptionFailure("Request to provider method failed with an exception",
          e, interaction)
      )
      return VerificationResult.Failed(
        "Request to provider method failed with an exception", interactionMessage,
        mapOf(interactionId.orEmpty() to errors), pending)
    }
  }

  private fun verifySynchronousMessage(
    methods: HashSet<Method>,
    interaction: V4Interaction.SynchronousMessages,
    interactionId: String?,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = interaction
    var result: VerificationResult = VerificationResult.Ok(interactionId, emptyList())
    methods.forEach { method ->
      val messageFactory = BiFunction<String, Any, Any> {
        desc, req -> invokeProviderMethod(desc, interaction, method, providerMethodInstance.apply(method)/*, req*/)!!
      }
      result = result.merge(verifySynchronousMessage(
        messageFactory,
        interaction,
        interactionId,
        interactionMessage,
        failures,
        pending,
        pluginConfiguration
      ))
    }
    return result
  }

  private fun verifySynchronousMessage(
    factory: BiFunction<String, Any, Any>,
    interaction: V4Interaction.SynchronousMessages,
    interactionId: String?,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = interaction
    emitEvent(Event.GeneratesAMessageWhich)
    val messageResult = factory.apply(interaction.description, interaction.request)
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
    val comparison = responseComparer.compareSynchronousMessage(interaction,
      OptionalBody.body(actualMessage, contentType), messageMetadata, pluginConfiguration)
    val s = ": generates a message which"
    return displayBodyResult(
      failures,
      comparison.bodyMismatches,
      interactionMessage + s,
      interactionId.orEmpty(),
      pending,
 currentInteraction
    ).merge(
      displayMetadataResult(
        messageMetadata ?: emptyMap(),
        failures,
        comparison.metadataMismatches,
        interactionMessage + s,
        interactionId.orEmpty(),
        pending,
 currentInteraction
      )
    )
  }

  @Deprecated("Use the version that passes in any plugin configuration")
  override fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ) = verifyResponseByFactory(providerInfo, consumer, interaction, interactionMessage, failures, pending, emptyMap())

  @Suppress("TooGenericExceptionCaught")
  override fun verifyResponseByFactory(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = interaction
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
          pending,
          pluginConfiguration
        )
      } else if (interaction.isSynchronousMessages()) {
        verifySynchronousMessage(
          { s, req -> factory.apply(s) }, // TODO: This does not support sync message needs
          interaction as V4Interaction.SynchronousMessages,
          interactionId,
          interactionMessage,
          failures,
          pending,
          pluginConfiguration
        )
      } else {
        val expectedResponse = (interaction as SynchronousRequestResponse).response
        val response = factory.apply(interaction.description) as Map<String, Any>
        val contentType = response["contentType"] as String?
        val ct = if (contentType == null) ContentType.UNKNOWN else ContentType(contentType)
        val actualResponse = ProviderResponse(
          response["statusCode"] as Int,
          response["headers"] as Map<String, List<String>>,
          ct,
          OptionalBody.body(response["data"] as String?, ct)
        )
        this.verifyRequestResponsePact(
          expectedResponse,
          actualResponse,
          interactionMessage,
          failures,
          interactionId,
          pending,
          pluginConfiguration
        )
      }
    } catch (e: Exception) {
      logger.error(e) { "Verification factory method failed with an exception" }
      failures[interactionMessage] = e
      emitEvent(Event.VerificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)))
      val errors = listOf(
        VerificationFailureType.ExceptionFailure("Verification factory method failed with an exception",
          e, interaction)
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
      @Suppress("SpreadOperator")
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
    pending: Boolean,
    interaction: Interaction?
  ): VerificationResult {
    return if (comparison is Ok && comparison.value.mismatches.isEmpty()) {
      emitEvent(Event.BodyComparisonOk)
      VerificationResult.Ok(interactionId, emptyList())
    } else {
      emitEvent(Event.BodyComparisonFailed(comparison))
      val description = "$comparisonDescription has a matching body"
      when (comparison) {
        is Err -> {
          failures[description] = comparison.error.description()
          VerificationResult.Failed("Body had differences", description,
            mapOf(interactionId to listOf(VerificationFailureType.MismatchFailure(comparison.error, interaction))),
            pending)
        }
        is Ok -> {
          failures[description] = comparison.value
          VerificationResult.Failed("Body had differences", description,
            mapOf(interactionId to comparison.value.mismatches.values.flatten()
              .map { VerificationFailureType.MismatchFailure(it, interaction) }), pending)
        }
      }
    }
  }

  @Deprecated("Use version that takes the Plugin Config as a parameter",
    ReplaceWith("verifyMessage(methods, message, interactionMessage, failures, pending, pluginConfiguration)")
  )
  fun verifyMessage(
    methods: Set<Method>,
    message: MessageInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ) = verifyMessage(methods, message, interactionMessage, failures, pending, emptyMap())

  fun verifyMessage(
    methods: Set<Method>,
    message: MessageInteraction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = message
    val interactionId = message.interactionId
    var result: VerificationResult = VerificationResult.Ok(interactionId, emptyList())
    methods.forEach { method ->
      val messageFactory: Function<String, Any> =
        Function { invokeProviderMethod(message.description, message, method, providerMethodInstance.apply(method))!! }
      result = result.merge(verifyMessage(
        messageFactory,
        message,
        interactionId.orEmpty(),
        interactionMessage,
        failures,
        pending,
        pluginConfiguration
      ))
    }
    return result
  }

  @Deprecated("Use version that takes the Plugin Config as a parameter",
    ReplaceWith(
      "verifyMessage(messageFactory, message, interactionId, interactionMessage, failures, pending, pluginConfig)"
    )
  )
  fun verifyMessage(
    messageFactory: Function<String, Any>,
    message: MessageInteraction,
    interactionId: String,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean
  ) = verifyMessage(messageFactory, message, interactionId, interactionMessage, failures, pending, emptyMap())

  fun verifyMessage(
    messageFactory: Function<String, Any>,
    message: MessageInteraction,
    interactionId: String,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    currentInteraction = message
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
    val comparison = responseComparer.compareMessage(message, OptionalBody.body(actualMessage, contentType),
      messageMetadata, pluginConfiguration)
    val s = ": generates a message which"
    return displayBodyResult(
      failures,
      comparison.bodyMismatches,
      interactionMessage + s,
      interactionId,
      pending,
      currentInteraction
    ).merge(
      displayMetadataResult(
        messageMetadata ?: emptyMap(),
        failures,
        comparison.metadataMismatches,
        interactionMessage + s,
        interactionId,
        pending,
        currentInteraction
      )
    )
  }

  private fun displayMetadataResult(
    expectedMetadata: Map<String, Any>,
    failures: MutableMap<String, Any>,
    comparison: Map<String, List<MetadataMismatch>>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean,
    interaction: Interaction?
  ): VerificationResult {
    return if (comparison.isEmpty()) {
      emitEvent(Event.MetadataComparisonOk())
      VerificationResult.Ok(interactionId, emptyList())
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
            mapOf(interactionId to metadataComparison.map { VerificationFailureType.MismatchFailure(it, interaction) }),
            pending))
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
  @Deprecated("Use the version of verifyInteraction that passes in the full Pact and transport entry",
    ReplaceWith("verifyInteraction(provider, consumer, failures, interaction, pact, transportEntry, providerClient)")
  )
  fun verifyInteraction(
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    failures: MutableMap<String, Any>,
    interaction: Interaction,
    providerClient: ProviderClient = ProviderClient(provider, HttpClientFactory())
  ): VerificationResult = verifyInteraction(provider, consumer, failures, interaction, null, null, providerClient)

  @JvmOverloads
  @SuppressWarnings("TooGenericExceptionThrown")
  fun verifyInteraction(
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    failures: MutableMap<String, Any>,
    interaction: Interaction,
    pact: Pact?,
    transportEntry: CatalogueEntry?,
    providerClient: ProviderClient = ProviderClient(provider, HttpClientFactory())
  ): VerificationResult {
    currentInteraction = interaction
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
          verifyResponseByFactory(provider, consumer, interaction, interactionMessage, failures, pending,
            ProviderUtils.pluginConfigForInteraction(pact, interaction)
          )
        }
        PactVerification.PLUGIN -> {
          logger.debug { "Verifying via plugin" }
          if (pact != null && pact.isV4Pact() && transportEntry != null) {
            val v4pact = pact.asV4Pact().unwrap()
            val v4Interaction = interaction.asV4Interaction()
            val config = mutableMapOf(
              "host" to provider.host.toString(),
              "port" to provider.port
            )

            for ((k, v) in stateChangeResult.stateChangeResult.value) {
              config[k] = v
            }

            val request = when (val result = DefaultPluginManager.prepareValidationForInteraction(transportEntry,
              v4pact, v4Interaction, config)) {
              is Ok -> result.value
              is Err -> throw RuntimeException("Failed to configure the interaction for verification - ${result.error}")
            }
            verifyInteractionViaPlugin(provider, consumer, v4pact, v4Interaction, providerClient, request, context)
          } else {
            throw RuntimeException("INTERNAL ERROR: Verification via a plugin requires the version of " +
              "verifyInteraction to be called with the full V4 Pact model")
          }
        }
        else -> {
          logger.debug { "Verifying via provider methods" }
          verifyResponseByInvokingProviderMethods(
            provider, consumer, interaction, interactionMessage, failures, pending,
            ProviderUtils.pluginConfigForInteraction(pact, interaction)
          )
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
          listOf(VerificationFailureType.StateChangeFailure("Provider state change callback failed",
            stateChangeResult, interaction))
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
    pending: Boolean,
    pluginConfiguration: Map<String, PluginConfiguration>
  ): VerificationResult {
    val comparison = responseComparer.compareResponse(expectedResponse, actualResponse, pluginConfiguration)

    reporters.forEach { it.returnsAResponseWhich() }

    return displayStatusResult(failures, expectedResponse.status, comparison.statusMismatch,
        interactionMessage, interactionId, pending, currentInteraction)
      .merge(displayHeadersResult(failures, expectedResponse.headers, comparison.headerMismatches,
        interactionMessage, interactionId, pending, currentInteraction))
      .merge(displayBodyResult(failures, comparison.bodyMismatches,
        interactionMessage, interactionId, pending, currentInteraction))
  }

  fun displayStatusResult(
    failures: MutableMap<String, Any>,
    status: Int,
    mismatch: StatusMismatch?,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean,
    interaction: Interaction?
  ): VerificationResult {
    return if (mismatch == null) {
      reporters.forEach { it.statusComparisonOk(status) }
      VerificationResult.Ok(interactionId, emptyList())
    } else {
      reporters.forEach { it.statusComparisonFailed(status, mismatch.description()) }
      val description = "$comparisonDescription: has status code $status"
      failures[description] = mismatch.description()
      VerificationResult.Failed("Response status did not match", description,
        mapOf(interactionId to listOf(VerificationFailureType.MismatchFailure(mismatch, interaction))), pending)
    }
  }

  fun displayHeadersResult(
    failures: MutableMap<String, Any>,
    expected: Map<String, List<String>>,
    headers: Map<String, List<HeaderMismatch>>,
    comparisonDescription: String,
    interactionId: String,
    pending: Boolean,
    interaction: Interaction?
  ): VerificationResult {
    val ok = VerificationResult.Ok(interactionId, emptyList())
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
              VerificationFailureType.MismatchFailure(it, interaction)
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

  @Suppress("TooGenericExceptionCaught", "LongParameterList")
  override fun verifyResponseFromProvider(
    provider: IProviderInfo,
    interaction: SynchronousRequestResponse,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    client: ProviderClient,
    context: MutableMap<String, Any>,
    pending: Boolean
  ): VerificationResult {
    currentInteraction = interaction
    return try {
      // TODO: Pass any plugin config in here
      val expectedResponse = DefaultResponseGenerator.generateResponse(interaction.response, context,
        GeneratorTestMode.Provider, emptyList(), emptyMap())
      val actualResponse = client.makeRequest(interaction.request.generatedRequest(context, GeneratorTestMode.Provider))

      verifyRequestResponsePact(
        expectedResponse,
        actualResponse,
        interactionMessage,
        failures,
        interaction.interactionId.orEmpty(),
        pending,
        emptyMap() // TODO: Pass any plugin config in here
      )
    } catch (e: Exception) {
      failures[interactionMessage] = e
      reporters.forEach {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      VerificationResult.Failed("Request to provider endpoint failed with an exception", interactionMessage,
        mapOf(interaction.interactionId.orEmpty() to
          listOf(VerificationFailureType.ExceptionFailure("Request to provider endpoint failed with an exception",
            e, interaction))),
        pending)
    }
  }

  override fun verifyProvider(provider: IProviderInfo): List<VerificationResult> {
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
    initialisePlugins(pact)

    return if (pact.interactions.isEmpty()) {
      reporters.forEach { it.warnPactFileHasNoInteractions(pact as Pact) }
      VerificationResult.Ok()
    } else {
      val result = pact.interactions.map {
        verifyInteraction(provider, consumer, failures, it, pact, provider.transportEntry)
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

  /**
   * Initialise any required plugins and plugin entries required for the verification
   */
  @SuppressWarnings("TooGenericExceptionThrown")
  fun initialisePlugins(pact: Pact) {
    CatalogueManager.registerCoreEntries(
      MatchingConfig.contentMatcherCatalogueEntries() +
      matcherCatalogueEntries() +
      interactionCatalogueEntries() +
      MatchingConfig.contentHandlerCatalogueEntries()
    )
    val v4pact = pact.asV4Pact().get()
    if (v4pact != null && v4pact.requiresPlugins()) {
      logger.info { "Pact file requires plugins, will load those now" }
      for (pluginDetails in v4pact.pluginData()) {
        val result = pluginManager.loadPlugin(pluginDetails.name, pluginDetails.version)
        if (result is Err) {
          throw RuntimeException(
            "Failed to load plugin ${pluginDetails.name}/${pluginDetails.version} - ${result.error}"
          )
        }
      }
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

  override fun verifyInteractionViaPlugin(
    providerInfo: IProviderInfo,
    consumer: IConsumerInfo,
    pact: V4Pact,
    interaction: V4Interaction,
    client: Any?,
    request: Any?,
    context: Map<String, Any>
  ): VerificationResult {
    currentInteraction = interaction
    val userConfig = context["userConfig"] as Map<String, Any?>? ?: emptyMap()
    logger.debug { "Verifying interaction => $request" }
    return when (val result = DefaultPluginManager.verifyInteraction(
      client as CatalogueEntry,
      (request as RequestDataToBeVerified).asInteractionVerificationData(),
      userConfig,
      pact,
      interaction
    )) {
      is Result.Ok -> if (result.value.ok) {
        VerificationResult.Ok(interaction.interactionId, result.value.output)
      } else {
        VerificationResult.Failed("Verification via plugin failed", "Verification Failed",
          mapOf(interaction.interactionId.orEmpty() to
            result.value.details.map {
              when (it) {
                is InteractionVerificationDetails.Error -> VerificationFailureType.InvalidInteractionFailure(it.message)
                is InteractionVerificationDetails.Mismatch -> VerificationFailureType.MismatchFailure(
                  BodyMismatch(it.expected, it.actual, it.mismatch, it.path), interaction
                )
              }
            }), output = result.value.output
        )
      }
      is Result.Err -> VerificationResult.Failed("Verification via plugin failed",
        "Verification Failed - ${result.error}")
    }
  }

  override fun displayOutput(output: List<String>) {
    emitEvent(Event.DisplayUserOutput(output))
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
      if (consumer.auth != null && consumer.auth !is Auth.None) {
        options["authentication"] = consumer.auth!!
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

  companion object {
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

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "UnusedPrivateMember", "ThrowsCount")
    fun invokeProviderMethod(_desc: String, interaction: Interaction, m: Method, instance: Any?): Any? {
      // TODO: do we need to support passing in the description?
      try {
        m.isAccessible = true
        return if (m.parameters.size == 1) {
          when (m.parameters[0].type) {
            V4Interaction.AsynchronousMessage::class.java -> m.invoke(instance, interaction.asAsynchronousMessage())
            V4Interaction.SynchronousMessages::class.java -> m.invoke(instance, interaction.asSynchronousMessages())
            MessageContents::class.java -> if (interaction.isAsynchronousMessage()) {
              val contents = interaction.asAsynchronousMessage()!!.contents
              m.invoke(instance, contents)
            } else if (interaction.isSynchronousMessages()) {
              val contents = interaction.asSynchronousMessages()!!.request
              m.invoke(instance, contents)
            } else throw RuntimeException("Failed to invoke provider method '${m.name}': " +
              "Only V4 message interactions support MessageContents")

            else -> throw RuntimeException("Failed to invoke provider method '${m.name}': " +
              "Parameters of type ${m.parameters[0].type} are not supported")
          }
        } else {
          m.invoke(instance)
        }
      } catch (e: Throwable) {
        logger.warn(e) { "Failed to invoke provider method '${m.name}'" }
        throw RuntimeException("Failed to invoke provider method '${m.name}'", e)
      }
    }
  }
}
