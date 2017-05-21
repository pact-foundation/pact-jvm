package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.PactVerificationResult;

public class TestFailureProviderRule extends PactProviderRuleMk2 {

  public interface BiConsumer {
    void accept(PactVerificationResult result, Throwable t);
  }

  private BiConsumer verificationResultConsumer;

  public TestFailureProviderRule(String provider, Object target) {
    super(provider, target);
  }

  @Override
  protected void validateResult(PactVerificationResult result, PactVerification pactVerification) throws Throwable {
    try {
      super.validateResult(result, pactVerification);
      if (verificationResultConsumer != null) {
        verificationResultConsumer.accept(result, null);
      }
    } catch (Throwable throwable) {
      if (verificationResultConsumer != null) {
        verificationResultConsumer.accept(result, throwable);
      } else {
        throw throwable;
      }
    }
  }

  public void validateResultWith(BiConsumer function) {
    this.verificationResultConsumer = function;
  }
}
