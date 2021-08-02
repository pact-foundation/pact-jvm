package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.DefaultPluginManager
import io.pact.plugins.jvm.core.PactPlugin
import io.pact.plugins.jvm.core.PactPluginEntryFoundException
import io.pact.plugins.jvm.core.PactPluginNotFoundException
import mu.KLogging

open class PactBuilder(
  var consumer: String = "consumer",
  var provider: String = "provider",
  var pactVersion: PactSpecVersion = PactSpecVersion.V4
) {
  private val plugins: MutableList<PactPlugin> = mutableListOf()
  private val interactions: MutableList<V4Interaction> = mutableListOf()

  init {
    CatalogueManager.registerCoreEntries(MatchingConfig.contentMatcherCatalogueEntries())
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
    val entry = CatalogueManager.lookupEntry(interactionType)
    when {
      entry == null -> {
        logger.error { "No interaction type of '$interactionType' was found in the catalogue" }
        throw PactPluginEntryFoundException(interactionType)
      }
      entry.type == CatalogueEntryType.INTERACTION -> {
        interactions.add(forEntry(entry, description, key))
      }
      else -> {
        TODO("Interactions of type $interactionType are not currently supported")
      }
    }
    return this
  }

  private fun forEntry(entry: CatalogueEntry, description: String, key: String?): V4Interaction {
    when (entry.providerType) {
      CatalogueEntryProviderType.CORE -> when (entry.key) {

      }
      CatalogueEntryProviderType.PLUGIN -> TODO()
    }
  }

  companion object : KLogging()
}
