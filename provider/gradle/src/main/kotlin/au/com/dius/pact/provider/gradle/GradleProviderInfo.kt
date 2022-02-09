package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ConsumersGroup
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.gradle.PactPluginBase.Companion.PACT_VERIFY
import groovy.lang.Closure
import mu.KLogging
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.net.URL

/**
 * Extends the provider info to be setup in a gradle build
 */
open class GradleProviderInfo(name: String, val project: Project) : ProviderInfo(name) {
  var providerVersion: Any? = null
  @Deprecated("Use providerTags instead")
  var providerTag: String? = null
  var providerTags: Any? = null
  var brokerConfig: PactBrokerConsumerConfig? = null

  open fun hasPactWith(consumer: String, closure: Closure<*>): IConsumerInfo {
    val consumerInfo = ConsumerInfo(consumer, null, true, listOf(), this.verificationType)
    consumers.add(consumerInfo)
    ConfigureUtil.configure(closure, consumerInfo)
    return consumerInfo
  }

  open fun hasPactsWith(consumersGroupName: String, closure: Closure<*>): List<IConsumerInfo> {
    val consumersGroup = ConsumersGroup(consumersGroupName)
    ConfigureUtil.configure(closure, consumersGroup)
    return setupConsumerListFromPactFiles(consumersGroup)
  }

  @JvmOverloads
  open fun hasPactsFromPactBroker(
    options: Map<String, Any> = mapOf(),
    pactBrokerUrl: String,
    closure: Closure<*>
  ): List<ConsumerInfo> {
    val fromPactBroker = this.hasPactsFromPactBroker(options, pactBrokerUrl)
    fromPactBroker.forEach {
      ConfigureUtil.configure(closure, it)
    }
    return fromPactBroker
  }

  override fun hasPactsFromPactBroker(options: Map<String, Any>, pactBrokerUrl: String): List<ConsumerInfo> {
    return try {
      super.hasPactsFromPactBroker(options, pactBrokerUrl)
    } catch (e: Exception) {
      val verifyTaskName = PACT_VERIFY.toLowerCase()
      if (project.gradle.startParameter.taskNames.any { it.toLowerCase().contains(verifyTaskName) }) {
        logger.error(e) { "Failed to access Pact Broker" }
        throw e
      } else {
        logger.warn { "Failed to access Pact Broker, no provider tasks will be configured - ${e.message}" }
        emptyList()
      }
    }
  }

  @JvmOverloads
  open fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>,
    closure: Closure<*>
  ): List<ConsumerInfo> {
    val fromPactBroker = this.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
    fromPactBroker.forEach {
      ConfigureUtil.configure(closure, it)
    }
    return fromPactBroker
  }

  override fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any?>,
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>
  ): List<ConsumerInfo> {
    return try {
      super.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
    } catch (e: Exception) {
      val verifyTaskName = PACT_VERIFY.toLowerCase()
      if (project.gradle.startParameter.taskNames.any { it.toLowerCase().contains(verifyTaskName) }) {
        logger.error(e) { "Failed to access Pact Broker" }
        throw e
      } else {
        logger.warn { "Failed to access Pact Broker, no provider tasks will be configured - ${e.message}" }
        emptyList()
      }
    }
  }

  open fun url(path: String) = URL(path)

  open fun fromPactBroker(closure: Closure<*>) {
    brokerConfig = PactBrokerConsumerConfig()
    ConfigureUtil.configure(closure, brokerConfig!!)

    val pending = brokerConfig!!.enablePending ?: false
    if (pending && (brokerConfig!!.providerTags.isNullOrEmpty() ||
      brokerConfig!!.providerTags!!.any { it.trim().isEmpty() })) {
      throw GradleScriptException(
        """
        |No providerTags: To use the pending pacts feature, you need to provide the list of provider names for the provider application version that will be published with the verification results.
        |
        |For instance:
        |
        |fromPactBroker {
        |    selectors = latestTags('test')
        |    enablePending = true
        |    providerTags = ['master']
        |}
        """.trimMargin("|"), null)
    }
  }

  companion object : KLogging()
}
