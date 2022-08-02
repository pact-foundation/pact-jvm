package au.com.dius.pact.provider.junitsupport.loader

import au.com.dius.pact.core.matchers.util.padTo
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.support.Utils.permutations
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseListExpression
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import mu.KLogging
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Out-of-the-box implementation of {@link PactLoader} that downloads pacts from Pact broker
 */
@Suppress("LongParameterList", "TooManyFunctions")
open class PactBrokerLoader(
  @Deprecated("Use pactBrokerUrl")
  val pactBrokerHost: String?,
  @Deprecated("Use pactBrokerUrl")
  val pactBrokerPort: String?,
  @Deprecated("Use pactBrokerUrl")
  val pactBrokerScheme: String?,
  @Deprecated(message = "use consumerVersionSelectors method or pactbroker.consumerversionselectors property")
  val pactBrokerTags: List<String>? = emptyList(),
  @Deprecated(message = "use consumerVersionSelectors method or pactbroker.consumerversionselectors property")
  val pactBrokerConsumerVersionSelectors: List<VersionSelector>,
  val pactBrokerConsumers: List<String> = emptyList(),
  var failIfNoPactsFound: Boolean = true,
  var authentication: PactBrokerAuth?,
  var valueResolverClass: KClass<out ValueResolver>?,
  valueResolver: ValueResolver? = null,
  val enablePendingPacts: String = "false",
  val providerTags: List<String> = emptyList(),
  val providerBranch: String = "",
  val includeWipPactsSince: String = "",
  val pactBrokerUrl: String? = null,
  val enableInsecureTls: String = "false"
) : OverrideablePactLoader {

  private var testClass: Class<*>? = null
  private var testInstance: Any? = null
  private var resolver: ValueResolver? = valueResolver
  private var overriddenPactUrl: String? = null
  private var overriddenConsumer: String? = null

  var pactReader: PactReader = DefaultPactReader

  constructor(pactBroker: PactBroker) : this(
    pactBroker.host,
    pactBroker.port,
    pactBroker.scheme,
    pactBroker.tags.toList(),
    pactBroker.consumerVersionSelectors.toList(),
    pactBroker.consumers.toList(),
    true,
    pactBroker.authentication,
    pactBroker.valueResolver,
    null,
    pactBroker.enablePendingPacts,
    pactBroker.providerTags.toList(),
    pactBroker.providerBranch,
    pactBroker.includeWipPactsSince,
    pactBroker.url,
    pactBroker.enableInsecureTls
  )

  override fun description(): String {
    val resolver = setupValueResolver()
    val consumerVersionSelectors = buildConsumerVersionSelectors(resolver)
    val consumers = pactBrokerConsumers.flatMap { parseListExpression(it, resolver) }.filter { it.isNotEmpty() }
    var source = getPactBrokerSource(resolver).description()
    if (consumerVersionSelectors.isNotEmpty()) {
      source += " consumerVersionSelectors=$consumerVersionSelectors"
    }
    if (consumers.isNotEmpty()) {
      source += " consumers=$consumers"
    }
  return source
  }

  override fun overridePactUrl(pactUrl: String, consumer: String) {
    overriddenPactUrl = pactUrl
    overriddenConsumer = consumer
  }

  @Throws(IOException::class)
  override fun load(providerName: String): List<Pact<*>> {
    val resolver = setupValueResolver()
    return when {
      overriddenPactUrl.isNotEmpty() -> {
        val brokerUri = brokerUrl(resolver).build()
        val pactBrokerClient = newPactBrokerClient(brokerUri, resolver)
        val pactSource = BrokerUrlSource(overriddenPactUrl!!, brokerUri.toString(), options = pactBrokerClient.options)
        pactSource.encodePath = false
        listOf(pactReader.loadPact(pactSource, pactBrokerClient.options))
      }
      else -> {
        try {
          val consumerVersionSelectors = buildConsumerVersionSelectors(resolver)
          loadPactsForProvider(providerName, consumerVersionSelectors, resolver)
        } catch (e: NoPactsFoundException) {
          // Ignoring exception at this point, it will be handled at a higher level
          emptyList<Pact<Interaction>>()
        }
      }
    }
  }

  fun buildConsumerVersionSelectors(resolver: ValueResolver): List<ConsumerVersionSelectors> {
    val tags = pactBrokerTags.orEmpty().flatMap { parseListExpression(it, resolver) }
    val selectorsMethod = testClassHasSelectorsMethod(this.testClass)
    return if (selectorsMethod != null) {
      invokeSelectorsMethod(this.testInstance, selectorsMethod)
    } else if (shouldFallBackToTags(tags, pactBrokerConsumerVersionSelectors, resolver)) {
      permutations(tags, pactBrokerConsumers.flatMap { parseListExpression(it, resolver) })
        .map { ConsumerVersionSelectors.Selector(it.first, true, it.second) }
    } else {
      pactBrokerConsumerVersionSelectors.flatMap {
        val tags = parseListExpression(it.tag, resolver)
        val consumers = parseListExpression(it.consumer, resolver)
        val fallbackTag = parseExpression(it.fallbackTag, DataType.STRING, resolver) as String?
        val parsedLatest = parseListExpression(it.latest, resolver)
        val latest = when {
          parsedLatest.isEmpty() -> List(tags.size) { true.toString() }
          parsedLatest.size == 1 -> parsedLatest.padTo(tags.size, parsedLatest[0])
          else -> parsedLatest
        }

        if (tags.size != latest.size) {
          throw IllegalArgumentException("Invalid Consumer version selectors. Each version selector must have a tag " +
                  "and latest property")
        }

        when {
          tags.isNotEmpty() && consumers.isNotEmpty() -> {
            permutations(tags.mapIndexed { index, tag -> tag to index  }, consumers).map { (tag, consumer) ->
              ConsumerVersionSelectors.Selector(tag!!.first, latest[tag.second].toBoolean(), consumer, fallbackTag)
            }
          }
          tags.isNotEmpty() -> {
            tags.mapIndexed { index, tag ->
              ConsumerVersionSelectors.Selector(tag, latest[index].toBoolean(), consumers.firstOrNull(), fallbackTag)
            }
          }
          consumers.isNotEmpty() -> {
            consumers.map { name ->
              ConsumerVersionSelectors.Selector(null, true, name, fallbackTag)
            }
          }
          else -> listOf()
        }
      }
    }
  }

  fun shouldFallBackToTags(tags: List<String>, selectors: List<VersionSelector>, resolver: ValueResolver): Boolean {
    return selectors.isEmpty() || (selectors.size == 1 && parseListExpression(selectors[0].tag, resolver).isEmpty() &&
      tags.isNotEmpty())
  }

  private fun setupValueResolver(): ValueResolver {
    var valueResolver: ValueResolver = SystemPropertyResolver
    if (resolver != null) {
      valueResolver = resolver!!
    } else if (valueResolverClass != null) {
      if (valueResolverClass!!.objectInstance != null) {
        valueResolver = valueResolverClass!!.objectInstance!!
      } else {
        try {
          valueResolver = valueResolverClass!!.java.newInstance()
        } catch (e: InstantiationException) {
          logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
        } catch (e: IllegalAccessException) {
          logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
        }
      }
    }
    return valueResolver
  }

  override fun getPactSource(): PactSource? {
    val resolver = setupValueResolver()
    return getPactBrokerSource(resolver)
  }

  override fun setValueResolver(valueResolver: ValueResolver) {
    this.resolver = valueResolver
  }

  @Throws(IOException::class, IllegalArgumentException::class)
  @Suppress("ThrowsCount")
  private fun loadPactsForProvider(
    providerName: String,
    selectors: List<ConsumerVersionSelectors>,
    resolver: ValueResolver
  ): List<Pact<*>> {
    logger.debug { "Loading pacts from pact broker for provider $providerName and consumer version selectors " +
      "$selectors" }
    val pending = parseExpression(enablePendingPacts, DataType.BOOLEAN, resolver) as Boolean
    val providerTags = providerTags.flatMap { parseListExpression(it, resolver) }.filter { it.isNotEmpty() }
    val providerBranch = parseExpression(providerBranch, DataType.STRING, resolver) as String?

    if (pending && providerTags.none { it.isNotEmpty() } && providerBranch.isNullOrBlank()) {
      throw IllegalArgumentException("Pending pacts feature has been enabled, but no provider tags or branch have" +
        " been specified. To use the pending pacts feature, you need to provide the list of provider names for the" +
        " provider application version with the providerTags or providerBranch property that will be published with" +
        " the verification results.")
    }
    val wipSinceDate = if (pending) parseExpression(includeWipPactsSince, DataType.STRING, resolver) as String else ""

    val uriBuilder = brokerUrl(resolver)
    try {
      val pactBrokerClient = newPactBrokerClient(uriBuilder.build(), resolver)

      val result = pactBrokerClient.fetchConsumersWithSelectorsV2(providerName, selectors, providerTags,
              providerBranch, pending, wipSinceDate)
      var consumers = when (result) {
        is Ok -> result.value
        is Err -> throw result.error
      }

      if (failIfNoPactsFound && consumers.isEmpty()) {
        throw NoPactsFoundException("No consumer pacts were found for provider '" + providerName + "' and consumer " +
          "version selectors '" + selectors + "'. (URL " + getUrlForProvider(providerName, pactBrokerClient) + ")")
      }

      if (pactBrokerConsumers.isNotEmpty()) {
        val consumerInclusions = pactBrokerConsumers.flatMap { parseListExpression(it, resolver) }
        consumers = consumers.filter { it.usedNewEndpoint || consumerInclusions.isEmpty() ||
          consumerInclusions.contains(it.name) }
      }

      return consumers.map { pactReader.loadPact(it, pactBrokerClient.options) }
    } catch (e: URISyntaxException) {
      throw IOException("Was not able load pacts from broker as the broker URL was invalid", e)
    }
  }

  fun brokerUrl(resolver: ValueResolver): URIBuilder {
    val (host, port, scheme, _, url) = getPactBrokerSource(resolver)

    return if (url.isNullOrEmpty()) {
      val uriBuilder = URIBuilder().setScheme(scheme).setHost(host)
      if (port.isNotEmpty()) {
        uriBuilder.port = Integer.parseInt(port)
      }
      uriBuilder
    } else {
      URIBuilder(url)
    }
  }

  fun getPactBrokerSource(resolver: ValueResolver): PactBrokerSource<Interaction> {
    val scheme = parseExpression(pactBrokerScheme, DataType.RAW, resolver)?.toString()
    val host = parseExpression(pactBrokerHost, DataType.RAW, resolver)?.toString()
    val port = parseExpression(pactBrokerPort, DataType.RAW, resolver)?.toString()
    val url = parseExpression(pactBrokerUrl, DataType.RAW, resolver)?.toString()

    return if (url.isNullOrEmpty()) {
      if (host.isNullOrEmpty() || !host.matches(Regex("[0-9a-zA-Z\\-.]+"))) {
        throw IllegalArgumentException(String.format("Invalid pact broker host specified ('%s'). " +
          "Please provide a valid host or specify the system property 'pactbroker.host'.", pactBrokerHost))
      }

      if (port.isNotEmpty() && !port!!.matches(Regex("^[0-9]+"))) {
        throw IllegalArgumentException(String.format("Invalid pact broker port specified ('%s'). " +
          "Please provide a valid port number or specify the system property 'pactbroker.port'.", pactBrokerPort))
      }

      if (scheme == null) {
        PactBrokerSource(host, port)
      } else {
        PactBrokerSource(host, port, scheme)
      }
    } else {
      PactBrokerSource(null, null, url = url)
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun getUrlForProvider(providerName: String, pactBrokerClient: IPactBrokerClient): String {
    return try {
      pactBrokerClient.getUrlForProvider(providerName, "") ?: "Unknown"
    } catch (e: Exception) {
      logger.debug(e) { "Failed to get provider URL from the pact broker" }
      "Unknown"
    }
  }

  open fun newPactBrokerClient(url: URI, resolver: ValueResolver): IPactBrokerClient {
    var options = mapOf<String, Any>()
    val insecureTls = parseExpression(enableInsecureTls, DataType.BOOLEAN, resolver) as Boolean
    val config = PactBrokerClientConfig(insecureTLS = insecureTls)

    if (authentication == null) {
      logger.debug { "Authentication: None" }
    } else {
      val username = parseExpression(authentication!!.username, DataType.RAW, resolver)?.toString()
      val token = parseExpression(authentication!!.token, DataType.RAW, resolver)?.toString()

      // Check if username is set. If yes, use basic auth.
      if (username.isNotEmpty()) {
        logger.debug { "Authentication: Basic" }
        options = mapOf(
          "authentication" to listOf(
            "basic", username,
            parseExpression(authentication!!.password, DataType.RAW, resolver)
          )
        )
      // Check if token is set. If yes, use bearer auth.
      } else if (token.isNotEmpty()) {
        logger.debug { "Authentication: Bearer" }
        options = mapOf("authentication" to listOf("bearer", token))
      }
    }

    return PactBrokerClient(url.toString(), options.toMutableMap(), config)
  }

  override fun initLoader(testClass: Class<*>?, testInstance: Any?) {
    this.testClass = testClass
    this.testInstance = testInstance
  }

  companion object : KLogging() {
    @JvmStatic
    fun invokeSelectorsMethod(testInstance: Any?, selectorsMethod: KCallable<*>): List<ConsumerVersionSelectors> {
      val projectedType = SelectorBuilder::class.starProjectedType
      return when (selectorsMethod.parameters.size) {
        0 -> if (selectorsMethod.returnType.isSubtypeOf(projectedType)) {
          val builder = selectorsMethod.call() as SelectorBuilder
          builder.build()
        } else {
          selectorsMethod.call() as List<ConsumerVersionSelectors>
        }
        1 -> if (selectorsMethod.returnType.isSubtypeOf(projectedType)) {
          val builder = selectorsMethod.call(testInstance) as SelectorBuilder
          builder.build()
        } else {
          selectorsMethod.call(testInstance) as List<ConsumerVersionSelectors>
        }
        else -> throw java.lang.IllegalArgumentException(
          "Consumer version selector method should not take any parameters and return an instance of SelectorBuilder")
      }
    }

    @JvmStatic
    fun testClassHasSelectorsMethod(testClass: Class<*>?): KCallable<*>? {
      val projectedType = SelectorBuilder::class.starProjectedType
      return try {
        testClass?.kotlin?.members?.firstOrNull { method ->
          method.findAnnotation<PactBrokerConsumerVersionSelectors>() != null && (
            // static method
            method.parameters.isEmpty()
              // instance method
              || (method.parameters.size == 1 && method.parameters[0].kind == KParameter.Kind.INSTANCE)
            && method.visibility == KVisibility.PUBLIC
            && (
              method.returnType.isSubtypeOf(projectedType)
              || method.returnType.isSubtypeOf(List::class.starProjectedType)
            )
          )
        }
      } catch (e: NullPointerException) {
        null
      }
    }
  }
}
