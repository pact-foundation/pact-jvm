package au.com.dius.pact.provider

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.support.Utils
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import java.io.File
import java.net.URL

/**
 * Provider Info Config
 */
open class ProviderInfo @JvmOverloads constructor (
  override var name: String = "provider",
  override var protocol: String = "http",
  override var host: Any? = "localhost",
  override var port: Any? = 8080,
  override var path: String = "/",
  open var startProviderTask: Any? = null,
  open var terminateProviderTask: Any? = null,
  override var requestFilter: Any? = null,
  override var stateChangeRequestFilter: Any? = null,
  override var createClient: Any? = null,
  override var insecure: Boolean = false,
  override var trustStore: File? = null,
  override var trustStorePassword: String? = "changeit",
  override var stateChangeUrl: URL? = null,
  override var stateChangeUsesBody: Boolean = true,
  override var stateChangeTeardown: Boolean = false,
  open var isDependencyForPactVerify: Boolean = true,
  override var verificationType: PactVerification? = PactVerification.REQUEST_RESPONSE,
  override var packagesToScan: List<String> = emptyList(),
  open var consumers: MutableList<IConsumerInfo> = mutableListOf()
) : IProviderInfo {

  override fun hashCode() = HashCodeBuilder()
    .append(name).append(protocol).append(host).append(port).append(path).toHashCode()

  override fun toString() = ToStringBuilder.reflectionToString(this)

  open fun hasPactWith(consumer: String, closure: ConsumerInfo.() -> Unit): ConsumerInfo {
    val consumerInfo = ConsumerInfo(consumer)
    consumers.add(consumerInfo)
    consumerInfo.closure()
    return consumerInfo
  }

  open fun hasPactsWith(consumersGroupName: String, closure: ConsumersGroup.() -> Unit): List<IConsumerInfo> {
    val consumersGroup = ConsumersGroup(consumersGroupName)
    consumersGroup.closure()

    return setupConsumerListFromPactFiles(consumersGroup)
  }

  @JvmOverloads
  open fun hasPactsFromPactBroker(options: Map<String, Any> = mapOf(), pactBrokerUrl: String) =
    hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl, emptyList())

  @JvmOverloads
  @Suppress("TooGenericExceptionThrown")
  open fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>
  ): List<ConsumerInfo> {
    val enablePending = Utils.lookupInMap(options, "enablePending", Boolean::class.java, false)
    if (enablePending && (!options.containsKey("providerTags") || options["providerTags"] !is List<*>)) {
      throw RuntimeException("No providerTags: To use the pending pacts feature, you need to provide the list of " +
        "provider names for the provider application version that will be published with the verification results")
    }
    val providerTags = if (enablePending) {
      options["providerTags"] as List<String>
    } else {
      emptyList()
    }
    val client = pactBrokerClient(pactBrokerUrl, options)
    val consumersFromBroker = client.fetchConsumersWithSelectors(name, selectors, providerTags, enablePending)
      .map { results -> results.map { ConsumerInfo.from(it) }
    }
    return when (consumersFromBroker) {
      is Ok<List<ConsumerInfo>> -> {
        val list = consumersFromBroker.value
        consumers.addAll(list)
        list
      }
      is Err<Exception> -> {
        throw RuntimeException("Call to fetch pacts from Pact Broker failed with an exception",
          consumersFromBroker.error)
      }
    }
  }

  open fun pactBrokerClient(pactBrokerUrl: String, options: Map<String, Any>) =
    PactBrokerClient(pactBrokerUrl, options)

  @Suppress("TooGenericExceptionThrown")
  open fun setupConsumerListFromPactFiles(consumersGroup: ConsumersGroup): MutableList<IConsumerInfo> {
    val pactFileDirectory = consumersGroup.pactFileLocation ?: return mutableListOf()
    if (!pactFileDirectory.exists() || !pactFileDirectory.canRead()) {
      throw RuntimeException("pactFileDirectory ${pactFileDirectory.absolutePath} does not exist or is not readable")
    }

    pactFileDirectory.walkBottomUp().forEach { file ->
      if (file.isFile && consumersGroup.include.matches(file.name)) {
        val name = DefaultPactReader.loadPact(file).consumer.name
        consumers.add(ConsumerInfo(name,
          consumersGroup.stateChange,
          consumersGroup.stateChangeUsesBody,
          emptyList(),
          null,
          FileSource<Interaction>(file)
        ))
      }
    }

    return consumers
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ProviderInfo

    if (name != other.name) return false
    if (protocol != other.protocol) return false
    if (host != other.host) return false
    if (port != other.port) return false
    if (path != other.path) return false

    return true
  }
}
