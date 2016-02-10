package au.com.dius.pact.provider.junit.target;

import org.junit.runners.model.TestClass;

/**
 * Interface to target implementations that require more information from the test class (like annotated methods)
 */
public interface TestClassAwareTarget extends Target {
  void setTestClass(TestClass testClass, Object testTarget);
}
