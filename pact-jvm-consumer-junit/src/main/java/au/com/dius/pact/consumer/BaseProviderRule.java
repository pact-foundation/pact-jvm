package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.JUnitTestSupport;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;

public class BaseProviderRule extends ExternalResource {

  protected final String provider;
  protected final Object target;
  protected MockProviderConfig config;
  private Map<String, RequestResponsePact> pacts;
  private MockServer mockServer;

  public BaseProviderRule(Object target, String provider, String hostInterface, Integer port, PactSpecVersion pactVersion) {
    this.target = target;
    this.provider = provider;
    config = MockProviderConfig.httpConfig(StringUtils.isEmpty(hostInterface) ? MockProviderConfig.LOCALHOST : hostInterface,
      port == null ? 0 : port, pactVersion);
  }

  public MockProviderConfig getConfig() {
      return config;
  }

  public MockServer getMockServer() {
    return mockServer;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
      return new Statement() {

          @Override
          public void evaluate() throws Throwable {
              PactVerifications pactVerifications = description.getAnnotation(PactVerifications.class);
              if (pactVerifications != null) {
                  evaluatePactVerifications(pactVerifications, base);
                  return;
              }

              PactVerification pactDef = description.getAnnotation(PactVerification.class);
              // no pactVerification? execute the test normally
              if (pactDef == null) {
                  base.evaluate();
                  return;
              }

              Map<String, RequestResponsePact> pacts = getPacts(pactDef.fragment());
              Optional<RequestResponsePact> pact;
              if (pactDef.value().length == 1 && StringUtils.isEmpty(pactDef.value()[0])) {
                  pact = pacts.values().stream().findFirst();
              } else {
                  pact = Arrays.stream(pactDef.value()).map(pacts::get)
                          .filter(Objects::nonNull).findFirst();
              }
              if (!pact.isPresent()) {
                  base.evaluate();
                  return;
              }

              PactVerificationResult result = runPactTest(base, pact.get());
              validateResult(result, pactDef);
          }
      };
  }

