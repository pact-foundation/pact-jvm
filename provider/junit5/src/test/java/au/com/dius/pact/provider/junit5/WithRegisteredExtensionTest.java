package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Provider("myAwesomeService")
@PactFolder("pacts")
public class WithRegisteredExtensionTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(WithRegisteredExtensionTest.class);

  @RegisterExtension
  static final TestDependencyResolver resolverExt = new TestDependencyResolver(/*...*/);

  private final TestDependency dependency;

  public WithRegisteredExtensionTest(TestDependency dependency) {
    this.dependency = dependency;
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void test() {
    assertThat(dependency, is(notNullValue()));
  }

  @State("state 2")
  void state2() {
  }

  @State("default")
  void stateDefault() {
  }

  static class TestDependencyResolver implements Extension, ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      return parameterContext.getParameter().getType().isAssignableFrom(TestDependency.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      return new TestDependency();
    }
  }

  static class TestDependency {
  }
}
