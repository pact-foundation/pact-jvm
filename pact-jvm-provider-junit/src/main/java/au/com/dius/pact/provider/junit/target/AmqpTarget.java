package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.PactVerification;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import au.com.dius.pact.provider.junit.Provider;
import org.codehaus.groovy.runtime.MethodClosure;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against message pact and verify response
 */
public class AmqpTarget extends BaseTarget {
    /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final String consumerName, final Interaction interaction) {
      ProviderInfo provider = getProviderInfo();
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

    ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider,
                                         ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier();
    verifier.setProjectClasspath(new MethodClosure(this, "getClassPathUrls"));

    setupReporters(verifier, provider.getName(), interaction.getDescription());

    verifier.initialiseReporters(provider);
    verifier.reportVerificationForConsumer(consumer, provider);

    if (interaction.getProviderState() != null) {
      verifier.reportStateForInteraction(interaction.getProviderState(), provider, consumer, true);
    }

    verifier.reportInteractionDescription(interaction);

    return verifier;
  }

  private URL[] getClassPathUrls() {
    return ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
  }

  ProviderInfo getProviderInfo() {
    Provider provider = testClass.getAnnotation(Provider.class);
    ProviderInfo providerInfo = new ProviderInfo(provider.value());
    providerInfo.setVerificationType(PactVerification.ANNOTATED_METHOD);
    return providerInfo;
  }
}
