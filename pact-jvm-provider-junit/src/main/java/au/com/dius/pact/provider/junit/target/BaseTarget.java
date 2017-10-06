package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.junit.sysprops.SystemPropertyResolver;
import au.com.dius.pact.provider.junit.sysprops.ValueResolver;
import au.com.dius.pact.provider.reporters.ReporterManager;
import au.com.dius.pact.provider.reporters.VerifierReporter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
  public abstract void testInteraction(final String consumerName, final Interaction interaction, PactSource source);

  protected abstract ProviderInfo getProviderInfo(PactSource source);

  protected abstract ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                                    ConsumerInfo consumer);

  protected void setupReporters(ProviderVerifier verifier, String name, String description) {
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
      verifier.setReporters(Seq.of(reports)
        .filter(r -> !r.isEmpty())
        .map(r -> {
          VerifierReporter reporter = ReporterManager.createReporter(r.trim());
          reporter.setReportDir(reportDir);
          reporter.setReportFile(new File(reportDir, name + " - " + description + reporter.getExt()));
          return reporter;
        }).toList());
    }
  }

  protected AssertionError getAssertionError(final Map<String, Object> mismatches) {
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

    Throwable cause = err.getCause();
    String details = "";
    if (cause != null) {
      details = ExceptionUtils.getStackTrace(cause);
    }

    if (message.contains("\n")) {
      String padString = StringUtils.leftPad("", prefixLength);
      Tuple2<Optional<String>, Seq<String>> lines = Seq.of(message.split("\n")).splitAtHead();
      return lines.v1.orElse("") + System.lineSeparator() + lines.v2.map(line -> padString + line)
              .toString(System.lineSeparator()) + "\n" + details;
    } else {
      return message + "\n" + details;
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

  public ValueResolver getValueResolver() {
    return valueResolver;
  }

  public void setValueResolver(ValueResolver valueResolver) {
    this.valueResolver = valueResolver;
  }
}
