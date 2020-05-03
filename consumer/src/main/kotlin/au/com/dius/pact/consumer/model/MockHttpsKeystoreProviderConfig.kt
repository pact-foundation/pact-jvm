package au.com.dius.pact.consumer.model

import au.com.dius.pact.core.model.PactSpecVersion
import java.io.File

/**
 * Mock Provider configuration for HTTPS using a keystore
 */
class MockHttpsKeystoreProviderConfig(
  val keystore: String,
  val password: String,
  override val hostname: String = LOCALHOST,
  override val port: Int = 0,
  override val pactVersion: PactSpecVersion = PactSpecVersion.V3,
  override val scheme: String = "https"
) : MockProviderConfig(hostname, port, pactVersion, scheme) {

  companion object {

    /**
     * Creates instance of config
     * @param hostname Name of the host to mock
     * @param port Port the mock service should listen on
     * @param keystore Full path (including file name) of keystore to use.
     * @param password Keystore password
     * @param pactVersion Version of {@link PactSpecVersion}
     * @return
     */
    @JvmOverloads
    @JvmStatic
    fun httpsKeystoreConfig(
      hostname: String = LOCALHOST,
      port: Int = 0,
      keystore: String,
      password: String,
      pactVersion: PactSpecVersion = PactSpecVersion.V2
    ): MockProviderConfig {
      val keystoreFile = File(keystore)
      if (!keystoreFile.isFile) {
        throw IllegalArgumentException(
          "Keystore path/file '$keystore' is not valid! It should be formatted similar to `/path/to/keystore.jks'")
      }
      return MockHttpsKeystoreProviderConfig(keystore, password, hostname, port, pactVersion)
    }
  }
}
