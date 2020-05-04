package au.com.dius.pact.provider

import org.apache.commons.lang3.NotImplementedException

class TestVerifyResponseByInvokingProviderMethodsClass {
  @PactVerifyProvider('verifyResponseByInvokingProviderMethods Test Message')
  String method() { throw new NotImplementedException('Boom!') }
}
