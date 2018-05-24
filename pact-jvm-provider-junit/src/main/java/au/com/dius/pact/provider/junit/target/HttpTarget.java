package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.model.ProviderState;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.HttpClientFactory;
import au.com.dius.pact.provider.ProviderClient;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import org.apache.http.HttpRequest;
import org.junit.runners.model.FrameworkMethod;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against http service and verify response
 */
public class HttpTarget extends BaseTarget {
    private final String path;
    private final String host;
    protected int port;
    private final String protocol;
    private final boolean insecure;

  /**
     * @param host host of tested service
     * @param port port of tested service
     */
    public  HttpTarget(final String host, final int port) {
        this("http", host, port);
    }

    /**
     * Host of tested service is assumed as "localhost"
     *
     * @param port port of tested service
     */
    public HttpTarget(final int port) {
        this("http", "localhost", port);
    }

    /**
     * @param host host of tested service
     * @param port port of tested service
     * @param protocol protocol of tested service
     */
    public HttpTarget(final String protocol, final String host, final int port) {
        this(protocol, host, port, "/");
    }

    /**
     * @param host host of tested service
     * @param port port of tested service
     * @param protocol protocol of tested service
     * @param path protocol of the tested service
     */
    public HttpTarget(final String protocol, final String host, final int port, final String path) {
        this(protocol, host, port, path, false);
    }

    /**
     *
     * @param host host of tested service
     * @param port port of tested service
     * @param protocol protocol of the tested service
     * @param path path of the tested service
     * @param insecure true if certificates should be ignored
     */
    public HttpTarget(final String protocol, final String host, final int port, final String path, final boolean insecure){
        super();
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.path = path;
        this.insecure = insecure;
    }

    /**
     * @param url url of the tested service
     */
    public HttpTarget(final URL url) {
        this(url, false);
    }

    /**
     *
     * @param url url of the tested service
     * @param insecure true if certificates should be ignored
     */
    public HttpTarget(final URL url, final boolean insecure) {
        this(url.getProtocol() == null ? "http" : url.getProtocol(),
                url.getHost(),
                url.getPort() == -1 && url.getProtocol().equalsIgnoreCase("http") ? 8080 : url.getPort() == -1 && url.getProtocol().equalsIgnoreCase("https") ? 443 : url.getPort(),
                url.getPath() == null ? "/" : url.getPath(),
                insecure);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final Interaction interaction, PactSource source) {
      ProviderInfo provider = getProviderInfo(source);
      ConsumerInfo consumer = new ConsumerInfo(consumerName);
      ProviderVerifier verifier = setupVerifier(interaction, provider, consumer);

      Map<String, Object> failures = new HashMap<>();
      ProviderClient client = new ProviderClient(provider, new HttpClientFactory());
      verifier.verifyResponseFromProvider(provider, interaction, interaction.getDescription(), failures, client);
      reportTestResult(failures.isEmpty(), verifier);

      try {
        if (!failures.isEmpty()) {
          verifier.displayFailures(failures);
          throw getAssertionError(failures);
        }
      } finally {
        verifier.finialiseReports();
      }
    }

    @Override
    protected ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                             ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier();

    setupReporters(verifier, provider.getName(), interaction.getDescription());

    verifier.initialiseReporters(provider);
    verifier.reportVerificationForConsumer(consumer, provider);

    if (!interaction.getProviderStates().isEmpty()) {
      for (ProviderState providerState: interaction.getProviderStates()) {
        verifier.reportStateForInteraction(providerState.getName(), provider, consumer, true);
      }
    }

    verifier.reportInteractionDescription(interaction);

    return verifier;
  }

  protected ProviderInfo getProviderInfo(PactSource source) {
      Provider provider = testClass.getAnnotation(Provider.class);
      final ProviderInfo providerInfo = new ProviderInfo(provider.value());
      providerInfo.setPort(port);
      providerInfo.setHost(host);
      providerInfo.setProtocol(protocol);
      providerInfo.setPath(path);
      providerInfo.setInsecure(insecure);

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
