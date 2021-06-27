package au.com.dius.pact.consumer.model

import au.com.dius.pact.core.model.PactSpecVersion
import java.net.InetSocketAddress

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
  Default;

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
  open val addCloseHeader: Boolean = false
) {

  fun url() = "$scheme://$hostname:$port"

  fun address() = InetSocketAddress(hostname, port)

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
  }
}
