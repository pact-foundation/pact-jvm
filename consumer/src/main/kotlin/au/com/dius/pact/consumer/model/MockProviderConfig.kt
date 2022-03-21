package au.com.dius.pact.consumer.model

import au.com.dius.pact.consumer.junit.MockServerConfig
import au.com.dius.pact.core.model.PactSpecVersion
import java.net.InetSocketAddress
import java.util.Optional

/**
 * Mock Server Implementation
 */
enum class MockServerImplementation {
  /**
   * Uses the Java HTTP server that comes with the JDK
   */
  JavaHttpServer,

  /**
   * Uses the KTor server framework
   */
  KTorServer,

  /**
   * Use the Java server for HTTP and the KTor server for HTTPS
   */
  Default,

  /**
   * Mock server provided by a plugin
   */
  Plugin;

  fun merge(implementation: MockServerImplementation) = if (this == Default) {
    implementation
  } else {
    this
  }
}

/**
 * Configuration of the Pact Mock Server.
 *
 * By default this class will setup the configuration for a http mock server running on
 * local host and a random port
 */
open class MockProviderConfig @JvmOverloads constructor (
  open val hostname: String = LOCALHOST,
  open val port: Int = 0,
  open val pactVersion: PactSpecVersion = PactSpecVersion.V3,
  open val scheme: String = HTTP,
  open val mockServerImplementation: MockServerImplementation = MockServerImplementation.JavaHttpServer,
  open val addCloseHeader: Boolean = false,
  open val transportRegistryEntry: String = ""
) {

  fun url() = "$scheme://$hostname:$port"

  fun address() = InetSocketAddress(hostname, port)

  /**
   * Create the mock server configuration required to pass to a plugin
   */
  fun toPluginMockServerConfig(): io.pact.plugins.jvm.core.MockServerConfig {
    return io.pact.plugins.jvm.core.MockServerConfig(
      hostname, port, scheme == "https"
    )
  }

  companion object {
    const val LOCALHOST = "127.0.0.1"
    const val HTTP = "http"

    @JvmStatic
    @JvmOverloads
    fun httpConfig(
      hostname: String = LOCALHOST,
      port: Int = 0,
      pactVersion: PactSpecVersion = PactSpecVersion.V3,
      implementation: MockServerImplementation = MockServerImplementation.JavaHttpServer,
      addCloseHeader: Boolean = System.getProperty("pact.mockserver.addCloseHeader") == "true"
    ) = MockProviderConfig(hostname, port, pactVersion, HTTP,
      implementation.merge(MockServerImplementation.JavaHttpServer), addCloseHeader)

    @JvmStatic
    fun createDefault() = createDefault(LOCALHOST, PactSpecVersion.V3)

    @JvmStatic
    fun createDefault(pactVersion: PactSpecVersion) = createDefault(LOCALHOST, pactVersion)

    @JvmStatic
    fun createDefault(host: String, pactVersion: PactSpecVersion) =
      MockProviderConfig(hostname = host, pactVersion = pactVersion,
        addCloseHeader = System.getProperty("pact.mockserver.addCloseHeader") == "true")

    fun fromMockServerAnnotation(config: Optional<MockServerConfig>): MockProviderConfig? {
      return if (config.isPresent) {
        val annotation = config.get()
        MockProviderConfig(
          annotation.hostInterface.ifEmpty { LOCALHOST },
          if (annotation.port.isEmpty()) 0 else annotation.port.toInt(),
          PactSpecVersion.V4,
          if (annotation.tls) "tls" else HTTP,
          annotation.implementation,
          System.getProperty("pact.mockserver.addCloseHeader") == "true",
          annotation.registryEntry
        )
      } else {
        null
      }
    }
  }
}
