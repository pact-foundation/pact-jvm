package au.com.dius.pact.consumer.model

import au.com.dius.pact.core.model.PactSpecVersion
import io.netty.handler.ssl.util.SelfSignedCertificate
import java.security.KeyStore

/**
 * Mock Provider configuration for HTTPS
 */
class MockHttpsProviderConfig @JvmOverloads constructor(
  val httpsCertificate: SelfSignedCertificate? = null,
  override val hostname: String = LOCALHOST,
  override val port: Int = 0,
  override val pactVersion: PactSpecVersion = PactSpecVersion.V3,
  val keyStore: KeyStore? = null,
  val keyStoreAlias: String = "alias",
  val keystorePassword: String = "changeme",
  val privateKeyPassword: String = "changeme",
  override val mockServerImplementation: MockServerImplementation = MockServerImplementation.JavaHttpServer
) : MockProviderConfig(hostname, port, pactVersion, "https", mockServerImplementation) {

  companion object {
    @JvmStatic
    @JvmOverloads
    fun httpsConfig(hostname: String = LOCALHOST, port: Int = 0, pactVersion: PactSpecVersion = PactSpecVersion.V3) =
      MockHttpsProviderConfig(SelfSignedCertificate(), hostname, port, pactVersion)
  }
}
