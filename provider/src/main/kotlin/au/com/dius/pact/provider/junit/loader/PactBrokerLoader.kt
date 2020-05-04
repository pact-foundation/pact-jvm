package au.com.dius.pact.provider.junit.loader

import arrow.core.Either
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.expressions.DataType
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
@Suppress("LongParameterList", "TooManyFunctions")
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

  override fun description(): String {
    val resolver = setupValueResolver()
    val tags = pactBrokerTags?.flatMap { parseListExpression(it, resolver) }?.filter { it.isNotEmpty() }
    val consumers = pactBrokerConsumers.flatMap { parseListExpression(it, resolver) }.filter { it.isNotEmpty() }
    var source = getPactBrokerSource(resolver).description()
    if (tags != null && tags.isNotEmpty()) {
      source += " tags=$tags"
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

  override fun load(providerName: String): List<Pact<Interaction>> {
    val resolver = setupValueResolver()
    val pacts = when {
      overriddenPactUrl.isNotEmpty() -> {
        val brokerUri = brokerUrl(resolver).build()
        val pactBrokerClient = newPactBrokerClient(brokerUri, resolver)
        val pactSource = BrokerUrlSource(overriddenPactUrl!!, brokerUri.toString())
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
      var consumers: List<ConsumerInfo> = emptyList()
      val pactBrokerClient = newPactBrokerClient(uriBuilder.build(), resolver)
      val result = if (tag.isNullOrEmpty() || tag == "latest") {
        pactBrokerClient.fetchConsumersWithSelectors(providerName)
      } else {
        pactBrokerClient.fetchConsumersWithSelectors(providerName, listOf(ConsumerVersionSelector(tag)))
      }
      when (result) {
        is Either.Right -> consumers = result.b.map { ConsumerInfo.from(it) }
        is Either.Left -> throw result.a
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
    val scheme = parseExpression(pactBrokerScheme, DataType.RAW, resolver)?.toString()
    val host = parseExpression(pactBrokerHost, DataType.RAW, resolver)?.toString()
    val port = parseExpression(pactBrokerPort, DataType.RAW, resolver)?.toString()

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

  @Suppress("TooGenericExceptionCaught")
  private fun getUrlForProvider(providerName: String, tag: String, pactBrokerClient: PactBrokerClient): String {
    return try {
      pactBrokerClient.getUrlForProvider(providerName, tag) ?: "Unknown"
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
    if (authentication == null) {
      logger.debug { "Authentication: None" }
      return PactBrokerClient(url.toString(), emptyMap())
    }

    val username = parseExpression(authentication!!.username, DataType.RAW, resolver)?.toString()
    val token = parseExpression(authentication!!.token, DataType.RAW, resolver)?.toString()

    // Check if username is set. If yes, use basic auth.
    if (username.isNotEmpty()) {
      logger.debug { "Authentication: Basic" }
      val options = mapOf("authentication" to listOf("basic", username,
        parseExpression(authentication!!.password, DataType.RAW, resolver)))
      return PactBrokerClient(url.toString(), options)
    }

    // Check if token is set. If yes, use bearer auth.
    if (token.isNotEmpty()) {
      logger.debug { "Authentication: Bearer" }
      val options = mapOf("authentication" to listOf("bearer", token))
      return PactBrokerClient(url.toString(), options)
    }

    logger.debug { "Authentication: None" }
    return PactBrokerClient(url.toString(), emptyMap())
  }

  companion object : KLogging()
}
