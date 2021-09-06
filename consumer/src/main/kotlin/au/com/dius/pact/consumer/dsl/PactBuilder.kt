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
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.deepMerge
import au.com.dius.pact.core.support.json.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.ContentMatcher
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.PactPlugin
import io.pact.plugins.jvm.core.PactPluginEntryNotFoundException
import io.pact.plugins.jvm.core.PactPluginNotFoundException
import mu.KLogging
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

open class PactBuilder(
  var consumer: String = "consumer",
  var provider: String = "provider",
  var pactVersion: PactSpecVersion = PactSpecVersion.V4
) {
  private val plugins: MutableList<PactPlugin> = mutableListOf()
  private val interactions: MutableList<Interaction> = mutableListOf()
  private var currentInteraction: V4Interaction? = null
  private val pluginConfiguration: MutableMap<String, MutableMap<String, JsonValue>> = mutableMapOf()

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
   * Use the old HTTP Pact DSL
   */
  fun usingLegacyMessageDsl(): MessagePactBuilder {
    return MessagePactBuilder(pactVersion).consumer(consumer).hasPactWith(provider)
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
   * Adds an interaction with the given description and type
   */
  @JvmOverloads
  fun expectsToReceive(description: String, interactionType: String, key: String? = null): PactBuilder {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
    }

    val entry = CatalogueManager.lookupEntry(interactionType)
    when {
      entry == null -> {
        logger.error { "No interaction type of '$interactionType' was found in the catalogue" }
        throw PactPluginEntryNotFoundException(interactionType)
      }
      entry.type == CatalogueEntryType.INTERACTION -> {
        currentInteraction = forEntry(entry, description, key)
      }
      else -> {
        TODO("Interactions of type $interactionType are not currently supported")
      }
    }
    return this
  }

  private fun forEntry(entry: CatalogueEntry, description: String, key: String?): V4Interaction {
    return when (entry.providerType) {
      CatalogueEntryProviderType.CORE -> when (entry.key) {
        "http", "https" -> V4Interaction.SynchronousHttp(key.orEmpty(), description)
        "message" -> V4Interaction.AsynchronousMessage(key.orEmpty(), description)
        else -> TODO("Interactions of type ${entry.key} are not currently supported")
      }
      CatalogueEntryProviderType.PLUGIN -> TODO()
    }
  }

  /**
   * Values to configure the interaction
   */
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
        logger.debug { "Configuring interaction from $values" }
        if (values.containsKey("message.contents")) {
          interaction.contents = setupMessageContents(values["message.contents"], interaction)
        }
        interaction.updateProperties(values.filter { it.key != "message.contents" })
      }
      is V4Interaction.SynchronousMessages -> TODO()
    }
    return this
  }

  private fun setupMessageContents(contents: Any?, interaction: V4Interaction.AsynchronousMessage): MessageContents {
    logger.debug { "Explicit contents, will look for a content matcher" }
    return when (contents) {
      is Map<*, *> -> if (contents.containsKey("content-type")) {
        val contentType = contents["content-type"].toString()
        val bodyConfig = contents.filter { it.key != "content-type" } as Map<String, Any?>
        val matcher = CatalogueManager.findContentMatcher(ContentType(contentType))
        logger.debug { "Found a matcher for '$contentType': $matcher" }
        if (matcher == null || matcher.isCore) {
          logger.debug { "Either no matcher was found, or a core matcher, will use the internal implementation" }
          val contentMatcher = MatchingConfig.lookupContentMatcher(contentType)
          if (contentMatcher != null) {
            val (body, rules, generators, _, _) = contentMatcher.setupBodyFromConfig(bodyConfig)
            val matchingRules = MatchingRulesImpl()
            if (rules != null) {
              matchingRules.addCategory(rules)
            }
            MessageContents(body, mapOf(), matchingRules, generators ?: Generators())
          } else {
            MessageContents(OptionalBody.body(toJson(bodyConfig).serialise().toByteArray(), ContentType(contentType)))
          }
        } else {
          logger.debug { "Plugin matcher, will get the plugin to provide the interaction contents" }
          val (body, rules, generators, metadata, config) = matcher.configureContent(contentType, bodyConfig)
          val matchingRules = MatchingRulesImpl()
          if (rules != null) {
            matchingRules.addCategory(rules)
          }
          if (config.interactionConfiguration.isNotEmpty()) {
            interaction.addPluginConfiguration(matcher.pluginName, config.interactionConfiguration)
          }
          if (config.pactConfiguration.isNotEmpty()) {
            addPluginConfiguration(matcher, config.pactConfiguration)
          }
          MessageContents(body, metadata, matchingRules, generators ?: Generators())
        }
      } else {
        MessageContents(OptionalBody.body(toJson(contents).serialise().toByteArray()))
      }
      else -> MessageContents(OptionalBody.body(contents.toString().toByteArray()))
    }
  }

  private fun addPluginConfiguration(contentMatcher: ContentMatcher, pactConfiguration: Map<String, JsonValue>) {
    if (pluginConfiguration.containsKey(contentMatcher.pluginName)) {
      pluginConfiguration[contentMatcher.pluginName].deepMerge(pactConfiguration)
    } else {
      pluginConfiguration[contentMatcher.pluginName] = pactConfiguration.toMutableMap()
    }
  }

  private fun setupContents(contents: Any?, part: IHttpPart, interaction: V4Interaction.SynchronousHttp) {
    logger.debug { "Explicit contents, will look for a content matcher" }
    when (contents) {
      is Map<*, *> -> if (contents.containsKey("content-type")) {
        val contentType = contents["content-type"].toString()
        val bodyConfig = contents.filter { it.key != "content-type" } as Map<String, Any?>
        val matcher = CatalogueManager.findContentMatcher(ContentType(contentType))
        logger.debug { "Found a matcher for '$contentType': $matcher" }
        if (matcher == null || matcher.isCore) {
          logger.debug { "Either no matcher was found, or a core matcher, will use the internal implementation" }
          val contentMatcher = MatchingConfig.lookupContentMatcher(contentType)
          if (contentMatcher != null) {
            val (body, rules, generators, _, _) = contentMatcher.setupBodyFromConfig(bodyConfig)
            part.body = body
            if (rules != null) {
              part.matchingRules.addCategory(rules)
            }
            if (generators != null) {
              part.generators.addGenerators(generators)
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
    val (body, rules, generators, _, config) = matcher.configureContent(contentType, bodyConfig)
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

    if (config.interactionConfiguration.isNotEmpty()) {
      interaction.addPluginConfiguration(matcher.pluginName, config.interactionConfiguration)
    }
    if (config.pactConfiguration.isNotEmpty()) {
      addPluginConfiguration(matcher, config.pactConfiguration)
    }
  }

  fun toPact(): V4Pact {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
    }
    return V4Pact(Consumer(consumer), Provider(provider), interactions,
      BasePact.metaData(null, PactSpecVersion.V4) + pluginMetadata(),
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

  companion object : KLogging() {
    @JvmStatic
    fun textFile(filePath: String): String {
      var path = Paths.get(filePath)
      if (!path.exists()) {
        val cwd = Path.of("").toAbsolutePath()
        path = cwd.resolve(filePath).toAbsolutePath()
      }
      return path.toFile().bufferedReader().readText()
    }

    @JvmStatic
    fun filePath(filePath: String): String {
      var path = Paths.get(filePath).toAbsolutePath()
      if (!path.exists()) {
        val cwd = Path.of("").toAbsolutePath()
        path = cwd.resolve(filePath).toAbsolutePath()
      }
      return path.normalize().toString()
    }
  }
}

