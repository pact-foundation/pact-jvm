package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseListExpression
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ConsumerInfo
import mu.KLogging
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KClass

/**
 * Out-of-the-box implementation of {@link PactLoader} that downloads pacts from Pact broker
 */
open class PactBrokerLoader(
  val pactBrokerHost: String,
  val pactBrokerPort: String?,
  val pactBrokerScheme: String,
  val pactBrokerTags: List<String>? = listOf("latest"),
  val pactBrokerConsumers: List<String> = emptyList(),
  var failIfNoPactsFound: Boolean = true,
  var authentication: PactBrokerAuth?,
  var valueResolverClass: KClass<out ValueResolver>?,
  valueResolver: ValueResolver? = null
) : OverrideablePactLoader {

  private var pacts: MutableMap<Consumer, MutableList<Pact<Interaction>>> = mutableMapOf()
  private var resolver: ValueResolver? = valueResolver
  private var overriddenPactUrl: String? = null
  private var overriddenConsumer: String? = null

  var pactReader: PactReader = DefaultPactReader

  constructor(pactBroker: PactBroker) : this(
    pactBroker.host,
    pactBroker.port,
    pactBroker.scheme,
    pactBroker.tags.toList(),
    pactBroker.consumers.toList(),
    true,
    pactBroker.authentication,
    pactBroker.valueResolver
  )

  override fun overridePactUrl(pactUrl: String, consumer: String) {
    overriddenPactUrl = pactUrl
    overriddenConsumer = consumer
  }

  override fun load(providerName: String): List<Pact<Interaction>> {
    val resolver = setupValueResolver()
    val pacts = when {
      overriddenPactUrl.isNotEmpty() -> {
        val pactBrokerClient = newPactBrokerClient(brokerUrl(resolver).build(), resolver)
        val pactSource = UrlSource<Interaction>(overriddenPactUrl!!)
        pactSource.encodePath = false
        listOf(loadPact(ConsumerInfo(name = overriddenConsumer!!, pactSource = pactSource),
          pactBrokerClient.options))
      }
      pactBrokerTags.isNullOrEmpty() -> loadPactsForProvider(providerName, null, resolver)
      else -> {
        pactBrokerTags.flatMap { parseListExpression(it, resolver) }.flatMap {
          try {
            loadPactsForProvider(providerName, it, resolver)
          } catch (e: NoPactsFoundException) {
            // Ignoring exception at this point, it will be handled at a higher level
            emptyList<Pact<Interaction>>()
          }
        }
      }
    }
    return pacts
  }

  private fun setupValueResolver(): ValueResolver {
    var valueResolver: ValueResolver = SystemPropertyResolver()
    if (resolver != null) {
      valueResolver = resolver!!
    } else if (valueResolverClass != null) {
      try {
        valueResolver = valueResolverClass!!.java.newInstance()
      } catch (e: InstantiationException) {
        logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
      } catch (e: IllegalAccessException) {
        logger.warn(e) { "Failed to instantiate the value resolver, using the default" }
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
  private fun loadPactsForProvider(
    providerName: String,
    tag: String?,
    resolver: ValueResolver
  ): List<Pact<Interaction>> {
    logger.debug { "Loading pacts from pact broker for provider $providerName and tag $tag" }
    val uriBuilder = brokerUrl(resolver)
    try {
      var consumers: List<ConsumerInfo>
      val pactBrokerClient = newPactBrokerClient(uriBuilder.build(), resolver)
      consumers = if (tag.isNullOrEmpty() || tag == "latest") {
        pactBrokerClient.fetchConsumers(providerName).map { ConsumerInfo.from(it) }
      } else {
        pactBrokerClient.fetchConsumersWithTag(providerName, tag).map { ConsumerInfo.from(it) }
      }

      if (failIfNoPactsFound && consumers.isEmpty()) {
        throw NoPactsFoundException("No consumer pacts were found for provider '" + providerName + "' and tag '" +
          tag + "'. (URL " + getUrlForProvider(providerName, tag.orEmpty(), pactBrokerClient) + ")")
      }

      if (pactBrokerConsumers.isNotEmpty()) {
        val consumerInclusions = pactBrokerConsumers.flatMap { parseListExpression(it, resolver) }
        consumers = consumers.filter { consumerInclusions.isEmpty() || consumerInclusions.contains(it.name) }
      }

      return consumers.map { loadPact(it, pactBrokerClient.options) }
    } catch (e: URISyntaxException) {
      throw IOException("Was not able load pacts from broker as the broker URL was invalid", e)
    }
  }

  private fun brokerUrl(resolver: ValueResolver): URIBuilder {
    val (host, port, scheme) = getPactBrokerSource(resolver)

    val uriBuilder = URIBuilder().setScheme(scheme).setHost(host)
    if (port.isNotEmpty()) {
      uriBuilder.port = Integer.parseInt(port)
    }
    return uriBuilder
  }

  private fun getPactBrokerSource(resolver: ValueResolver): PactBrokerSource<Interaction> {
    val scheme = parseExpression(pactBrokerScheme, resolver)
    val host = parseExpression(pactBrokerHost, resolver)
    val port = parseExpression(pactBrokerPort, resolver)

    if (host.isNullOrEmpty()) {
      throw IllegalArgumentException(String.format("Invalid pact broker host specified ('%s'). " +
        "Please provide a valid host or specify the system property 'pactbroker.host'.", pactBrokerHost))
    }

    if (port.isNotEmpty() && !port!!.matches(Regex("^[0-9]+"))) {
      throw IllegalArgumentException(String.format("Invalid pact broker port specified ('%s'). " +
        "Please provide a valid port number or specify the system property 'pactbroker.port'.", pactBrokerPort))
    }

    return if (scheme == null) {
      PactBrokerSource(host, port, pacts = pacts)
    } else {
      PactBrokerSource(host, port, scheme, pacts)
    }
  }

  private fun getUrlForProvider(providerName: String, tag: String, pactBrokerClient: PactBrokerClient): String {
    return try {
      pactBrokerClient.getUrlForProvider(providerName, tag)!!
    } catch (e: Exception) {
      logger.debug(e) { "Failed to get provider URL from the pact broker" }
      "Unknown"
    }
  }

  open fun loadPact(consumer: ConsumerInfo, options: Map<String, Any>): Pact<Interaction> {
    val pact = pactReader.loadPact(consumer.pactSource!!, options) as Pact<Interaction>
    val pactConsumer = consumer.toPactConsumer()
    val pactList = pacts.getOrDefault(pactConsumer, mutableListOf())
    pactList.add(pact)
    pacts[pactConsumer] = pactList
    return pact
  }

  open fun newPactBrokerClient(url: URI, resolver: ValueResolver): PactBrokerClient {
    if (authentication == null || authentication!!.scheme.equals("none", ignoreCase = true)) {
      logger.debug { "Authentication: None" }
      return PactBrokerClient(url.toString(), emptyMap())
    }

    val scheme = parseExpression(authentication!!.scheme, resolver)
    if (scheme.isNotEmpty()) {
      // Legacy behavior (before support for bearer token was added):
      // If scheme was not explicitly set, basic was always used.
      // If it was explicitly set, the given value was used together with username and password
      val schemeToUse = if (scheme.equals("legacy")) "basic" else scheme
      logger.debug { "Authentication: $schemeToUse" }
      val options = mapOf("authentication" to listOf(schemeToUse,
              parseExpression(authentication!!.username, resolver),
              parseExpression(authentication!!.password, resolver)))
      return PactBrokerClient(url.toString(), options)
    }

    // Check if username is set. If yes, use basic auth.
    val username = parseExpression(authentication!!.username, resolver)
    if (username.isNotEmpty()) {
      logger.debug { "Authentication: Basic" }
      val options = mapOf("authentication" to listOf("basic", username,
        parseExpression(authentication!!.password, resolver)))
      return PactBrokerClient(url.toString(), options)
    }

    // Check if token is set. If yes, use bearer auth.
    val token = parseExpression(authentication!!.token, resolver)
    if (token.isNotEmpty()) {
      logger.debug { "Authentication: Bearer" }
      val options = mapOf("authentication" to listOf("bearer", token))
      return PactBrokerClient(url.toString(), options)
    }

    throw IllegalArgumentException("Invalid pact authentication specified. Either username or token must be set.")
  }

  companion object : KLogging()
}
