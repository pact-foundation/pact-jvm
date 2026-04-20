package au.com.dius.pact.consumer.spock

import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.PactSpecVersion

/**
 * Configures a Spock feature method (or the entire spec class) as a Pact consumer test.
 *
 * When placed on the spec class, applies to all feature methods. When placed on a feature method,
 * overrides any class-level annotation for that feature.
 *
 * @param providerName Name of the provider to create the mock server for.
 * @param pactMethod Name of the method annotated with [@Pact] that builds the pact for this test.
 * @param pactVersion Pact spec version for the generated pact file (defaults to V4).
 * @param providerType Whether the provider is HTTP (SYNCH), message (ASYNCH), or synchronous message (SYNCH_MESSAGE).
 * @param port Fixed port for the mock server (empty = random port).
 * @param hostInterface Host interface the mock server listens on (empty = localhost).
 * @param https Whether to start the mock server with HTTPS.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PactTestFor(
  val providerName: String = "",
  val hostInterface: String = "",
  val port: String = "",
  val pactVersion: PactSpecVersion = PactSpecVersion.UNSPECIFIED,
  val pactMethod: String = "",
  val providerType: ProviderType = ProviderType.UNSPECIFIED,
  val https: Boolean = false,
  val keyStorePath: String = "",
  val keyStoreAlias: String = "",
  val keyStorePassword: String = "",
  val privateKeyPassword: String = "",
  @Deprecated("Use MockServerConfig annotation instead")
  val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default
)
