package au.com.dius.pact.provider

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.support.Utils
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import mu.KLogging
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import java.io.File
import java.net.URL

/**
 * Provider Info Config
 */
@Suppress("LongParameterList")
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
  override var consumers: MutableList<IConsumerInfo> = mutableListOf()
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

  @Deprecated("Use version that takes list of ConsumerVersionSelectors",
    replaceWith = ReplaceWith("hasPactsFromPactBrokerWithSelectorsV2"))
  open fun hasPactsFromPactBrokerWithSelectors(
    options: Map<String, Any?> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>
  ) = hasPactsFromPactBrokerWithSelectorsV2(options, pactBrokerUrl, selectors.map { it.toSelector() })

  /**
   * Fetches all pacts from the broker that match the given selectors.
   *
   * Options:
   * * enablePending (boolean) - Enables pending Pact support
   * * providerTags (List<String>) - List of provider tag names
   * * providerBranch (String) - Provider branch
   * * includeWipPactsSince (String) - Date to include Pacts as WIP
   */
  @JvmOverloads
  open fun hasPactsFromPactBrokerWithSelectorsV2(
    options: Map<String, Any?> = mapOf(),
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelectors>
  ): List<ConsumerInfo> {
    val enablePending = Utils.lookupInMap(options, "enablePending", Boolean::class.java, false)
    val providerTags = if (enablePending) {
      options["providerTags"] as List<String>?
    } else {
      emptyList()
    }

    val providerBranch = Utils.lookupInMap(options, "providerBranch", String::class.java, "")
    val includePactsSince = Utils.lookupInMap(options, "includeWipPactsSince", String::class.java, "")
    val pactBrokerOptions = PactBrokerOptions(enablePending, providerTags.orEmpty(), providerBranch,
            includePactsSince, false, PactBrokerOptions.parseAuthSettings(options))

    return hasPactsFromPactBrokerWithSelectorsV2(pactBrokerUrl, selectors, pactBrokerOptions)
  }

  @Suppress("TooGenericExceptionThrown")
  @Deprecated("Use version that takes list of ConsumerVersionSelectors",
    replaceWith = ReplaceWith("hasPactsFromPactBrokerWithSelectorsV2"))
  open fun hasPactsFromPactBrokerWithSelectors(
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelector>,
    options: PactBrokerOptions
  ) = hasPactsFromPactBrokerWithSelectorsV2(pactBrokerUrl, selectors.map { it.toSelector() }, options)

  @Suppress("TooGenericExceptionThrown")
  open fun hasPactsFromPactBrokerWithSelectorsV2(
    pactBrokerUrl: String,
    selectors: List<ConsumerVersionSelectors>,
    options: PactBrokerOptions
  ): List<ConsumerInfo> {
    if (options.enablePending && options.providerTags.isEmpty() && options.providerBranch.isNullOrBlank() ) {
      throw RuntimeException("No providerTags or providerBranch: To use the pending pacts feature, you need to" +
        " provide the list of provider names for the provider application version that will be published with the" +
        " verification results")
    }
    val client = pactBrokerClient(pactBrokerUrl, options)
    val consumersFromBroker = client.fetchConsumersWithSelectorsV2(name, selectors, options.providerTags,
      options.providerBranch, options.enablePending, options.includeWipPactsSince)
      .map { results -> results.map { ConsumerInfo.from(it) } }

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

  open fun pactBrokerClient(pactBrokerUrl: String, options: PactBrokerOptions): PactBrokerClient {
    return PactBrokerClient(
      pactBrokerUrl,
      options.toMutableMap(),
      PactBrokerClientConfig(insecureTLS = options.insecureTLS)
    )
  }

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
          FileSource(file)
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

  companion object : KLogging()
}
