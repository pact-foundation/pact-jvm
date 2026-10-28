package au.com.dius.pact.provider.methodfinder;

import au.com.dius.pact.provider.PactVerifyProvider;

public class FinderParent {
  @PactVerifyProvider("parent message")
  String fromParent() {
    return "{\"from\":\"parent\"}";
  }

  @PactVerifyProvider("overridden message")
  public String overridden() {
    return "{\"from\":\"parent\"}";
  }
}
