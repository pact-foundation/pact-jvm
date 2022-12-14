package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.consumer.groovy.GroovyBuilder
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.BuiltToolConfig
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.json.JsonValue
import groovy.lang.Closure
import groovy.lang.Closure.DELEGATE_FIRST
import groovy.lang.DelegatesTo
import java.util.function.BiFunction

/**
 * Pact builder for consumer tests for synchronous messaging
 */
open class PactSynchronousMessageBuilder @JvmOverloads constructor(
  pactVersion: PactSpecVersion = PactSpecVersion.V4
): GroovyBuilder(pactVersion) {
  var consumer = Consumer()
  var provider = Provider()
  val providerStates: MutableList<ProviderState> = mutableListOf()
  val messages: MutableList<V4Interaction.SynchronousMessages> = mutableListOf()
  private val pluginConfiguration: MutableMap<String, MutableMap<String, JsonValue>> = mutableMapOf()

  init {
    if (pactVersion < PactSpecVersion.V4) {
      throw RuntimeException("SynchronousMessages require V4 Pact format")
    }
  }

  /**
   * Service consumer
   */
  fun serviceConsumer(consumer: String): PactSynchronousMessageBuilder {
    this.consumer = Consumer(consumer)
    return this
  }

  /**
   * Provider that the consumer has a pact with
   */
  fun hasPactWith(provider: String): PactSynchronousMessageBuilder {
    this.provider = Provider(provider)
    return this
  }

  /**
   * Provider state required for the message to be produced
   */
  @JvmOverloads
  fun given(providerState: String, params: Map<String, Any?> = mapOf()): PactSynchronousMessageBuilder {
    this.providerStates.add(ProviderState(providerState, params))
    return this
  }

  /**
   * Enable the plugin
   * @param name Plugin Name
   * @param version Plugin Version
   */
  fun usingPlugin(name: String, version: String): PactSynchronousMessageBuilder {
    return super.usingPlugin(name, version) as PactSynchronousMessageBuilder
  }

  /**
   * Enable the plugin
   * @param name Plugin Name
   */
  override fun usingPlugin(name: String): PactSynchronousMessageBuilder {
    return super.usingPlugin(name) as PactSynchronousMessageBuilder
  }

  /**
   * Description of the message to be received
   * @param description Message description. Must be unique.
   */
  @JvmOverloads
  fun expectsToReceive(description: String, key: String? = null, callback: Closure<Any>): PactSynchronousMessageBuilder {
    val builder = SynchronousMessageBuilder(description, key, providerStates)
    callback.delegate = builder
    callback.resolveStrategy = DELEGATE_FIRST
    callback.call(builder)
    messages.add(builder.build())
    pluginConfiguration.putAll(builder.pluginConfiguration)
    return this
  }

  /**
   * Execute the given closure for each defined message
   */
  fun run(closure: BiFunction<V4Interaction.SynchronousMessages, V4Pact, Any?>): List<Any?> {
    val pact = V4Pact(consumer, provider, messages.toMutableList(),
      BasePact.metaData(null, PactSpecVersion.V4) + pluginMetadata())
    val results = messages.map {
      try {
        closure.apply(it, pact)
      } catch (ex: Exception) {
        ex
      }
    }

    Metrics.sendMetrics(MetricEvent.ConsumerTestRun(messages.size, "groovy"))

    if (results.any { it is Exception }) {
      throw MessagePactFailedException(results.filterIsInstance<Exception>().map { it.message.orEmpty() })
    } else {
      if (pactVersion >= PactSpecVersion.V4) {
        pact.write(BuiltToolConfig.pactDirectory, pactVersion)
        return results
      } else {
        throw RuntimeException("SynchronousMessages require V4 Pact format")
      }
    }
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

  override fun call(
    @DelegatesTo(value = PactSynchronousMessageBuilder::class, strategy = DELEGATE_FIRST) closure: Closure<Any?>
  ): Any? {
	  return super.build(closure)
  }

	override fun build(
    @DelegatesTo(value = PactSynchronousMessageBuilder::class, strategy = DELEGATE_FIRST) closure: Closure<Any?>
  ): Any? {
		return super.build(closure)
	}
}
