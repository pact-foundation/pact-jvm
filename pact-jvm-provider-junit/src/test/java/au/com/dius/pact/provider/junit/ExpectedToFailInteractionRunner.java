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
    final OrderedMap<Description, Boolean> failed = ListOrderedMap.listOrderedMap(new HashMap<Description, Boolean>());
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
        failed.put(description, false);
        notifier.fireTestStarted(description);
      }

      @Override
      public void testFailure(Failure failure) throws Exception {
        failed.put(failed.lastKey(), true);
      }

      @Override
      public void testFinished(Description description) throws Exception {
        if (!failed.get(description)) {
          notifier.fireTestFailure(new Failure(description, new Exception("Expected the test to fail but it did not")));
        }
        notifier.fireTestFinished(description);
      }
    });
    baseRunner.run(testNotifier);
  }
}
