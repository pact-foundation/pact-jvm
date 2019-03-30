package au.com.dius.pact.consumer.model

import au.com.dius.pact.core.model.PactSpecVersion
import io.netty.handler.ssl.util.SelfSignedCertificate

/**
 * Mock Provider configuration for HTTPS
 */
class MockHttpsProviderConfig(
  val httpsCertificate: SelfSignedCertificate,
  override val hostname: String = LOCALHOST,
  override val port: Int = 0,
  override val pactVersion: PactSpecVersion = PactSpecVersion.V3,
  override val scheme: String = HTTP
) : MockProviderConfig(hostname, port, pactVersion, scheme) {

  companion object {
    @JvmStatic
    @JvmOverloads
    fun httpsConfig(hostname: String = LOCALHOST, port: Int = 0, pactVersion: PactSpecVersion = PactSpecVersion.V3) =
      MockHttpsProviderConfig(SelfSignedCertificate(), hostname, port, pactVersion)
  }
}
