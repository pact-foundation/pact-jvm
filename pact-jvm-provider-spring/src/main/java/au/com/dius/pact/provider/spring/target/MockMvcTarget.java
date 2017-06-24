package au.com.dius.pact.provider.spring.target;

import au.com.dius.pact.model.Interaction;
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
import org.apache.http.HttpRequest;
import org.codehaus.groovy.runtime.MethodClosure;
import org.junit.runners.model.FrameworkMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link RequestResponseInteraction} against Spring MockMVC controllers and verify response
 */
public class MockMvcTarget extends BaseTarget {
    private List<Object> controllers;
    private List<Object> controllerAdvice;
    private boolean printRequestResponse;
    private int runTimes;
    private MockMvc mockMvc;

    public MockMvcTarget() {
        this(Collections.emptyList());
    }

    public MockMvcTarget(List<Object> controllers) {
        this(controllers, false);
    }

    public MockMvcTarget(List<Object> controllers, boolean printRequestResponse) {
        this(controllers, new ArrayList<Object>() {}, printRequestResponse);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, boolean printRequestResponse) {
        this(controllers, controllerAdvice, printRequestResponse, 1);
    }

    public MockMvcTarget(List<Object> controllers, List<Object> controllerAdvice, boolean printRequestResponse, int runTimes) {
        Optional<List<Object>> c = Optional.ofNullable(controllers);
        this.controllers = c.orElseThrow(() -> new IllegalArgumentException("controllers cannot be null"));
        this.controllerAdvice = Optional.ofNullable(controllerAdvice).orElse(Collections.emptyList());
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

    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

  /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final Interaction interaction) {
        ProviderInfo provider = getProviderInfo();
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

        return standaloneSetup(controllers.toArray())
            .setControllerAdvice(controllerAdvice.toArray()).build();
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

        verifier.setProjectClasspath(new MethodClosure(this, "getClassPathUrls"));

        verifier.initialiseReporters(provider);
        verifier.reportVerificationForConsumer(consumer, provider);

        if (interaction.getProviderState() != null) {
            verifier.reportStateForInteraction(interaction.getProviderState(), provider, consumer, true);
        }

        verifier.reportInteractionDescription(interaction);

        return verifier;
    }

    @Override
    protected ProviderInfo getProviderInfo() {
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
}

