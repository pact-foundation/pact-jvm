package au.com.dius.pact.model

import groovy.transform.Canonical
import org.apache.commons.lang.math.RandomUtils

/**
 * Configuration of the Pact Mock Server.
 *
 * By default this class will setup the configuration for a http mock server running on
 * local host and a random port
 */
@Canonical
@SuppressWarnings('FactoryMethodName')
class MockProviderConfig {
  private static final String LOCALHOST = 'localhost'
  private static final String HTTP = 'http'

  String hostname = LOCALHOST
  int port = 0
  PactSpecVersion pactVersion = PactSpecVersion.V2
  String scheme = HTTP

  String url() {
    "$scheme://$hostname:$port"
  }

  static MockProviderConfig httpConfig(String hostname = LOCALHOST, int port = 0,
                                       PactSpecVersion pactVersion = PactSpecVersion.V2) {
    new MockProviderConfig(hostname, port, pactVersion, HTTP)
  }

  static MockProviderConfig createDefault() {
    createDefault(LOCALHOST, PactSpecVersion.V2)
  }

  static MockProviderConfig createDefault(PactSpecVersion pactVersion) {
    createDefault(LOCALHOST, pactVersion)
  }

  /**
   * @deprecated Set the port to zero to get the OS to assign a random port
   */
  @Deprecated
  @SuppressWarnings('FieldName')
  public static final int portLowerBound = 20000
  /**
   * @deprecated Set the port to zero to get the OS to assign a random port
   */
  @Deprecated
  @SuppressWarnings('FieldName')
  public static final int portUpperBound = 40000

  static MockProviderConfig createDefault(String host, PactSpecVersion pactVersion) {
    new MockProviderConfig(host, randomPort(portLowerBound, portUpperBound), pactVersion)
  }

  /**
   * @deprecated Set the port to zero to get the OS to assign a random port
   */
  @Deprecated
  static MockProviderConfig create(int lower, int upper, PactSpecVersion pactVersion) {
    new MockProviderConfig(LOCALHOST, randomPort(lower, upper), pactVersion)
  }

  /**
   * @deprecated Set the port to zero to get the OS to assign a random port
   */
  @Deprecated
  static MockProviderConfig create(String hostname, int lower, int upper, PactSpecVersion pactVersion) {
    new MockProviderConfig(hostname, randomPort(lower, upper), pactVersion)
  }

  /**
   * @deprecated Set the port to zero to get the OS to assign a random port
   */
  @Deprecated
  static int randomPort(int lower, int upper) {
    Integer port = null
    int count = 0
    while (port == null && count < 20) {
      int randomPort = RandomUtils.nextInt(upper - lower) + lower
      if (portAvailable(randomPort)) {
        port = randomPort
      }
      count++
    }

    if (port == null) {
      port = 0
    }

    port
  }

  private static boolean portAvailable(int p) {
    ServerSocket socket = null
    try {
      socket = new ServerSocket(p)
      true
    } catch (IOException ignored) {
      false
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch (ignored) { }
      }
    }
  }
}
