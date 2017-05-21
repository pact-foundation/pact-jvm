package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.RequestResponsePact;
import au.com.dius.pact.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
              Optional<RequestResponsePact> pact = Optional.empty();
              if (pactDef.value().length == 1 && StringUtils.isEmpty(pactDef.value()[0])) {
                  pact = Optional.of(pacts.values().iterator().next());
              } else {
                for (String verification: pactDef.value()) {
                  if (pacts.containsKey(verification)) {
                    pact = Optional.of(pacts.get(verification));
                  }
                }
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
      Optional<PactVerification> optional = Optional.empty();
      for (PactVerification p: pactVerificationValues) {
        String[] providers = p.value();
        if (providers.length != 1) {
          throw new IllegalArgumentException(
            "Each @PactVerification must specify one and only provider when using @PactVerifications");
        }
        String provider = providers[0];
        if (provider.equals(this.provider)) {
          optional = Optional.of(p);
        }
      }
      return optional;
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

  class PactTestRunner implements PactTestRun {

    private final BaseProviderRule baseProviderRule;
    private final Statement base;

    PactTestRunner(BaseProviderRule baseProviderRule, Statement base) {
      this.baseProviderRule = baseProviderRule;
      this.base = base;
    }

    @Override
    public void run(@NotNull MockServer mockServer) throws Throwable {
      baseProviderRule.mockServer = mockServer;
      base.evaluate();
      baseProviderRule.mockServer = null;
    }
  }

  private PactVerificationResult runPactTest(final Statement base, RequestResponsePact pact) {
      return runConsumerTest(pact, config, new PactTestRunner(this, base));
  }

  protected void validateResult(PactVerificationResult result, PactVerification pactVerification) throws Throwable {
    if (!result.equals(PactVerificationResult.Ok.INSTANCE)) {
      if (result instanceof PactVerificationResult.Error) {
        PactVerificationResult.Error error = (PactVerificationResult.Error) result;
        if (error.getMockServerState() != PactVerificationResult.Ok.INSTANCE) {
          throw new AssertionError("Pact Test function failed with an exception, possibly due to " +
            error.getMockServerState(), ((PactVerificationResult.Error) result).getError());
        } else {
          throw new AssertionError("Pact Test function failed with an exception: " +
            error.getError().getMessage(), error.getError());
        }
      } else {
        throw new PactMismatchesException(result);
      }
    }
  }

  /**
   * scan all methods for @Pact annotation and execute them, if not already initialized
   * @param fragment
   */
  protected Map<String, RequestResponsePact> getPacts(String fragment) {
      if (pacts == null) {
        pacts = new HashMap<>();
          for (Method m: target.getClass().getMethods()) {
              if (conformsToSignature(m) && methodMatchesFragment(m, fragment)) {
                  Pact pact = m.getAnnotation(Pact.class);
                  if (StringUtils.isEmpty(pact.provider()) || provider.equals(pact.provider())) {
                      PactDslWithProvider dslBuilder = ConsumerPactBuilder.consumer(pact.consumer())
                          .hasPactWith(provider);
                      try {
                        pacts.put(provider, (RequestResponsePact) m.invoke(target, dslBuilder));
                      } catch (Exception e) {
                          throw new RuntimeException("Failed to invoke pact method", e);
                      }
                  }
              }
          }
      }
      return pacts;
  }

  private boolean methodMatchesFragment(Method m, String fragment) {
      return StringUtils.isEmpty(fragment) || m.getName().equals(fragment);
  }

  /**
   * validates method signature as described at {@link Pact}
   */
  private boolean conformsToSignature(Method m) {
      Pact pact = m.getAnnotation(Pact.class);
      boolean conforms =
          pact != null
          && RequestResponsePact.class.isAssignableFrom(m.getReturnType())
          && m.getParameterTypes().length == 1
          && m.getParameterTypes()[0].isAssignableFrom(PactDslWithProvider.class);

      if (!conforms && pact != null) {
          throw new UnsupportedOperationException("Method " + m.getName() +
              " does not conform required method signature 'public RequestResponsePact xxx(PactDslWithProvider builder)'");
      }
      return conforms;
  }

  /**
   * Returns the URL for the mock server
   * @return String URL
   */
  public String getUrl() {
    return mockServer == null ? null : mockServer.getUrl();
  }

  public Integer getPort() {
    return mockServer == null ? null : mockServer.getPort();
  }
}
