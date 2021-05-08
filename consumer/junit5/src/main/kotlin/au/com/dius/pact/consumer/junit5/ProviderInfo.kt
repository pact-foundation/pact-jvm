package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser

/**
 * The type of provider (synchronous or asynchronous)
 */
enum class ProviderType {
  /**
   * Synchronous provider (HTTP)
   */
  SYNCH,
  /**
   * Asynchronous provider (Messages)
   */
  ASYNCH,
  /**
   * Unspecified, will default to synchronous
   */
  UNSPECIFIED
}

data class ProviderInfo @JvmOverloads constructor(
  val providerName: String = "",
  val hostInterface: String = "",
  val port: String = "",
  val pactVersion: PactSpecVersion? = null,
  val providerType: ProviderType? = null,
  val https: Boolean = false,
  val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default
) {
  fun mockServerConfig() = if (https) {
    MockHttpsProviderConfig.httpsConfig(
      if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
      if (port.isEmpty()) 0 else port.toInt(),
      pactVersion ?: PactSpecVersion.V3,
      mockServerImplementation
    )
  } else {
    MockProviderConfig.httpConfig(
      hostInterface.ifEmpty { MockProviderConfig.LOCALHOST },
      if (port.isEmpty()) 0 else port.toInt(),
      pactVersion ?: PactSpecVersion.V3,
      mockServerImplementation
    )
  }

  fun merge(other: ProviderInfo): ProviderInfo {
    return copy(providerName = providerName.ifEmpty { other.providerName },
      hostInterface = hostInterface.ifEmpty { other.hostInterface },
      port = port.ifEmpty { other.port },
      pactVersion = pactVersion ?: other.pactVersion,
      providerType = providerType ?: other.providerType,
      https = https || other.https,
      mockServerImplementation = mockServerImplementation.merge(other.mockServerImplementation)
    )
  }

  companion object {
    fun fromAnnotation(annotation: PactTestFor): ProviderInfo =
      ProviderInfo(ExpressionParser.parseExpression(annotation.providerName, DataType.STRING)?.toString()
        ?: annotation.providerName,
        annotation.hostInterface, annotation.port,
        when (annotation.pactVersion) {
          PactSpecVersion.UNSPECIFIED -> null
          else -> annotation.pactVersion
        },
        when (annotation.providerType) {
          ProviderType.UNSPECIFIED -> null
          else -> annotation.providerType
        }, annotation.https, annotation.mockServerImplementation)
  }
}