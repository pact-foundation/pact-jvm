package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.junit.sysprops.SystemPropertyResolver;
import au.com.dius.pact.provider.junit.sysprops.ValueResolver;
import au.com.dius.pact.provider.reporters.ReporterManager;
import au.com.dius.pact.provider.reporters.VerifierReporter;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link RequestResponseInteraction} against http service and verify response
 */
public class HttpTarget implements TestClassAwareTarget {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTarget.class);

    private final String path;
    private final String host;
    private final int port;
    private final String protocol;
    private final boolean insecure;
    private TestClass testClass;
    private Object testTarget;
    private ValueResolver valueResolver = new SystemPropertyResolver();

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
    public HttpTarget(final String protocol, final String host, final int port, final String path, final boolean insecure) {
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
                url.getPort() == -1 ? 8080 : url.getPort(),
                url.getPath() == null ? "/" : url.getPath(),
                insecure);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final RequestResponseInteraction interaction) {
      ProviderInfo provider = getProviderInfo();
      ConsumerInfo consumer = new ConsumerInfo(consumerName);
      ProviderVerifier verifier = setupVerifier(interaction, provider, consumer);

      Map<String, Object> failures = new HashMap<String, Object>();
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
    String reportDirectory = "target/pact/reports";
    String[] reports = new String[]{};
    boolean reportingEnabled = false;

    VerificationReports verificationReports = testClass.getAnnotation(VerificationReports.class);
    if (verificationReports != null) {
      reportingEnabled = true;
      reportDirectory = verificationReports.reportDir();
      reports = verificationReports.value();
    } else if (valueResolver.propertyDefined("pact.verification.reports")) {
      reportingEnabled = true;
      reportDirectory = valueResolver.resolveValue("pact.verification.reportDir:" + reportDirectory);
      reports = valueResolver.resolveValue("pact.verification.reports:").split(",");
    }

    if (reportingEnabled) {
      File reportDir = new File(reportDirectory);
      reportDir.mkdirs();
      List<VerifierReporter> reportsList = new ArrayList<VerifierReporter>();
      for (String report: reports) {
        if (!report.isEmpty()) {
          VerifierReporter reporter = ReporterManager.createReporter(report.trim());
          reporter.setReportDir(reportDir);
          reporter.setReportFile(new File(reportDir, name + " - " + description + reporter.getExt()));
          reportsList.add(reporter);
        }
      }
      verifier.setReporters(reportsList);
    }
  }

  private ProviderInfo getProviderInfo() {
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
          providerInfo.setRequestFilter(new Closure() {
            @Override
            public void execute(Object httpRequest) {
              for (FrameworkMethod method : methods) {
                try {
                  method.invokeExplosively(testTarget, httpRequest);
                } catch (Throwable t) {
                  LOGGER.error("Request filter failed with an exception", t);
                  throw new AssertionError("Request filter method " + method.getName() + " failed with an exception");
                }
              }
            }
          });
        }
      }

      return providerInfo;
    }

  private AssertionError getAssertionError(final Map<String, Object> mismatches) {
    String error = SystemUtils.LINE_SEPARATOR;

    int count = 0;
    for (Object mismatch: mismatches.values()) {
      String errPrefix = String.valueOf(count++) + " - ";
      if (mismatch instanceof Throwable) {
        error += errPrefix + exceptionMessage((Throwable) mismatch, errPrefix.length());
      } else if (mismatch instanceof Map) {
        error += errPrefix + convertMapToErrorString((Map) mismatch);
      } else {
        error += errPrefix + mismatch.toString();
      }
      error += SystemUtils.LINE_SEPARATOR;
    }

    return new AssertionError(error);
  }

  private String exceptionMessage(Throwable err, int prefixLength) {
    String message = err.getMessage();
    if (message.contains("\n")) {
      String padString = StringUtils.leftPad("", prefixLength);
      String[] lines = message.split("\n");
      message = lines[0] + SystemUtils.LINE_SEPARATOR;
      for (int line = 1; line < lines.length; line++) {
        message += padString + lines[line] + SystemUtils.LINE_SEPARATOR;
      }
    }
    return message;
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
    String map = "";
    for (Object o: comparison.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      map += String.valueOf(e.getKey()) + " -> " + e.getValue() + SystemUtils.LINE_SEPARATOR;
    }
    return map;
  }

  @Override
  public void setTestClass(final TestClass testClass, final Object testTarget) {
    this.testClass = testClass;
    this.testTarget = testTarget;
  }

  public ValueResolver getValueResolver() {
    return valueResolver;
  }

  public void setValueResolver(ValueResolver valueResolver) {
    this.valueResolver = valueResolver;
  }
}
