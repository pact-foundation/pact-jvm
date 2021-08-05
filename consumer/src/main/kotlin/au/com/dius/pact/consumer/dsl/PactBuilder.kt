package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.interactionCatalogueEntries
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.matcherCatalogueEntries
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.unwrap
import au.com.dius.pact.core.support.Json.toJson
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.ContentMatcher
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.PactPlugin
import io.pact.plugins.jvm.core.PactPluginEntryFoundException
import io.pact.plugins.jvm.core.PactPluginNotFoundException
import io.pact.plugins.jvm.core.PluginManager
import mu.KLogging

open class PactBuilder(
  var consumer: String = "consumer",
  var provider: String = "provider",
  var pactVersion: PactSpecVersion = PactSpecVersion.V4
) {
  private val plugins: MutableList<PactPlugin> = mutableListOf()
  private val interactions: MutableList<Interaction> = mutableListOf()
  private var currentInteraction: V4Interaction? = null

  init {
    CatalogueManager.registerCoreEntries(MatchingConfig.contentMatcherCatalogueEntries() +
      matcherCatalogueEntries() + interactionCatalogueEntries())
  }

  /**
   * Use the old HTTP Pact DSL
   */
  fun usingLegacyDsl(): PactDslWithProvider {
    return PactDslWithProvider(ConsumerPactBuilder(consumer), provider, pactVersion)
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
        throw PactPluginEntryFoundException(interactionType)
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
        else -> TODO()
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
    currentInteraction!!.updateProperties(values)
    return this
  }

  /**
   * Values to configure the response
   */
  fun willRespondWith(values: Map<String, Any?>): PactBuilder {
    require(currentInteraction != null) {
      "'with' must be preceded by 'expectsToReceive'"
    }
    when (val interaction = currentInteraction) {
      is V4Interaction.AsynchronousMessage -> TODO()
      is V4Interaction.SynchronousHttp -> {
        logger.debug { "Configuring interaction response from $values" }
        if (values.containsKey("contents")) {
          logger.debug { "Interaction has explicit contents, will look for a content matcher" }
          val contents = values["contents"]
          interaction.response.updateProperties(values.filter { it.key != "contents" })
          when (contents) {
            is Map<*, *> -> if (contents.containsKey("content-type")) {
              val contentType = contents["content-type"].toString()
              val bodyConfig = contents.filter { it.key != "content-type" } as Map<String, Any?>
              val matcher = CatalogueManager.findContentMatcher(io.pact.plugins.jvm.core.ContentType(contentType))
              logger.debug { "Found a matcher for '$contentType': $matcher" }
              if (matcher == null || matcher.isCore) {
                logger.debug { "Either no matcher was found, or a core matcher, will use the internal implementation" }
                val contentMatcher = MatchingConfig.lookupContentMatcher(contentType)
                if (contentMatcher != null) {
                  val (body, rules, generators) = contentMatcher.setupBodyFromConfig(bodyConfig)
                  interaction.response.body = body
                  if (rules != null) {
                    interaction.response.matchingRules.addCategory(rules)
                  }
                  if (generators != null) {
                    interaction.response.generators.addGenerators(generators)
                  }
                } else {
                  interaction.response.body = OptionalBody.body(toJson(
                    bodyConfig).serialise().toByteArray(), ContentType(contentType))
                }
              } else {
                logger.debug { "Plugin matcher, will get the plugin to provide the interaction contents" }
                setupResponseFromPlugin(matcher, contentType, bodyConfig, interaction)
              }
            } else {
              interaction.response.body = OptionalBody.body(toJson(contents).serialise().toByteArray())
            }
            else -> interaction.response.body = OptionalBody.body(contents.toString().toByteArray())
          }
        } else {
          interaction.response.updateProperties(values)
        }
      }
    }
    return this
  }

  private fun setupResponseFromPlugin(
    matcher: ContentMatcher,
    contentType: String,
    bodyConfig: Map<String, Any?>,
    interaction: V4Interaction.SynchronousHttp
  ) {
    val (body, rules, generators) = matcher.configureContent(contentType, bodyConfig)
    interaction.response.body = body
    if (!interaction.response.hasHeader("content-type")) {
      interaction.response.headers["content-type"] = listOf(body.contentType.toString())
    }
    if (rules != null) {
      interaction.response.matchingRules.addCategory(rules)
    }
    if (generators != null) {
      interaction.response.generators.addGenerators(generators)
    }
    logger.debug { "Interaction from plugin: ${interaction.response}" }
  }

  fun toPact(): V4Pact {
    if (currentInteraction != null) {
      interactions.add(currentInteraction!!)
    }
    return V4Pact(Consumer(consumer), Provider(provider), interactions,
      BasePact.metaData(null, PactSpecVersion.V4) + pluginMetadata(),
      UnknownPactSource)
  }

  private fun pluginMetadata(): Map<String, Any> {
    return mapOf("plugins" to plugins.map {
      mapOf(
        "name" to it.manifest.name,
        "version" to it.manifest.version
      )
    })
  }

  companion object : KLogging()
}
