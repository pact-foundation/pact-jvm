package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.model.MockServerImplementation
import java.lang.annotation.Inherited

/**
 * Annotation to configure the mock server for a consumer test
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@Suppress("LongParameterList")
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
  val privateKeyPassword: String = ""
)
