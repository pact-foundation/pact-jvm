package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import groovy.json.JsonOutput
import org.apache.commons.lang3.exception.ExceptionUtils

/**
 * Pact verifier reporter that generates the results of the verification in JSON format
 */
@SuppressWarnings('MethodCount')
class JsonReporter implements VerifierReporter {

  private static final REPORT_FORMAT = '0.0.0'
  private static final FAILED = 'failed'

  String name
  File reportDir
  File reportFile
  def jsonData
  String ext = '.json'

  @Override
  void initialise(ProviderInfo provider) {
    jsonData = [
      metaData: [
        date: new Date(),
        pactJvmVersion: BasePact.lookupVersion(),
        reportFormat: REPORT_FORMAT
      ],
      provider: [
        name: provider.name
      ],
      execution: []
    ]
    reportDir.mkdirs()
    reportFile = new File(reportDir, (provider.name + ext))
  }

  @Override
  void finaliseReport() {
    reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonData))
  }

  @Override
  void reportVerificationForConsumer(ConsumerInfo consumer, ProviderInfo provider) {
    jsonData.execution << [
      consumer: [
        name: consumer.name
      ],
      interactions: []
    ]
  }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, ConsumerInfo consumer) {
    jsonData.execution.last().consumer.source = [
      url: pactUrl.url
    ]
  }

  @Override
  void verifyConsumerFromFile(PactSource pactFile, ConsumerInfo consumer) {
    jsonData.execution.last().consumer.source = [
      file: pactFile instanceof FileSource ? pactFile.file : pactFile.description()
    ]
  }

  @Override
  void pactLoadFailureForConsumer(ConsumerInfo consumerInfo, String message) {
    jsonData.execution.last().result = [
      state: 'Pact Load Failure',
      message: message
    ]
  }

  @Override
  void warnProviderHasNoConsumers(ProviderInfo providerInfo) { }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) { }

  @Override
  void interactionDescription(Interaction interaction) {
    jsonData.execution.last().interactions << [
      interaction: interaction.toMap(PactSpecVersion.V3),
      verification: [
        result: 'OK'
      ]
    ]
  }

  @Override
  void stateForInteraction(String state, ProviderInfo provider, ConsumerInfo consumer, boolean isSetup) { }

  @Override
  void warnStateChangeIgnored(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo) { }

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
                                             Object stateChangeHandler) { }

  @Override
  void requestFailed(ProviderInfo providerInfo, Interaction interaction, String interactionMessage, Exception e,
                     boolean printStackTrace) {
    jsonData.execution.last().interactions.last().verification = [
      result: FAILED,
      message: interactionMessage,
      exception: [
        message: e.message,
        stackTrace: ExceptionUtils.getStackFrames(e)
      ]
    ]
  }

  @Override
  void returnsAResponseWhich() { }

  @Override
  void statusComparisonOk(int status) { }

  @Override
  void statusComparisonFailed(int status, def comparison) {
    def verification = jsonData.execution.last().interactions.last().verification
    verification.result = FAILED
    verification.status = []
    comparison.message.eachLine { verification.status << it }
  }

  @Override
  void includesHeaders() { }

  @Override
  void headerComparisonOk(String key, String value) { }

  @Override
  void headerComparisonFailed(String key, String value, def comparison) {
    def verification = jsonData.execution.last().interactions.last().verification
    verification.result = FAILED
    verification.header = verification.header ?: [:]
    verification.header[key] = comparison
  }

  @Override
  void bodyComparisonOk() { }

  @Override
  void bodyComparisonFailed(def comparison) {
    def verification = jsonData.execution.last().interactions.last().verification
    verification.result = FAILED
    verification.body = comparison
  }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) {
    jsonData.execution.last().interactions.last().verification = [
      result: FAILED,
      cause: [
        message: 'No Annotated Methods Found For Interaction'
      ]
    ]
  }

  @Override
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) {
    jsonData.execution.last().interactions.last().verification = [
      result: FAILED,
      exception: [
        message: e.message,
        stackTrace: ExceptionUtils.getStackFrames(e)
      ]
    ]
  }

  @Override
  void generatesAMessageWhich() { }

  @Override
  void displayFailures(Map failures) { }
}
