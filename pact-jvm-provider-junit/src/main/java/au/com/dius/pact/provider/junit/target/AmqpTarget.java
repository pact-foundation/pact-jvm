package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.DirectorySource;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactBrokerSource;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.model.ProviderState;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.PactVerification;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.loader.PactFolderLoader;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against message pact and verify response
 */
public class AmqpTarget extends BaseTarget {
  private List<String> packagesToScan = Collections.emptyList();

  public AmqpTarget() { }

  /**
   * Initialises the AMPQ target with the list of packages to scan
   * @param packagesToScan List of JVM packages
   */
  public AmqpTarget(List<String> packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final Interaction interaction, final PactSource source) {
      ProviderInfo provider = getProviderInfo(source);
      ConsumerInfo consumer = new ConsumerInfo(consumerName);
      ProviderVerifier verifier = setupVerifier(interaction, provider, consumer);

      Map<String, Object> failures = new HashMap<>();
      verifier.verifyResponseByInvokingProviderMethods(provider, consumer, interaction, interaction.getDescription(),
        failures);

      try {
        if (!failures.isEmpty()) {
          verifier.displayFailures(failures);
          throw getAssertionError(failures);
        }
      } finally {
        verifier.finialiseReports();
      }
    }

    protected ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                             ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier();
    verifier.setProjectClasspath(new MethodClosure(this, "getClassPathUrls"));

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

  private URL[] getClassPathUrls() {
    return ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
  }

  protected ProviderInfo getProviderInfo(PactSource source) {
    Provider provider = testClass.getAnnotation(Provider.class);
    ProviderInfo providerInfo = new ProviderInfo(provider.value());
    providerInfo.setVerificationType(PactVerification.ANNOTATED_METHOD);
    providerInfo.setPackagesToScan(packagesToScan);

    if (source instanceof PactBrokerSource) {
      PactBrokerSource brokerSource = (PactBrokerSource) source;
      providerInfo.setConsumers(brokerSource.getPacts().entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(p -> new ConsumerInfo(e.getKey().getName(), p)))
        .collect(Collectors.toList()));
    } else if (source instanceof DirectorySource) {
      DirectorySource directorySource = (DirectorySource) source;
      providerInfo.setConsumers(directorySource.getPacts().entrySet().stream()
        .map(e -> new ConsumerInfo(e.getValue().getConsumer().getName(), e.getValue()))
        .collect(Collectors.toList())
      );
    }

    return providerInfo;
  }
}
