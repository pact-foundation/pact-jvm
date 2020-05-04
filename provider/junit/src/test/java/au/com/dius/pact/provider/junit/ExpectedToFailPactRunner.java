package au.com.dius.pact.provider.junit;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class ExpectedToFailPactRunner extends PactRunner {
  public ExpectedToFailPactRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
  }

  @Override
  protected void runChild(InteractionRunner interaction, RunNotifier notifier) {
    try {
      new ExpectedToFailInteractionRunner(interaction).run(notifier);
    } catch (InitializationError initializationError) {
      throw new RuntimeException(initializationError);
    }
  }
}
