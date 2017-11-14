package au.com.dius.pact.provider.spring.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.model.ProviderState;
import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.PactVerification;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.target.BaseTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.spring.MvcProviderVerifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.junit.runners.model.FrameworkMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link RequestResponseInteraction} against Spring MockMVC controllers and verify response
 */
public class MockMvcTarget extends BaseTarget {
    private List<Object> controllers;
    private List<Object> controllerAdvice;
    private List<HttpMessageConverter> messageConverters;
    private boolean printRequestResponse;
    private int runTimes;
    private MockMvc mockMvc;
    private String servletPath;

  public MockMvcTarget() {
        this(Collections.emptyList());
    }

    public MockMvcTarget(List<Object> controllers) {
        this(controllers, false);
    }

    public MockMvcTarget(List<Object> controllers, boolean printRequestResponse) {
        this(controllers, printRequestResponse, 1);
    }

    public MockMvcTarget(List<Object> controllers, boolean printRequestResponse, int runTimes) {
        this(controllers, new ArrayList<Object>() {}, printRequestResponse, runTimes);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, boolean printRequestResponse) {
        this(controllers, controllerAdvice, printRequestResponse, 1);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, boolean printRequestResponse, int runTimes) {
        this(controllers, controllerAdvice, new ArrayList<HttpMessageConverter>() {}, printRequestResponse, runTimes);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, List<HttpMessageConverter> messageConverters, boolean printRequestResponse) {
        this(controllers, controllerAdvice, messageConverters, printRequestResponse, 1);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, List<HttpMessageConverter> messageConverters, boolean printRequestResponse, int runTimes) {
        Optional<List<Object>> c = Optional.ofNullable(controllers);
        this.controllers = c.orElseThrow(() -> new IllegalArgumentException("controllers cannot be null"));
        this.controllerAdvice = Optional.ofNullable(controllerAdvice).orElse(Collections.emptyList());
        this.messageConverters = Optional.ofNullable(messageConverters).orElse(Collections.emptyList());
        this.printRequestResponse = printRequestResponse;
        this.runTimes = runTimes;
    }

    public void setRunTimes(int runTimes) {
        this.runTimes = runTimes;
    }

    public void setPrintRequestResponse(boolean printRequestResponse) {
        this.printRequestResponse = printRequestResponse;
    }

    public void setControllers(Object... controllers) {
        Optional<Object[]> c = Optional.ofNullable(controllers);
        this.controllers = Arrays.asList(c.orElseThrow(() -> new IllegalArgumentException("controllers cannot be null")));
    }

    public void setControllerAdvice(Object... controllerAdvice) {
        this.controllerAdvice = Arrays.asList(Optional.ofNullable(controllerAdvice).orElse(new Object[0]));
    }

    public void setMessageConvertors(HttpMessageConverter... messageConverters) {
        this.messageConverters = Arrays.asList(
            Optional
                .ofNullable(messageConverters)
                .orElse(new HttpMessageConverter[0])
        );
    }

  public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

  /**
   * Sets the servlet path on the default request, if one is required
   * @param servletPath The servlet path prefix
   */
  public void setServletPath(String servletPath) {
    this.servletPath = servletPath;
  }

  /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final Interaction interaction, PactSource source) {
        ProviderInfo provider = getProviderInfo(source);
        ConsumerInfo consumer = new ConsumerInfo(consumerName);
        provider.setVerificationType(PactVerification.ANNOTATED_METHOD);

        MockMvc mockMvc = buildMockMvc();

        MvcProviderVerifier verifier = (MvcProviderVerifier)setupVerifier(interaction, provider, consumer);

        Map<String, Object> failures = new HashMap<>();

        for(int i = 0; i < runTimes; i++) {
            verifier.verifyResponseFromProvider(provider, interaction, interaction.getDescription(), failures, mockMvc);
        }

        try {
            if (!failures.isEmpty()) {
                verifier.displayFailures(failures);
                throw getAssertionError(failures);
            }
        } finally {
            verifier.finialiseReports();
        }
    }

    private MockMvc buildMockMvc() {
        if (mockMvc != null) {
            return mockMvc;
        }

      MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/");
      if (StringUtils.isNoneEmpty(servletPath)) {
        requestBuilder.servletPath(servletPath);
      }
      return standaloneSetup(controllers.toArray())
            .setControllerAdvice(controllerAdvice.toArray())
            .setMessageConverters(getMessageConverterArray())
            .defaultRequest(requestBuilder)
            .build();
    }

    private URL[] getClassPathUrls() {
        return ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
    }

    @Override
    protected ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                   ConsumerInfo consumer) {
        MvcProviderVerifier verifier = new MvcProviderVerifier();
        verifier.setDebugRequestResponse(printRequestResponse);

        setupReporters(verifier, provider.getName(), interaction.getDescription());

        verifier.setProjectClasspath(this::getClassPathUrls);

        verifier.initialiseReporters(provider);
        verifier.reportVerificationForConsumer(consumer, provider);

        if (!interaction.getProviderStates().isEmpty()) {
          for (ProviderState state: interaction.getProviderStates()) {
            verifier.reportStateForInteraction(state.getName(), provider, consumer, true);
          }
        }

        verifier.reportInteractionDescription(interaction);

        return verifier;
    }

    @Override
    protected ProviderInfo getProviderInfo(PactSource source) {
        Provider provider = testClass.getAnnotation(Provider.class);
        final ProviderInfo providerInfo = new ProviderInfo(provider.value());

        if (testClass != null) {
            final List<FrameworkMethod> methods = testClass.getAnnotatedMethods(TargetRequestFilter.class);
            if (!methods.isEmpty()) {
                providerInfo.setRequestFilter((Consumer<HttpRequest>) httpRequest -> methods.forEach(method -> {
                    try {
                        method.invokeExplosively(testTarget, httpRequest);
                    } catch (Throwable t) {
                        throw new AssertionError("Request filter method " + method.getName() + " failed with an exception", t);
                    }
                }));
            }
        }

        return providerInfo;
    }

    private HttpMessageConverter[] getMessageConverterArray() {
        return messageConverters.toArray(
            new HttpMessageConverter[messageConverters.size()]
        );
    }

}
