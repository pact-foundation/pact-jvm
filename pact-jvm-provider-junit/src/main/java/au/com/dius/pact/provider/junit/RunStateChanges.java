package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.ProviderState;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;

public class RunStateChanges extends Statement {
    private final Statement next;
    private final Object target;
    private final List<FrameworkMethod> methods;
    private final ProviderState providerState;

    public RunStateChanges(Statement next, List<FrameworkMethod> methods, Object target, ProviderState providerState) {
        this.next = next;
        this.methods = methods;
        this.target = target;
        this.providerState = providerState;
    }

    @Override
    public void evaluate() throws Throwable {
        for (FrameworkMethod method : methods) {
          if (method.getMethod().getParameterTypes().length == 1) {
            method.invokeExplosively(target, providerState.getParams());
          } else {
            method.invokeExplosively(target);
          }
        }
        next.evaluate();
    }
}
