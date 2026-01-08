package steps.shared

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.reporters.BaseVerifierReporter
import au.com.dius.pact.provider.reporters.Event
import org.jetbrains.annotations.NotNull

@SuppressWarnings('GetterMethodCouldBeProperty')
class StubVerificationReporter extends BaseVerifierReporter  {
  List<Map<String, Object>> events = []

  @Override
  String getExt() { null }

  @Override
  File getReportDir() { null }

  @Override
  void setReportDir(File file) { }

  @Override
  File getReportFile() { null }

  @Override
  void setReportFile(File file) { }

  @Override
  IProviderVerifier getVerifier() { null }

  @Override
  void setVerifier(IProviderVerifier iProviderVerifier) { }

  @Override
  void initialise(IProviderInfo provider) { }

  @Override
  void finaliseReport() { }

  @Override
  void reportVerificationForConsumer(IConsumerInfo consumer, IProviderInfo provider, String tag) { }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, IConsumerInfo consumer) { }

  @Override
  void verifyConsumerFromFile(PactSource pactFile, IConsumerInfo consumer) { }

  @Override
  void pactLoadFailureForConsumer(IConsumerInfo consumer, String message) { }

  @Override
  void warnProviderHasNoConsumers(IProviderInfo provider) { }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) { }

  @Override
  void interactionDescription(Interaction interaction) { }

  @Override
  void stateForInteraction(String state, IProviderInfo provider, IConsumerInfo consumer, boolean isSetup) { }

  @Override
  void warnStateChangeIgnored(String state, IProviderInfo provider, IConsumerInfo consumer) {
    events << [state: state, provider: provider, consumer: consumer]
  }

  @Override
  void stateChangeRequestFailedWithException(String state, boolean isSetup, Exception e, boolean printStackTrace) { }

  @Override
  void stateChangeRequestFailed(String state, IProviderInfo provider, boolean isSetup, String httpStatus) { }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String s, IProviderInfo p, boolean isSetup, Object stateChangeHandler) { }

  @Override
  void requestFailed(IProviderInfo p, Interaction i, String message, Exception e, boolean printStackTrace) { }

  @Override
  void returnsAResponseWhich() { }

  @Override
  void statusComparisonOk(int status) { }

  @Override
  void statusComparisonFailed(int status, Object comparison) { }

  @Override
  void includesHeaders() { }

  @Override
  void headerComparisonOk(String key, List<String> value) { }

  @Override
  void headerComparisonFailed(String key, List<String> value, Object comparison) { }

  @Override
  void bodyComparisonOk() { }

  @Override
  void bodyComparisonFailed(Object comparison) { }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) { }

  @Override
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) { }

  @Override
  void generatesAMessageWhich() { }

  @Override
  void displayFailures(List<VerificationResult.Failed> failures) { }

  @Override
  void includesMetadata() { }

  @Override
  void metadataComparisonOk() { }

  @Override
  void metadataComparisonOk(String key, Object value) { }

  @Override
  void metadataComparisonFailed(String key, Object value, Object comparison) { }

  @Override
  void receive(@NotNull Event event) {
    switch (event) {
      case Event.DisplayInteractionComments:
        events << [comments: event.comments]
        break
      default:
        super.receive(event)
    }
  }
}
