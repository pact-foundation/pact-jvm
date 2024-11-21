package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.interactionCatalogueEntries
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.MatchingConfig.contentHandlerCatalogueEntries
import au.com.dius.pact.core.matchers.MatchingConfig.contentMatcherCatalogueEntries
import au.com.dius.pact.core.matchers.matcherCatalogueEntries
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IHttpPart
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.InteractionMarkup
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.Result.Ok
import au.com.dius.pact.core.support.Result.Err
import au.com.dius.pact.core.support.deepMerge
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KLogging
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.ContentMatcher
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.PactPlugin
import io.pact.plugins.jvm.core.PactPluginEntryNotFoundException
import io.pact.plugins.jvm.core.PactPluginNotFoundException

interface DslBuilder {
  fun addPluginConfiguration(matcher: ContentMatcher, pactConfiguration: Map<String, JsonValue>)
}

/**
 * Sets up the data required by a plugin to configure an interaction
 */
interface PluginInteractionBuilder {
  /**
   * Construct the map of configuration that is to be passed through to the plugin
   */
  fun build(): Map<String, Any?>
}

/**
 * Pact builder DSL that supports V4 formatted Pact files
 */
@Suppress("TooManyFunctions")
open class PactBuilder(
  var consumer: String = "consumer",
  var provider: String = "provider",
  var pactVersion: PactSpecVersion = PactSpecVersion.V4
): DslBuilder {
  private val plugins: MutableList<PactPlugin> = mutableListOf()
  private val interactions: MutableList<V4Interaction> = mutableListOf()
  private var currentInteraction: V4Interaction? = null
  private val providerStates: MutableList<ProviderState> = mutableListOf()
  private val pluginConfiguration: MutableMap<String, MutableMap<String, JsonValue>> = mutableMapOf()
  private val additionalMetadata: MutableMap<String, JsonValue> = mutableMapOf()
  private val comments: MutableList<JsonValue.StringValue> = mutableListOf()

  init {
    CatalogueManager.registerCoreEntries(contentMatcherCatalogueEntries() +
      matcherCatalogueEntries() + interactionCatalogueEntries() + contentHandlerCatalogueEntries())
  }

  /**
   * Use the old HTTP Pact DSL
   */
  fun usingLegacyDsl(): PactDslWithProvider {
    return PactDslWithProvider(ConsumerPactBuilder(consumer), provider, pactVersion)
  }

  /**
   * Use the old Message Pact DSL
   */
  fun usingLegacyMessageDsl(): MessagePactBuilder {
    return MessagePactBuilder(pactVersion).consumer(consumer).hasPactWith(provider)
  }

  /**
   * Use the Synchronous Message DSL
   */
  fun usingSynchronousMessageDsl(): SynchronousMessagePactBuilder {
    return SynchronousMessagePactBuilder(pactVersion).consumer(consumer).hasPactWith(provider)
  }

  /**
   * Sets the Pact specification version
   */
  fun pactSpecVersion(version: PactSpecVersion) {
    pactVersion = version
  }

  /**
   * Enable a plugin
   */
  @JvmOverloads
  fun usingPlugin(name: String, version: String? = null): PactBuilder {
    val plugin = findPlugin(name, version)
    if (plugin == null) {
      when (val result = DefaultPluginManager.loadPlugin(name, version)) {
        is Ok -> plugins.add(result.value)
        is Err -> {
          logger.error { result.error }
          throw PactPluginNotFoundException(name, version)
        }
      }
    }
    return this
  }

  private fun findPlugin(name: String, version: String?): PactPlugin? {
    return if (version == null) {
      plugins.filter { it.manifest.name == name }.maxByOrNull { it.manifest.version }
    } else {
      plugins.find { it.manifest.name == name && it.manifest.version == version }
    }
  }

  /**
   * Describe the state the provider needs to be in for the pact test to be verified. Any parameters for the provider
   * state can be provided in the second parameter.
   */
  @JvmOverloads
  fun given(state: String, params: Map<String, Any?> = emptyMap()): PactBuilder {
    if (currentInteraction != null) {
      currentInteraction!!.providerStates.add(ProviderState(state, params))
    } else {
      providerStates.add(ProviderState(state, params))
    }
    return this
  }

  /**
   * Describe the state the provider needs to be in for the pact test to be verified.
   *
   * @param firstKey Key of first parameter element
   * @param firstValue Value of first parameter element
   * @param paramsKeyValuePair Additional parameters in key-value pairs
   */
  fun given(state: String, firstKey: String, firstValue: Any?, vararg paramsKeyValuePair: Any): PactBuilder {
    require(paramsKeyValuePair.size % 2 == 0) {
      "Pairs of key value should be provided, but there is one key without value."
    }
    val params = mutableMapOf(firstKey to firstValue)
    var i = 0
    while (i < paramsKeyValuePair.size) {
      params[paramsKeyValuePair[i].toString()] = paramsKeyValuePair[i + 1]
      i += 2
    }
    if (currentInteraction != null) {
      currentInteraction!!.providerStates.add(ProviderState(state, params))
    } else {
      providerStates.add(ProviderState(state, params))
    }
    return this
  }

  /**
   * Describe the state the provider needs to be in for the pact test to be verified.
   *
   * @param params Additional parameters in key-value pairs
   */
  fun given(state: String, vararg params: Pair<String, Any>): PactBuilder {
    if (currentInteraction != null) {
      currentInteraction!!.providerStates.add(ProviderState(state, params.toMap()))
    } else {
      providerStates.add(ProviderState(state, params.toMap()))
    }
    return this
  }

  /**
   * Adds an interaction with the given description and type. If interactionType is not specified (is the empty string)
   * will default to an HTTP interaction
   *
   * @param description The interaction description. Must be unique.
   * @param interactionType The key of the interaction type as found in the catalogue manager. If empty, will default to
   * a HTTP interaction ('core/transport/http').
   * @param key (Optional) unique key to assign to the interaction
   */
  @JvmOverloads
  fun expectsToReceive(description: String, interactionType: String, key: String? = null): PactBuilder {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
    }

    val entry = CatalogueManager.lookupEntry(interactionType.ifEmpty { "core/transport/http" })
    when {
      entry == null -> {
        logger.error { "No interaction type of '$interactionType' was found in the catalogue" }
        throw PactPluginEntryNotFoundException(interactionType)
      }
      entry.type == CatalogueEntryType.INTERACTION || entry.type == CatalogueEntryType.TRANSPORT -> {
        currentInteraction = forEntry(entry, description, key)
      }
      else -> {
        TODO("Interactions of type '$interactionType' are not currently supported")
      }
    }

    if (providerStates.isNotEmpty()) {
      currentInteraction!!.providerStates.addAll(providerStates)
      providerStates.clear()
    }

    if (comments.isNotEmpty()) {
      currentInteraction!!.comments.merge("text", JsonValue.Array(comments.toMutableList())) { a, b ->
        a.asArray()!!.addAll(b)
        a
      }
      comments.clear()
    }

    return this
  }

  private fun forEntry(entry: CatalogueEntry, description: String, key: String?): V4Interaction {
    return when (entry.providerType) {
      CatalogueEntryProviderType.CORE -> when (entry.key) {
        "http", "https" -> V4Interaction.SynchronousHttp(key.orEmpty(), description)
        "message" -> V4Interaction.AsynchronousMessage(key.orEmpty(), description)
        "synchronous-message" -> V4Interaction.SynchronousMessages(key.orEmpty(), description)
        else -> TODO("Interactions of type '${entry.key}' are not currently supported")
      }
      CatalogueEntryProviderType.PLUGIN -> TODO()
    }
  }

  /**
   * Values to configure the interaction. In the case of an interaction configured by a plugin, you need to follow
   * the plugin documentation of what values must be specified here.
   */
  @Suppress("ComplexMethod")
  fun with(values: Map<String, Any?>): PactBuilder {
    require(currentInteraction != null) {
      "'with' must be preceded by 'expectsToReceive'"
    }

    when (val interaction = currentInteraction) {
      is V4Interaction.SynchronousHttp -> {
        logger.debug { "Configuring interaction from $values" }
        if (values.containsKey("request.contents")) {
          setupContents(values["request.contents"], interaction.request, interaction)
        }
        if (values.containsKey("response.contents")) {
          setupContents(values["response.contents"], interaction.response, interaction)
        }
        interaction.updateProperties(values.filter { it.key != "request.contents" && it.key != "response.contents" })
      }

      is V4Interaction.AsynchronousMessage -> {
        logger.debug { "Configuring AsynchronousMessage interaction from $values" }
        if (values.containsKey("message.contents")) {
          val messageContents = setupMessageContents(this, values["message.contents"], interaction)
          if (messageContents.size > 1) {
            logger.warn { "Received multiple values for the interaction contents, will only use the first" }
          }
          val contents = messageContents.first()
          interaction.contents = contents.first
          if (contents.second.isNotEmpty()) {
            interaction.interactionMarkup = contents.second
          }
        }
        interaction.updateProperties(values.filter { it.key != "message.contents" })
      }

      is V4Interaction.SynchronousMessages -> {
        logger.debug { "Configuring SynchronousMessages interaction from $values" }
        val result = setupMessageContents(this, values, interaction)
        val requestContents = result.find { it.first.partName == "request" }
        if (requestContents != null) {
          interaction.request = requestContents.first
          if (requestContents.second.isNotEmpty()) {
            interaction.interactionMarkup = requestContents.second
          }
        }

        for (response in result.filter { it.first.partName == "response" }) {
          interaction.response.add(response.first)
          if (response.second.isNotEmpty()) {
            interaction.interactionMarkup = interaction.interactionMarkup.merge(response.second)
          }
        }

        interaction.updateProperties(values.filter { it.key != "request" && it.key != "response" })
      }
      else -> {}
    }

    return this
  }

  /**
   * Configure the interaction using a builder supplied by the plugin author.
   */
  fun with(builder: PluginInteractionBuilder): PactBuilder {
    require(currentInteraction != null) {
      "'with' must be preceded by 'expectsToReceive'"
    }
    return with(builder.build())
  }

  override fun addPluginConfiguration(matcher: ContentMatcher, pactConfiguration: Map<String, JsonValue>) {
    if (pluginConfiguration.containsKey(matcher.pluginName)) {
      pluginConfiguration[matcher.pluginName] = pluginConfiguration[matcher.pluginName].deepMerge(pactConfiguration)
    } else {
      pluginConfiguration[matcher.pluginName] = pactConfiguration.toMutableMap()
    }
  }

  @Suppress("NestedBlockDepth")
  private fun setupContents(contents: Any?, part: IHttpPart, interaction: V4Interaction.SynchronousHttp) {
    logger.debug { "Explicit contents, will look for a content matcher" }
    when (contents) {
      is Map<*, *> -> if (contents.containsKey("pact:content-type")) {
        val contentType = contents["pact:content-type"].toString()
        val bodyConfig = contents.filter { it.key != "pact:content-type" } as Map<String, Any?>
        val matcher = CatalogueManager.findContentMatcher(ContentType(contentType))
        logger.debug { "Found a matcher for '$contentType': $matcher" }
        if (matcher == null || matcher.isCore) {
          logger.debug { "Either no matcher was found, or a core matcher, will use the internal implementation" }
          val contentMatcher = MatchingConfig.lookupContentMatcher(contentType)
          if (contentMatcher != null) {
            when (val result = contentMatcher.setupBodyFromConfig(bodyConfig)) {
              is Ok -> {
                if (result.value.size > 1) {
                  logger.warn { "Plugin returned multiple contents, will only use the first" }
                }
                val (_, body, rules, generators, _, _, _, _, _) = result.value.first()
                part.body = body
                if (rules != null) {
                  part.matchingRules.addCategory(rules)
                }
                if (generators != null) {
                  part.generators.addGenerators(generators)
                }
              }
              is Err -> throw InteractionConfigurationError("Failed to set the interaction: " + result.error)
            }
          } else {
            part.body = OptionalBody.body(toJson(bodyConfig).serialise().toByteArray(), ContentType(contentType))
          }
        } else {
          logger.debug { "Plugin matcher, will get the plugin to provide the interaction contents" }
          setupBodyFromPlugin(matcher, contentType, bodyConfig, part, interaction)
        }
      } else {
        part.body = OptionalBody.body(toJson(contents).serialise().toByteArray())
      }
      else -> part.body = OptionalBody.body(contents.toString().toByteArray())
    }
  }

  private fun setupBodyFromPlugin(
    matcher: ContentMatcher,
    contentType: String,
    bodyConfig: Map<String, Any?>,
    part: IHttpPart,
    interaction: V4Interaction
  ) {
    when (val result = matcher.configureContent(contentType, bodyConfig)) {
      is Ok -> {
        if (result.value.size > 1) {
          logger.warn { "Plugin returned multiple contents, will only use the first" }
        }
        val (_, body, rules, generators, _, config, interactionMarkup, interactionMarkupType, _) = result.value.first()
        part.body = body
        if (!part.hasHeader("content-type")) {
          part.headers["content-type"] = listOf(body.contentType.toString())
        }
        if (rules != null) {
          part.matchingRules.addCategory(rules)
        }
        if (generators != null) {
          part.generators.addGenerators(generators)
        }

        logger.debug { "Http part from plugin: $part" }
        logger.debug { "Plugin config: $config" }

        if (config.interactionConfiguration.isNotEmpty()) {
          interaction.addPluginConfiguration(matcher.pluginName, part.transformConfig(config.interactionConfiguration))
        }
        if (config.pactConfiguration.isNotEmpty()) {
          addPluginConfiguration(matcher, config.pactConfiguration)
        }
        if (interactionMarkup.isNotEmpty()) {
          interaction.interactionMarkup = InteractionMarkup(interactionMarkup, interactionMarkupType)
        }
      }
      is Err -> throw InteractionConfigurationError("Failed to set the interaction: " + result.error)
    }
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: String): PactBuilder {
    additionalMetadata[key] = JsonValue.StringValue(value)
    return this
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: JsonValue): PactBuilder {
    additionalMetadata[key] = value
    return this
  }

  /**
   * Terminates this builder and returns the created Pact object
   */
  fun toPact(): V4Pact {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
    }
    val interactions = interactions.map { i ->
      if (i.key.isNotEmpty()) i else i.withGeneratedKey()
    } as List<Interaction>
    return V4Pact(Consumer(consumer), Provider(provider), interactions.toMutableList(),
      BasePact.metaData(null, PactSpecVersion.V4) + additionalMetadata + pluginMetadata(),
      UnknownPactSource)
  }

  private fun pluginMetadata(): Map<String, Any?> {
    return mapOf("plugins" to plugins.map {
      val map = mutableMapOf<String, Any?>(
        "name" to it.manifest.name,
        "version" to it.manifest.version
      )
      if (pluginConfiguration.containsKey(it.manifest.name)) {
        map["configuration"] = pluginConfiguration[it.manifest.name]
      }
      map
    })
  }

  /**
   * Adds a text comment to the Pact interaction
   */
  fun comment(comment: String): PactBuilder {
    if (currentInteraction != null) {
      currentInteraction!!.comments.merge("text", JsonValue.Array.of(JsonValue.StringValue(comment))) { a, b ->
        a.asArray()!!.addAll(b)
        a
      }
    } else {
      this.comments.add(JsonValue.StringValue(comment))
    }
    return this
  }

  /**
   * Creates a new HTTP interaction with the given description, and passes a builder to the builder function to
   * construct it.
   */
  fun expectsToReceiveHttpInteraction(
    description: String,
    builderFn: (HttpInteractionBuilder) -> HttpInteractionBuilder?
  ): PactBuilder {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
      currentInteraction = null
    }

    val builder = HttpInteractionBuilder(description, providerStates, comments)
    val result = builderFn(builder)
    if (result != null) {
      interactions.add(result.build())
    } else {
      interactions.add(builder.build())
    }

    providerStates.clear()
    comments.clear()

    return this
  }

  /**
   * Creates a new asynchronous message interaction with the given description, and passes a builder to the builder
   * function to construct it.
   */
  fun expectsToReceiveMessageInteraction(
    description: String,
    builderFn: (MessageInteractionBuilder) -> MessageInteractionBuilder?
  ): PactBuilder {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
      currentInteraction = null
    }

    val builder = MessageInteractionBuilder(description, providerStates, comments)
    val result = builderFn(builder)
    if (result != null) {
      interactions.add(result.build())
    } else {
      interactions.add(builder.build())
    }

    providerStates.clear()
    comments.clear()

    return this
  }

  /**
   * Creates a new synchronous message interaction with the given description, and passes a builder to the builder
   * function to construct it.
   */
  fun expectsToReceiveSynchronousMessageInteraction(
    description: String,
    builderFn: (SynchronousMessageInteractionBuilder) -> SynchronousMessageInteractionBuilder?
  ): PactBuilder {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
      currentInteraction = null
    }

    val builder = SynchronousMessageInteractionBuilder(description, providerStates, comments)
    val result = builderFn(builder)
    if (result != null) {
      interactions.add(result.build())
    } else {
      interactions.add(builder.build())
    }

    providerStates.clear()
    comments.clear()

    return this
  }

  companion object : KLogging() {
    @Suppress("LongMethod", "ComplexMethod")
    fun setupMessageContents(
      pactBuilder: DslBuilder,
      contents: Any?,
      interaction: V4Interaction
    ): List<Pair<MessageContents, InteractionMarkup>> {
      logger.debug { "Explicit contents, will look for a content matcher" }
      return when (contents) {
        is Map<*, *> -> if (contents.containsKey("pact:content-type")) {
          val contentType = contents["pact:content-type"].toString()
          val bodyConfig = contents.filter { it.key != "pact:content-type" } as Map<String, Any?>
          val matcher = CatalogueManager.findContentMatcher(ContentType(contentType))
          logger.debug { "Found a matcher for '$contentType': $matcher" }
          if (matcher == null || matcher.isCore) {
            logger.debug { "Either no matcher was found, or a core matcher, will use the internal implementation" }
            val contentMatcher = MatchingConfig.lookupContentMatcher(contentType)
            if (contentMatcher != null) {
              when (val result = contentMatcher.setupBodyFromConfig(bodyConfig)) {
                is Ok -> {
                  result.value.map {
                    val (
                      partName,
                      body,
                      rules,
                      generators,
                      _, _,
                      interactionMarkup,
                      interactionMarkupType,
                      metadataRules
                    ) = it
                    val matchingRules = MatchingRulesImpl()
                    if (rules != null) {
                      matchingRules.addCategory(rules)
                    }
                    if (metadataRules != null) {
                      matchingRules.addCategory(metadataRules)
                    }
                    MessageContents(body, mutableMapOf(), matchingRules, generators ?: Generators(), partName) to
                      InteractionMarkup(interactionMarkup, interactionMarkupType)
                  }
                }
                is Err -> throw InteractionConfigurationError("Failed to set the interaction: " + result.error)
              }
            } else {
              listOf(
                MessageContents(OptionalBody.body(toJson(bodyConfig).serialise().toByteArray(),
                  ContentType(contentType))) to InteractionMarkup()
              )
            }
          } else {
            logger.debug { "Plugin matcher, will get the plugin to provide the interaction contents" }
            when (val result = matcher.configureContent(contentType, bodyConfig)) {
              is Ok -> {
                result.value.map {
                  val (
                    partName,
                    body,
                    rules,
                    generators,
                    metadata,
                    config,
                    interactionMarkup,
                    interactionMarkupType,
                    metadataRules
                  ) = it
                  val matchingRules = MatchingRulesImpl()
                  if (rules != null) {
                    matchingRules.addCategory(rules)
                  }
                  if (metadataRules != null) {
                    matchingRules.addCategory(metadataRules)
                  }
                  if (config.interactionConfiguration.isNotEmpty()) {
                    interaction.addPluginConfiguration(matcher.pluginName, config.interactionConfiguration)
                  }
                  if (config.pactConfiguration.isNotEmpty()) {
                    pactBuilder.addPluginConfiguration(matcher, config.pactConfiguration)
                  }
                  MessageContents(body, metadata.toMutableMap(), matchingRules, generators ?: Generators(), partName) to
                    InteractionMarkup(interactionMarkup, interactionMarkupType)
                }
              }
              is Err -> throw InteractionConfigurationError("Failed to set the interaction: " + result.error)
            }
          }
        } else {
          listOf(MessageContents(OptionalBody.body(toJson(contents).serialise().toByteArray())) to InteractionMarkup())
        }
        else -> listOf(MessageContents(OptionalBody.body(contents.toString().toByteArray())) to InteractionMarkup())
      }
    }

    /**
     * Loads the file given by the file path and returns the contents. Relative paths will be resolved against the
     * current working directory.
     */
    @JvmStatic
    fun textFile(filePath: String) = BuilderUtils.textFile(filePath)

    /**
     * Convenience function to resolve a file path against the current working directory.
     */
    @JvmStatic
    fun filePath(filePath: String) = BuilderUtils.filePath(filePath)
  }
}

class InteractionConfigurationError(error: String) : RuntimeException(error)
