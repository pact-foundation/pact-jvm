package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.PactSpecVersion
import java.lang.annotation.Inherited

/**
 * Main test annotation for a JUnit 5 test
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class PactTestFor(
  /**
   * Providers name. This will be recorded in the pact file
   */
  val providerName: String = "",

  /**
   * Host interface to use for the mock server. Only used for synchronous provider tests and defaults to the
   * loopback adapter (127.0.0.1).
   */
  val hostInterface: String = "",

  /**
   * Port number to bind to. Only used for synchronous provider tests and defaults to 0, which causes a random free port to be chosen.
   */
  val port: String = "",

  /**
   * Pact specification version to support. Will default to V3.
   */
  val pactVersion: PactSpecVersion = PactSpecVersion.UNSPECIFIED,

  /**
   * Test method that provides the Pact to use for the test. Default behaviour is to use the first one found.
   */
  val pactMethod: String = "",

  /**
   * Type of provider (synchronous HTTP or asynchronous messages)
   */
  val providerType: ProviderType = ProviderType.UNSPECIFIED,

  /**
   * If HTTPS should be used. If enabled, a mock server with a self-signed cert will be started.
   */
  val https: Boolean = false,

  /**
   * The type of mock server implementation to use. The default is to use the Java server for HTTP and the KTor
   * server for HTTPS
   */
  val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default,

  /**
   * Test methods that provides the Pacts to use for the test. This allows multiple providers to be
   * used in the same test.
   */
  val pactMethods: Array<String> = []
)