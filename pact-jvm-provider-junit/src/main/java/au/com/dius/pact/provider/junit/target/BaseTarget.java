package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.junit.sysprops.SystemPropertyResolver;
import au.com.dius.pact.provider.junit.sysprops.ValueResolver;
import au.com.dius.pact.provider.reporters.ReporterManager;
import au.com.dius.pact.provider.reporters.VerifierReporter;
import org.jooq.lambda.Seq;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against message pact and verify response
 */
public abstract class BaseTarget implements TestClassAwareTarget {
  protected TestClass testClass;
  protected Object testTarget;
  protected ValueResolver valueResolver = new SystemPropertyResolver();
  private List<BiConsumer<Boolean, ProviderVerifier>> callbacks = new ArrayList<>();

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
    return new AssertionError(JUnitProviderTestSupport.generateErrorStringFromMismatches(mismatches));
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

  @Override
  public void addResultCallback(BiConsumer<Boolean, ProviderVerifier> callback) {
    this.callbacks.add(callback);
  }

  protected void reportTestResult(Boolean result, ProviderVerifier verifier) {
    this.callbacks.forEach(callback -> callback.accept(result, verifier));
  }
}
