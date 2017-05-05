package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.PactVerificationResult;

import java.util.function.BiConsumer;

public class TestFailureProviderRule extends PactProviderRuleMk2 {
  private BiConsumer<PactVerificationResult, Throwable> verificationResultConsumer;

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

  public void validateResultWith(BiConsumer<PactVerificationResult, Throwable> function) {
    this.verificationResultConsumer = function;
  }
}
