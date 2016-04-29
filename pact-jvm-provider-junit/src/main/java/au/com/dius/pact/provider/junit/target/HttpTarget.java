package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.reporters.ReporterManager;
import au.com.dius.pact.provider.reporters.VerifierReporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link RequestResponseInteraction} against http service and verify response
 */
public class HttpTarget implements TestClassAwareTarget {
    private final String path;
    private final String host;
    private final int port;
    private final String protocol;
    private TestClass testClass;
    private Object testTarget;

  /**
     * @param host host of tested service
     * @param port port of tested service
     */
    public HttpTarget(final String host, final int port) {
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
     * @param protocol of tested service
     */
    public HttpTarget(final String protocol, final String host, final int port) {
        this(protocol, host, port, "/");
    }

    /**
     * @param host host of tested service
     * @param port port of tested service
     * @param protocol of tested service
     * @param path of the tested service
     */
    public HttpTarget(final String protocol, final String host, final int port, final String path) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.path = path;
    }

    /**
     * @param url of the tested service
     */
    public HttpTarget(final URL url) {
        this(url.getProtocol() == null ? "http" : url.getProtocol(),
                url.getHost(),
                url.getPort() == -1 ? 8080 : url.getPort(),
                url.getPath() == null ? "/" : url.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final RequestResponseInteraction interaction) {
      ProviderInfo provider = getProviderInfo();
      ConsumerInfo consumer = new ConsumerInfo(consumerName);
      ProviderVerifier verifier = setupVerifier(interaction, provider, consumer);

      Map<String, Object> failures = new HashMap<>();
      verifier.verifyResponseFromProvider(provider, interaction, interaction.getDescription(), failures);

      try {
        if (!failures.isEmpty()) {
          verifier.displayFailures(failures);
          throw getAssertionError(failures);
        }
      } finally {
        verifier.finialiseReports();
      }
    }

  private ProviderVerifier setupVerifier(RequestResponseInteraction interaction, ProviderInfo provider,
                                         ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier();

    setupReporters(verifier, provider.getName(), interaction.getDescription());

    verifier.initialiseReporters(provider);
    verifier.reportVerificationForConsumer(consumer, provider);

    if (interaction.getProviderState() != null) {
      verifier.reportStateForInteraction(interaction.getProviderState(), provider, consumer, true);
    }

    verifier.reportInteractionDescription(interaction);

    return verifier;
  }

  private void setupReporters(ProviderVerifier verifier, String name, String description) {
    VerificationReports verificationReports = testClass.getAnnotation(VerificationReports.class);
    if (verificationReports != null) {
      File reportDir = new File(verificationReports.reportDir());
      reportDir.mkdirs();
      verifier.setReporters(Seq.of(verificationReports.value())
        .map(r -> {
          VerifierReporter reporter = ReporterManager.createReporter(r);
          reporter.setReportDir(reportDir);
          reporter.setReportFile(new File(reportDir, name + " - " + description + reporter.getExt()));
          return reporter;
        })
        .toList());
    }
  }

  private ProviderInfo getProviderInfo() {
      Provider provider = testClass.getAnnotation(Provider.class);
      final ProviderInfo providerInfo = new ProviderInfo(provider.value());
      providerInfo.setPort(port);
      providerInfo.setHost(host);
      providerInfo.setProtocol(protocol);
      providerInfo.setPath(path);

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

  private AssertionError getAssertionError(final Map<String, Object> mismatches) {
    String error = System.lineSeparator() + Seq.seq(mismatches.values()).zipWithIndex()
      .map(i -> {
        String errPrefix = String.valueOf(i.v2) + " - ";
        if (i.v1 instanceof Throwable) {
          return errPrefix + exceptionMessage((Throwable) i.v1, errPrefix.length());
        } else if (i.v1 instanceof Map) {
          return errPrefix + convertMapToErrorString((Map) i.v1);
        } else {
          return errPrefix + i.v1.toString();
        }
      }).toString(System.lineSeparator());
    return new AssertionError(error);
  }

  private String exceptionMessage(Throwable err, int prefixLength) {
    String message = err.getMessage();
    if (message.contains("\n")) {
      String padString = StringUtils.leftPad("", prefixLength);
      Tuple2<Optional<String>, Seq<String>> lines = Seq.of(message.split("\n")).splitAtHead();
      return lines.v1.orElse("") + System.lineSeparator() + lines.v2.map(line -> padString + line)
        .toString(System.lineSeparator());
    } else {
      return message;
    }
  }

  private String convertMapToErrorString(Map mismatches) {
    if (mismatches.containsKey("comparison")) {
      Object comparison = mismatches.get("comparison");
      if (mismatches.containsKey("diff")) {
        return mapToString((Map) comparison);
      } else {
        if (comparison instanceof Map) {
          return mapToString((Map) comparison);
        } else {
          return String.valueOf(comparison);
        }
      }
    } else {
      return mapToString(mismatches);
    }
  }

  private String mapToString(Map comparison) {
    return comparison.entrySet().stream()
      .map(e -> String.valueOf(((Map.Entry)e).getKey()) + " -> " + ((Map.Entry)e).getValue())
      .collect(Collectors.joining(System.lineSeparator())).toString();
  }

  @Override
    public void setTestClass(final TestClass testClass, final Object testTarget) {
      this.testClass = testClass;
      this.testTarget = testTarget;
    }
}
