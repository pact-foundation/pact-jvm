package au.com.dius.pact.provider.junit;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.HashMap;

public class ExpectedToFailInteractionRunner extends Runner {
  private final InteractionRunner baseRunner;

  public ExpectedToFailInteractionRunner(InteractionRunner baseRunner) throws InitializationError {
    this.baseRunner = baseRunner;
  }

  @Override
  public Description getDescription() {
    return baseRunner.getDescription();
  }

  @Override
  public void run(final RunNotifier notifier) {
    RunNotifier testNotifier = new RunNotifier();
    testNotifier.addListener(new RunListener() {
      @Override
      public void testRunStarted(Description description) throws Exception {
        notifier.fireTestRunStarted(description);
      }

      @Override
      public void testRunFinished(Result result) throws Exception {
        notifier.fireTestRunFinished(result);
      }

      @Override
      public void testStarted(Description description) throws Exception {
        notifier.fireTestStarted(description);
      }

      @Override
      public void testFailure(Failure failure) throws Exception {
        notifier.fireTestFinished(failure.getDescription());
      }

      @Override
      public void testIgnored(Description description) throws Exception {
        notifier.fireTestFailure(new Failure(description, new Exception("Expected the test to fail but it did not")));
      }

      @Override
      public void testFinished(Description description) throws Exception {
        notifier.fireTestFailure(new Failure(description, new Exception("Expected the test to fail but it did not")));
      }
    });
    baseRunner.run(testNotifier);
  }
}
