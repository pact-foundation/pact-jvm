package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.ProviderState;
import kotlin.Pair;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;

public class RunStateChanges extends Statement {
  private final Statement next;
  private final Object target;
  private final List<Pair<FrameworkMethod, State>> methods;
  private final ProviderState providerState;

  public RunStateChanges(Statement next, List<Pair<FrameworkMethod, State>> methods, Object target, ProviderState providerState) {
    this.next = next;
    this.methods = methods;
    this.target = target;
    this.providerState = providerState;
  }

  @Override
  public void evaluate() throws Throwable {
    invokeStateChangeMethods(StateChangeAction.SETUP);
    next.evaluate();
    invokeStateChangeMethods(StateChangeAction.TEARDOWN);
  }

  private void invokeStateChangeMethods(StateChangeAction action) throws Throwable {
    for (Pair<FrameworkMethod, State> pair : methods) {
      if (pair.getSecond().action() == action) {
        if (pair.getFirst().getMethod().getParameterCount() == 1) {
          pair.getFirst().invokeExplosively(target, providerState.getParams());
        } else {
          pair.getFirst().invokeExplosively(target);
        }
      }
    }
  }
}
