package au.com.dius.pact.provider.methodfinder;

import au.com.dius.pact.provider.PactVerifyProvider;

public interface FinderInterface {
  @PactVerifyProvider("interface message")
  default String fromInterface() {
    return "{\"from\":\"interface\"}";
  }
}
