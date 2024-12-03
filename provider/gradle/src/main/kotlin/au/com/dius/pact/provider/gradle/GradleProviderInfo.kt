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
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gradle.api.GradleScriptException
import org.gradle.api.model.ObjectFactory
import java.io.File
import java.net.URL
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

/**
 * Extends the provider info to be setup in a gradle build
 */
open class GradleProviderInfo @Inject constructor(
  override var name: String,
  private val objectFactory: ObjectFactory,
) : IProviderInfo {
  var providerVersion: Any? = null
  var providerTags: Any? = null
  var brokerConfig: PactBrokerConsumerConfig = PactBrokerConsumerConfig(objectFactory)
  val provider = ProviderInfo(name)
  var taskNames: List<String> = emptyList()

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

  override val transportEntry: CatalogueEntry?
    get() = CatalogueManager.lookupEntry("transport/$protocol")

  open fun hasPactWith(consumer: String, closure: Closure<GradleConsumerInfo>): IConsumerInfo {
    val consumerInfo = objectFactory.newInstance(GradleConsumerInfo::class.java, consumer)
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

  @JvmOverloads
  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
  fun hasPactsFromPactBroker(options: Map<String, Any> = mapOf(), pactBrokerUrl: String): List<ConsumerInfo> {
    return try {
      provider.hasPactsFromPactBroker(options, pactBrokerUrl)
    } catch (e: Exception) {
      val verifyTaskName = PACT_VERIFY.lowercase()
      if (taskNames.any { it.lowercase().contains(verifyTaskName) }) {
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

  @JvmOverloads
  @Deprecated(message = "hasPactsFromPactBroker has been deprecated in favor of fromPactBroker")
  fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any?> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>
  ): List<ConsumerInfo> {
    return try {
      provider.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, selectors)
    } catch (e: Exception) {
      val verifyTaskName = PACT_VERIFY.lowercase()
      if (taskNames.any { it.lowercase().contains(verifyTaskName) }) {
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
      val verifyTaskName = PACT_VERIFY.lowercase()
      if (taskNames.any { it.lowercase().contains(verifyTaskName) }) {
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
    brokerConfig = objectFactory.newInstance(PactBrokerConsumerConfig::class.java)
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
        |    withSelectors { latestTag('test') }
        |    enablePending = true
        |    providerTags = ['master']
        |}
        """.trimMargin("|"), null)
    }
  }
}
