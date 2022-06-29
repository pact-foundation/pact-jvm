package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
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
import java.io.File
import java.net.URL

/**
 * Extends the provider info to be setup in a gradle build
 */
open class GradleProviderInfo(override var name: String, val project: Project) : IProviderInfo {
  var providerVersion: Any? = null
  @Deprecated("Use providerTags instead")
  var providerTag: String? = null
  var providerTags: Any? = null
  var brokerConfig: PactBrokerConsumerConfig = PactBrokerConsumerConfig(project.objects)
  val provider = ProviderInfo(name)

  override var protocol: String
    get() = provider.protocol
    set(value) { provider.protocol = value }
  override var host: Any?
    get() = provider.host
    set(value) { provider.host = value }
  override var port: Any?
    get() = provider.port
    set(value) { provider.port = value }
  override var path: String
    get() = provider.path
    set(value) { provider.path = value }
  override var requestFilter: Any?
    get() = provider.requestFilter
    set(value) { provider.requestFilter = value }
  override var stateChangeRequestFilter: Any?
    get() = provider.stateChangeRequestFilter
    set(value) { provider.stateChangeRequestFilter = value }
  override var stateChangeUrl: URL?
    get() = provider.stateChangeUrl
    set(value) { provider.stateChangeUrl = value }
  override var stateChangeUsesBody: Boolean
    get() = provider.stateChangeUsesBody
    set(value) { provider.stateChangeUsesBody = value }
  override var stateChangeTeardown: Boolean
    get() = provider.stateChangeTeardown
    set(value) { provider.stateChangeTeardown = value }
  override var packagesToScan: List<String>
    get() = provider.packagesToScan
    set(value) { provider.packagesToScan = value }
  override var verificationType: PactVerification?
    get() = provider.verificationType
    set(value) { provider.verificationType = value }
  override var createClient: Any?
    get() = provider.createClient
    set(value) { provider.createClient = value }
  override var insecure: Boolean
    get() = provider.insecure
    set(value) { provider.insecure = value }
  override var trustStore: File?
    get() = provider.trustStore
    set(value) { provider.trustStore = value }
  override var trustStorePassword: String?
    get() = provider.trustStorePassword
    set(value) { provider.trustStorePassword = value }
  override var consumers: MutableList<IConsumerInfo>
    get() = provider.consumers
    set(value) { provider.consumers = value }
  var startProviderTask: Any?
    get() = provider.startProviderTask
    set(value) { provider.startProviderTask = value }
  var terminateProviderTask: Any?
    get() = provider.terminateProviderTask
    set(value) { provider.terminateProviderTask = value }
  var isDependencyForPactVerify: Boolean
    get() = provider.isDependencyForPactVerify
    set(value) { provider.isDependencyForPactVerify = value }

  open fun hasPactWith(consumer: String, closure: Closure<GradleConsumerInfo>): IConsumerInfo {
    val consumerInfo = project.objects.newInstance(GradleConsumerInfo::class.java, consumer)
    consumerInfo.name = consumer
    consumerInfo.verificationType = this.verificationType

    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = consumerInfo
    closure.call(consumerInfo)

    provider.consumers.add(consumerInfo)
    return consumerInfo
  }

  open fun hasPactsWith(consumersGroupName: String, closure: Closure<ConsumersGroup>): List<IConsumerInfo> {
    val consumersGroup = ConsumersGroup(consumersGroupName)

    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = consumersGroup
    closure.call(consumersGroup)

    return provider.setupConsumerListFromPactFiles(consumersGroup)
  }

  @JvmOverloads
  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
  open fun hasPactsFromPactBroker(
    options: Map<String, Any> = mapOf(),
    pactBrokerUrl: String,
    closure: Closure<IConsumerInfo>
  ): List<ConsumerInfo> {
    logger.warn { "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker" }
    val fromPactBroker = this.hasPactsFromPactBroker(options, pactBrokerUrl)
    fromPactBroker.forEach {
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure.delegate = it
      closure.call(it)
    }
    return fromPactBroker
  }

  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
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
  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
  open fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>,
    closure: Closure<IConsumerInfo>
  ): List<ConsumerInfo> {
    val fromPactBroker = this.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
    fromPactBroker.forEach {
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure.delegate = it
      closure.call(it)
    }
    return fromPactBroker
  }

  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
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

  fun hasPactsFromPactBrokerWithSelectorsV2(
    options: Map<String, Any?>,
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelectors>
  ): List<IConsumerInfo> {
    return try {
      provider.hasPactsFromPactBrokerWithSelectorsV2(options, pactBrokerUrl, selectors)
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

  open fun fromPactBroker(closure: Closure<PactBrokerConsumerConfig>) {
    brokerConfig = project.objects.newInstance(PactBrokerConsumerConfig::class.java)
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = brokerConfig
    closure.call(brokerConfig)

    val pending = brokerConfig.enablePending ?: false
    if (pending
      && (brokerConfig.providerTags.isNullOrEmpty() || brokerConfig.providerTags!!.any { it.trim().isEmpty() })
      && (brokerConfig.providerBranch.isNullOrBlank())
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
