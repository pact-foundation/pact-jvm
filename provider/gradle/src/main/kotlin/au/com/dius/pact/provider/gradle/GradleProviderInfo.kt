package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ConsumersGroup
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.gradle.PactPluginBase.Companion.PACT_VERIFY
import groovy.lang.Closure
import mu.KLogging
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.io.File
import java.net.URL

/**
 * Extends the provider info to be setup in a gradle build
 */
open class GradleProviderInfo(override var name: String, val project: Project) : IProviderInfo {
  var providerVersion: Any? = null
  var providerTags: Any? = null
  var brokerConfig: PactBrokerConsumerConfig? = null
  val provider = ProviderInfo(name)

  override var protocol: String by provider::protocol
  override var host: Any? by provider::host
  override var port: Any? by provider::port
  override var path: String by provider::path
  override var requestFilter: Any? by provider::requestFilter
  override var stateChangeRequestFilter: Any? by provider::stateChangeRequestFilter
  override var stateChangeUrl: URL? by provider::stateChangeUrl
  override var stateChangeUsesBody: Boolean by provider::stateChangeUsesBody
  override var stateChangeTeardown: Boolean by provider::stateChangeTeardown
  override var packagesToScan: List<String> by provider::packagesToScan
  override var verificationType: PactVerification? by provider::verificationType
  override var createClient: Any? by provider::createClient
  override var insecure: Boolean by provider::insecure
  override var trustStore: File? by provider::trustStore
  override var trustStorePassword: String? by provider::trustStorePassword
  override var consumers: MutableList<IConsumerInfo> by provider::consumers
  var startProviderTask: Any? by provider::startProviderTask
  var terminateProviderTask: Any? by provider::terminateProviderTask
  var isDependencyForPactVerify: Boolean by provider::isDependencyForPactVerify

  open fun hasPactWith(consumer: String, closure: Closure<*>): IConsumerInfo {
    val consumerInfo = ConsumerInfo(consumer, null, true, listOf(), this.verificationType)
    provider.consumers.add(consumerInfo)
    ConfigureUtil.configure(closure, consumerInfo)
    return consumerInfo
  }

  open fun hasPactsWith(consumersGroupName: String, closure: Closure<*>): List<IConsumerInfo> {
    val consumersGroup = ConsumersGroup(consumersGroupName)
    ConfigureUtil.configure(closure, consumersGroup)
    return provider.setupConsumerListFromPactFiles(consumersGroup)
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

  fun hasPactsFromPactBroker(options: Map<String, Any>, pactBrokerUrl: String): List<ConsumerInfo> {
    return try {
      provider.hasPactsFromPactBroker(options, pactBrokerUrl)
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

  fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any?>,
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>
  ): List<ConsumerInfo> {
    return try {
      provider.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
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
    if (pending
      && (brokerConfig!!.providerTags.isNullOrEmpty() || brokerConfig!!.providerTags!!.any { it.trim().isEmpty() })
      && (brokerConfig!!.providerBranch.isNullOrBlank())
      ) {
      throw GradleScriptException(
        """
        |No providerTags or providerBranch: To use the pending pacts feature, you need to provide the list of provider names for the provider application version that will be published with the verification results.
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
