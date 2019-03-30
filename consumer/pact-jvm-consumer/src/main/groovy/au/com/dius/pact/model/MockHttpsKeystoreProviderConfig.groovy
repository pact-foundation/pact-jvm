package au.com.dius.pact.model

import au.com.dius.pact.core.model.PactSpecVersion
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Mock Provider configuration for HTTPS using a keystore
 */
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true)
@CompileStatic
class MockHttpsKeystoreProviderConfig extends MockProviderConfig {

  private final String keystore
  private final String password

  MockHttpsKeystoreProviderConfig(String hostname, int port, String keystore, String password,
                                  PactSpecVersion pactVersion) {
      super(hostname, port, pactVersion, 'https')
      this.keystore = keystore
      this.password = password
  }

  /**
   * Creates instance of config
   * @param hostname Name of the host to mock
   * @param port Port the mock service should listen on
   * @param keystore Full path (including file name) of keystore to use.
   * @param password Keystore password
   * @param pactVersion Version of {@link PactSpecVersion}
   * @return
   */
  static MockProviderConfig httpsKeystoreConfig(String hostname = 'localhost',
                                                int port = 0,
                                                final String keystore,
                                                final String password,
                                                PactSpecVersion pactVersion = PactSpecVersion.V2) {
    File keystoreFile = new File(keystore)
    if (!keystoreFile.isFile()) {
      throw new IllegalArgumentException(
        "Keystore path/file '$keystore' is not valid! It should be formatted similar to `/path/to/keystore.jks'")
    }
    new MockHttpsKeystoreProviderConfig(hostname, port, keystore, password, pactVersion)
  }

  /**
   * @return The String value of the keystore path and file.
   * Example: '/path/to/keystore.jks'
   */
  String getKeystore() {
    keystore
  }

  /**
   * @return The password for the keystore
   */
  String getKeystorePassword() {
    password
  }

}
