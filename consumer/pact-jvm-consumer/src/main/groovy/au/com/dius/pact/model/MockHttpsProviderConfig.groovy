package au.com.dius.pact.model

import au.com.dius.pact.core.model.PactSpecVersion
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.netty.handler.ssl.util.SelfSignedCertificate

/**
 * Mock Provider configuration for HTTPS
 */
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true)
@CompileStatic
class MockHttpsProviderConfig extends MockProviderConfig {

  SelfSignedCertificate httpsCertificate

  MockHttpsProviderConfig(SelfSignedCertificate httpsCertificate) {
    super()
    this.httpsCertificate = httpsCertificate
  }

  MockHttpsProviderConfig(SelfSignedCertificate httpsCertificate,
                          String hostname, int port, PactSpecVersion pactVersion) {
    super(hostname, port, pactVersion, 'https')
    this.httpsCertificate = httpsCertificate
  }

  static MockProviderConfig httpsConfig(String hostname = 'localhost', int port = 0,
                                        PactSpecVersion pactVersion = PactSpecVersion.V2) {
    SelfSignedCertificate httpsCertificate = new SelfSignedCertificate()
    new MockHttpsProviderConfig(httpsCertificate, hostname, port, pactVersion)
  }

}
