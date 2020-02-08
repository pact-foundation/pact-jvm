package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import java.io.File
import java.net.URL

/**
 * Provider Info
 */
open class Provider @JvmOverloads constructor(
  override var name: String = "provider",
  override var protocol: String = "http",
  override var host: Any? = "localhost",
  override var port: Any? = 8080,
  override var path: String = "/",
  override var startProviderTask: Any? = null,
  override var terminateProviderTask: Any? = null,
  override var requestFilter: Any? = null,
  override var stateChangeRequestFilter: Any? = null,
  override var createClient: Any? = null,
  override var insecure: Boolean = false,
  override var trustStore: File? = null,
  override var trustStorePassword: String? = "changeit",
  override var stateChangeUrl: URL? = null,
  override var stateChangeUsesBody: Boolean = true,
  override var stateChangeTeardown: Boolean = false,
  override var isDependencyForPactVerify: Boolean = true,
  override var verificationType: PactVerification? = PactVerification.REQUEST_RESPONSE,
  override var packagesToScan: List<String> = emptyList(),
  override var consumers: MutableList<IConsumerInfo> = mutableListOf(),
  var pactFileDirectory: File?,
  var pactBrokerUrl: URL?,
  var pactBroker: PactBroker?,
  var pactFileDirectories: List<File>? = emptyList()
) : ProviderInfo(name, protocol, host, port, path, startProviderTask, terminateProviderTask, requestFilter,
  stateChangeRequestFilter, createClient, insecure, trustStore, trustStorePassword, stateChangeUrl,
  stateChangeUsesBody, stateChangeTeardown, isDependencyForPactVerify, verificationType, packagesToScan, consumers) {

  constructor() : this(pactFileDirectory = null, pactBrokerUrl = null, pactBroker = null)
}
