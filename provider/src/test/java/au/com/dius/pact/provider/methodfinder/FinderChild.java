package au.com.dius.pact.provider.methodfinder;

import au.com.dius.pact.provider.PactVerifyProvider;

public class FinderChild extends FinderParent implements FinderInterface {
  @PactVerifyProvider("child message")
  private String fromChild() {
    return "{\"from\":\"child\"}";
  }

  @Override
  @PactVerifyProvider("overridden message")
  public String overridden() {
    return "{\"from\":\"child\"}";
  }

  public String notAnnotated() {
    return "{\"from\":\"none\"}";
  }
}
