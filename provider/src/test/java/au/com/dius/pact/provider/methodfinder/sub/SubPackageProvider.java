package au.com.dius.pact.provider.methodfinder.sub;

import au.com.dius.pact.provider.PactVerifyProvider;

public class SubPackageProvider {

  @PactVerifyProvider("sub package message")
  public String message() {
    return "{\"from\":\"sub\"}";
  }
}
