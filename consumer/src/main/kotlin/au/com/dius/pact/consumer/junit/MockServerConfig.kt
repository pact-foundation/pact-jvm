package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.model.MockServerImplementation
import java.lang.annotation.Inherited

/**
 * Key/Value pair for a Transport Configuration Entry
 */
annotation class TransportConfigurationEntry(val key: String, val value: String)

/**
 * Annotation to configure the mock server for a consumer test
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@Suppress("LongParameterList")
@JvmRepeatable(MockServers::class)
annotation class MockServerConfig(
  /**
   * The type of mock server implementation to use. The default is to use the Java server for HTTP and the KTor
   * server for HTTPS.
   */
  val implementation: MockServerImplementation = MockServerImplementation.Default,

  /**
   * Mock server registry entry. Required for mock servers provided by plugins.
   */
  val registryEntry: String = "",

  /**
   * Host interface to use for the mock server. Defaults to the loopback adapter (127.0.0.1).
   */
  val hostInterface: String = "",

  /**
   * Port number to bind to. Defaults to 0, which causes a random free port to be chosen.
   */
  val port: String = "",

  /**
   * If TLS should be used. If enabled, a mock server with a self-signed cert will be started (if the mock server
   * supports TLS).
   */
  val tls: Boolean = false,

  /**
   * If an external keystore file should be provided to the mockServer (for TLS).
   */
  val keyStorePath: String = "",

  /**
   * The alias name of the certificate that should be used (for TLS).
   */
  val keyStoreAlias: String = "",

  /**
   * The password for the keystore (for TLS).
   */
  val keyStorePassword: String = "",

  /**
   * The password for the private key entry in the keystore (for TLS).
   */
  val privateKeyPassword: String = "",

  /**
   * Provider name this mock server is associated with. This is only needed when there are multiple for the same test
   */
  val providerName: String = "",

  /**
   * Configuration required for the transport used. This is mostly used where plugins provide things like mock servers
   * and require additional configuration.
   */
  val transportConfig: Array<TransportConfigurationEntry> = []
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class MockServers(val value: Array<MockServerConfig>)
