package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo

/**
 * Pact verifier reporter that displays the results of the verification in a markdown document
 */
class MarkdownReporter implements VerifierReporter {

  String name
  File reportDir
  File reportFile

  @Override
  void initialise(ProviderInfo provider) {
    reportFile = reportFile ?: new File(reportDir, (provider.name + '.md'))
  }

  @Override
  void finaliseReport() {

  }

  @Override
  void reportVerificationForConsumer(ConsumerInfo consumer, ProviderInfo provider) {

  }

  @Override
  void verifyConsumerFromUrl(ConsumerInfo consumer) {

  }

  @Override
  void verifyConsumerFromFile(ConsumerInfo consumer) {

  }

  @Override
  void pactLoadFailureForConsumer(ConsumerInfo consumerInfo, String message) {

  }

  @Override
  void warnProviderHasNoConsumers(ProviderInfo providerInfo) {

  }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) {

  }

  @Override
  void interactionDescription(Interaction interaction) {

  }

  @Override
  void stateForInteraction(String state, ProviderInfo provider, ConsumerInfo consumer, boolean isSetup) {

  }

  @Override
  void warnStateChangeIgnored(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo) {

  }

  @Override
  @SuppressWarnings('ParameterCount')
  void stateChangeRequestFailedWithException(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo,
                                             boolean isSetup, Exception e, boolean printStackTrace) {

  }

  @Override
  void stateChangeRequestFailed(String state, ProviderInfo providerInfo, boolean isSetup, String httpStatus) {

  }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String state, ProviderInfo providerInfo, boolean isSetup,
                                             Object stateChangeHandler) {

  }

  @Override
  void requestFailed(ProviderInfo providerInfo, Interaction interaction, String interactionMessage, Exception e,
                     boolean printStackTrace) {

  }

  @Override
  void returnsAResponseWhich() {

  }

  @Override
  void statusComparisonOk(int status) {

  }

  @Override
  void statusComparisonFailed(int status, def comparison) {

  }

  @Override
  void includesHeaders() {

  }

  @Override
  void headerComparisonOk(String key, String value) {

  }

  @Override
  void headerComparisonFailed(String key, String value, def comparison) {

  }

  @Override
  void bodyComparisonOk() {

  }

  @Override
  void bodyComparisonFailed(def comparison) {

  }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) {

  }

  @Override
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) {

  }

  @Override
  void generatesAMessageWhich() {

  }

  @Override
  void displayFailures(Map failures) {

  }
}
