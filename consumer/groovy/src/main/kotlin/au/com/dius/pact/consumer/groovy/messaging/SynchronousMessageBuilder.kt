package au.com.dius.pact.consumer.groovy.messaging

import au.com.dius.pact.consumer.dsl.DslBuilder
import au.com.dius.pact.consumer.dsl.MessageContentsBuilder
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.deepMerge
import au.com.dius.pact.core.support.json.JsonValue
import groovy.lang.Closure
import io.pact.plugins.jvm.core.ContentMatcher
import io.github.oshai.kotlinlogging.KLogging

open class SynchronousMessageBuilder(
  private val description: String,
  private val key: String?,
  private val providerStates: List<ProviderState>
) : DslBuilder {
  private var interaction = V4Interaction.SynchronousMessages(key, description, null, providerStates)
  val pluginConfiguration: MutableMap<String, MutableMap<String, JsonValue>> = mutableMapOf()

  open fun build(): V4Interaction.SynchronousMessages {
    return if (key == null) {
      interaction.withGeneratedKey() as V4Interaction.SynchronousMessages
    } else {
      interaction
    }
  }

  /**
   * Adds a comment to message interaction
   */
  fun comment(comment: String): SynchronousMessageBuilder {
    if (comment.isNotEmpty()) {
      val text = interaction.comments["text"]
      if (text is JsonValue.Array) {
        text.add(JsonValue.StringValue(comment))
        interaction.comments["text"] = text
      } else {
        interaction.comments["text"] = JsonValue.Array(mutableListOf(JsonValue.StringValue(comment)))
      }
    }

    return this
  }

  /**
   * Sets the name of the test
   */
  fun testname(testname: String): SynchronousMessageBuilder {
    if (testname.isNotEmpty()) {
      interaction.comments["testname"] = JsonValue.StringValue(testname)
    }
    return this
  }

  /**
   * If this interaction should be marked as pending
   */
  fun interactionPending(pending: Boolean): SynchronousMessageBuilder {
    interaction.pending = pending
    return this
  }

  /**
   * Build the request part of the message
   */
  fun withRequest(callback: Closure<Any>): SynchronousMessageBuilder {
    val builder = MessageContentsBuilder(MessageContents())
    callback.delegate = builder
    callback.resolveStrategy = Closure.DELEGATE_FIRST
    callback.call(builder)
    interaction.request = builder.contents

    return this
  }

  /**
   * Build the response part of the message
   */
  fun withResponse(callback: Closure<Any>): SynchronousMessageBuilder {
    val builder = MessageContentsBuilder(MessageContents())
    callback.delegate = builder
    callback.resolveStrategy = Closure.DELEGATE_FIRST
    callback.call(builder)
    interaction.response.add(builder.contents)
    return this
  }

  /**
   * Values to configure the interaction with. This will send the configuration through to the plugin to setup the
   * interaction.
   */
  fun withPluginConfig(values: Map<String, Any?>): SynchronousMessageBuilder {
    logger.debug { "Configuring SynchronousMessages interaction from $values" }
    val interaction = this.build()
    val result = PactBuilder.setupMessageContents(this, values, interaction)
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

    this.interaction = interaction

    return this
  }

  override fun addPluginConfiguration(matcher: ContentMatcher, pactConfiguration: Map<String, JsonValue>) {
    if (pluginConfiguration.containsKey(matcher.pluginName)) {
      pluginConfiguration[matcher.pluginName].deepMerge(pactConfiguration)
    } else {
      pluginConfiguration[matcher.pluginName] = pactConfiguration.toMutableMap()
    }
  }

  companion object: KLogging()
}
