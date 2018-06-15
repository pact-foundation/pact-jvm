package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo

/**
 * Interface to verification reporters that can hook into the events of the PactVerifier
 */
trait VerifierReporter {
  String ext

  abstract void setReportDir(File reportDir)
  abstract void setReportFile(File reportFile)

  abstract void initialise(ProviderInfo provider)
  abstract void finaliseReport()
  abstract void reportVerificationForConsumer(ConsumerInfo consumer, ProviderInfo provider)
  abstract void verifyConsumerFromUrl(UrlPactSource pactUrl, ConsumerInfo consumer)
  abstract void verifyConsumerFromFile(PactSource pactFile, ConsumerInfo consumer)
  abstract void pactLoadFailureForConsumer(ConsumerInfo consumerInfo, String message)
  abstract void warnProviderHasNoConsumers(ProviderInfo providerInfo)
  abstract void warnPactFileHasNoInteractions(Pact pact)
  abstract void interactionDescription(Interaction interaction)
  abstract void stateForInteraction(String state, ProviderInfo provider, ConsumerInfo consumer, boolean isSetup)
  abstract void warnStateChangeIgnored(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo)
  @SuppressWarnings('ParameterCount')
  abstract void stateChangeRequestFailedWithException(String state, ProviderInfo providerInfo,
                                                      ConsumerInfo consumerInfo, boolean isSetup, Exception e,
                                                      boolean printStackTrace)
  abstract void stateChangeRequestFailed(String state, ProviderInfo providerInfo, boolean isSetup, String httpStatus)
  abstract void warnStateChangeIgnoredDueToInvalidUrl(String state, ProviderInfo providerInfo, boolean isSetup,
                                                      def stateChangeHandler)
  abstract void requestFailed(ProviderInfo providerInfo, Interaction interaction, String interactionMessage,
                              Exception e, boolean printStackTrace)
  abstract void returnsAResponseWhich()
  abstract void statusComparisonOk(int status)
  abstract void statusComparisonFailed(int status, def comparison)
  abstract void includesHeaders()
  abstract void headerComparisonOk(String key, String value)
  abstract void headerComparisonFailed(String key, String value, def comparison)
  abstract void bodyComparisonOk()
  abstract void bodyComparisonFailed(def comparison)
  abstract void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction)
  abstract void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace)
  abstract void generatesAMessageWhich()
  abstract void displayFailures(Map failures)
}