  private void evaluatePactVerifications(PactVerifications pactVerifications, Statement base) throws Throwable {
      Optional<PactVerification> possiblePactVerification = findPactVerification(pactVerifications);
      if (!possiblePactVerification.isPresent()) {
          base.evaluate();
          return;
      }

      PactVerification pactVerification = possiblePactVerification.get();
      Optional<Method> possiblePactMethod = findPactMethod(pactVerification);
      if (!possiblePactMethod.isPresent()) {
          throw new UnsupportedOperationException("Could not find method with @Pact for the provider " + provider);
      }

      Method method = possiblePactMethod.get();
      Pact pactAnnotation = method.getAnnotation(Pact.class);
      PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pactAnnotation.consumer()).hasPactWith(provider);
      RequestResponsePact pact;
      try {
        pact = (RequestResponsePact) method.invoke(target, dslBuilder);
      } catch (Exception e) {
          throw new RuntimeException("Failed to invoke pact method", e);
      }
      PactVerificationResult result = runPactTest(base, pact);
      validateResult(result, pactVerification);
  }

  private Optional<PactVerification> findPactVerification(PactVerifications pactVerifications) {
      PactVerification[] pactVerificationValues = pactVerifications.value();
      return Arrays.stream(pactVerificationValues).filter(p -> {
          String[] providers = p.value();
          if (providers.length != 1) {
              throw new IllegalArgumentException(
                      "Each @PactVerification must specify one and only provider when using @PactVerifications");
          }
          String provider = providers[0];
          return provider.equals(this.provider);
      }).findFirst();
  }

  private Optional<Method> findPactMethod(PactVerification pactVerification) {
      String pactFragment = pactVerification.fragment();
      for (Method method : target.getClass().getMethods()) {
          Pact pact = method.getAnnotation(Pact.class);
          if (pact != null && pact.provider().equals(provider)
                  && (pactFragment.isEmpty() || pactFragment.equals(method.getName()))) {

              validatePactSignature(method);
              return Optional.of(method);
          }
      }
      return Optional.empty();
  }

  private void validatePactSignature(Method method) {
      boolean hasValidPactSignature =
        RequestResponsePact.class.isAssignableFrom(method.getReturnType())
                      && method.getParameterTypes().length == 1
                      && method.getParameterTypes()[0].isAssignableFrom(PactDslWithProvider.class);

      if (!hasValidPactSignature) {
          throw new UnsupportedOperationException("Method " + method.getName() +
              " does not conform required method signature 'public RequestResponsePact xxx(PactDslWithProvider builder)'");
      }
  }

  private PactVerificationResult runPactTest(final Statement base, RequestResponsePact pact) {
      return runConsumerTest(pact, config, mockServer -> {
        this.mockServer = mockServer;
        base.evaluate();
        this.mockServer = null;
      });
  }

  /**
   * @deprecated Use the static method JUnitTestSupport.validateMockServerResult
   */
  @Deprecated
  protected void validateResult(PactVerificationResult result, PactVerification pactVerification) throws Throwable {
    JUnitTestSupport.validateMockServerResult(result);
  }

  /**
   * scan all methods for @Pact annotation and execute them, if not already initialized
   * @param fragment
   */
  protected Map<String, RequestResponsePact> getPacts(String fragment) {
      if (pacts == null) {
        pacts = new HashMap<>();
          for (Method m: target.getClass().getMethods()) {
              if (JUnitTestSupport.conformsToSignature(m) && methodMatchesFragment(m, fragment)) {
                  Pact pactAnnotation = m.getAnnotation(Pact.class);
                  if (StringUtils.isEmpty(pactAnnotation.provider()) || provider.equals(pactAnnotation.provider())) {
                      PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pactAnnotation.consumer())
                          .hasPactWith(provider);
                      updateAnyDefaultValues(dslBuilder);
                      try {
                        RequestResponsePact pact = (RequestResponsePact) m.invoke(target, dslBuilder);
                        pacts.put(provider, pact);
                      } catch (Exception e) {
                          throw new RuntimeException("Failed to invoke pact method", e);
                      }
                  }
              }
          }
      }
      return pacts;
  }

  private void updateAnyDefaultValues(PactDslWithProvider dslBuilder) {
    for (Method m: target.getClass().getMethods()) {
      if (m.isAnnotationPresent(DefaultRequestValues.class)) {
        setupDefaultRequestValues(dslBuilder, m);
      } else if (m.isAnnotationPresent(DefaultResponseValues.class)) {
        setupDefaultResponseValues(dslBuilder, m);
      }
    }
  }

  private void setupDefaultRequestValues(PactDslWithProvider dslBuilder, Method m) {
    if (m.getParameterTypes().length == 1
      && m.getParameterTypes()[0].isAssignableFrom(PactDslRequestWithoutPath.class)) {
      PactDslRequestWithoutPath defaults = dslBuilder.uponReceiving("defaults");
      try {
        m.invoke(target, defaults);
      } catch (IllegalAccessException| InvocationTargetException e) {
        throw new RuntimeException("Failed to invoke default request method", e);
      }
      dslBuilder.setDefaultRequestValues(defaults);
    } else {
      throw new UnsupportedOperationException("Method " + m.getName() +
        " does not conform required method signature 'public void " + m.getName() +
        "(PactDslRequestWithoutPath defaultRequest)'");
    }
  }

  private void setupDefaultResponseValues(PactDslWithProvider dslBuilder, Method m) {
    if (m.getParameterTypes().length == 1
      && m.getParameterTypes()[0].isAssignableFrom(PactDslResponse.class)) {
      PactDslResponse defaults = new PactDslResponse(dslBuilder.getConsumerPactBuilder(), null, null, null);
      try {
        m.invoke(target, defaults);
      } catch (IllegalAccessException| InvocationTargetException e) {
        throw new RuntimeException("Failed to invoke default response method", e);
      }
      dslBuilder.setDefaultResponseValues(defaults);
    } else {
      throw new UnsupportedOperationException("Method " + m.getName() +
        " does not conform required method signature 'public void " + m.getName() +
        "(PactDslResponse defaultResponse)'");
    }
  }

  private boolean methodMatchesFragment(Method m, String fragment) {
      return StringUtils.isEmpty(fragment) || m.getName().equals(fragment);
  }

  /**
   * Returns the URL for the mock server. Returns null if the mock server is not running.
   * @return String URL or null if mock server not running
   */
  public String getUrl() {
    return mockServer == null ? null : mockServer.getUrl();
  }

  /**
   * Returns the port number for the mock server. Returns null if the mock server is not running.
   * @return port number or null if mock server not running
   */
  public Integer getPort() {
    return mockServer == null ? null : mockServer.getPort();
  }
}
