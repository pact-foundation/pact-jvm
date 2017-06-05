package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.junit.sysprops.SystemPropertyResolver;
import au.com.dius.pact.provider.junit.sysprops.ValueResolver;
import au.com.dius.pact.provider.reporters.ReporterManager;
import au.com.dius.pact.provider.reporters.VerifierReporter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against message pact and verify response
 */
public abstract class BaseTarget implements TestClassAwareTarget {
  protected TestClass testClass;
  protected Object testTarget;
  protected ValueResolver valueResolver = new SystemPropertyResolver();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract void testInteraction(final String consumerName, final Interaction interaction);

  abstract ProviderInfo getProviderInfo();

  abstract ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                         ConsumerInfo consumer);

  void setupReporters(ProviderVerifier verifier, String name, String description) {
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

  AssertionError getAssertionError(final Map<String, Object> mismatches) {
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

    Throwable cause = err.getCause();
    String details = "";
    if(cause != null)
      details = ExceptionUtils.getStackTrace(cause);

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
