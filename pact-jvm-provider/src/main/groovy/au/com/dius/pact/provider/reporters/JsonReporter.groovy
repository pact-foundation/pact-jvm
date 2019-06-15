package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.FileSource
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Pact verifier reporter that generates the results of the verification in JSON format
 */
@SuppressWarnings(['MethodCount', 'ParameterName'])
class JsonReporter implements VerifierReporter {

  private static final REPORT_FORMAT = '0.0.0'
  private static final FAILED = 'failed'

  String name
  File reportDir
  File reportFile
  def jsonData
  String ext = '.json'

  @Override
  void initialise(IProviderInfo provider) {
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
    if (reportFile.exists() && reportFile.length() > 0) {
      def existingContents = new JsonSlurper().parse(reportFile)
      if (jsonData.provider.name == existingContents?.provider?.name) {
        existingContents.metaData = jsonData.metaData
        existingContents.execution.addAll(jsonData.execution)
        reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(existingContents))
      } else {
        reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonData))
      }
    } else {
      reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonData))
    }
  }

  @Override
  void reportVerificationForConsumer(IConsumerInfo consumer, IProviderInfo provider) {
    jsonData.execution << [
      consumer: [
        name: consumer.name
      ],
      interactions: []
    ]
  }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, IConsumerInfo consumer) {
    jsonData.execution.last().consumer.source = [
      url: pactUrl.url
    ]
  }

  @Override
  void verifyConsumerFromFile(PactSource pactFile, IConsumerInfo consumer) {
    jsonData.execution.last().consumer.source = [
      file: pactFile instanceof FileSource ? pactFile.file : pactFile.description()
    ]
  }

  @Override
  void pactLoadFailureForConsumer(IConsumerInfo IConsumerInfo, String message) {
    jsonData.execution.last().result = [
      state: 'Pact Load Failure',
      message: message
    ]
  }

  @Override
  void warnProviderHasNoConsumers(IProviderInfo IProviderInfo) { }

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
  void stateForInteraction(String state, IProviderInfo provider, IConsumerInfo consumer, boolean isSetup) { }

  @Override
  void warnStateChangeIgnored(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo) { }

  @Override
  @SuppressWarnings('ParameterCount')
  void stateChangeRequestFailedWithException(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo,
                                             boolean isSetup, Exception e, boolean printStackTrace) {

  }

  @Override
  void stateChangeRequestFailed(String state, IProviderInfo IProviderInfo, boolean isSetup, String httpStatus) {

  }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String state, IProviderInfo IProviderInfo, boolean isSetup,
                                             Object stateChangeHandler) { }

  @Override
  void requestFailed(IProviderInfo IProviderInfo, Interaction interaction, String interactionMessage, Exception e,
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
    if (comparison.hasProperty('message')) {
      comparison.message.eachLine { verification.status << it }
    } else {
      comparison.toString().eachLine { verification.status << it }
    }
  }

  @Override
  void includesHeaders() { }

  @Override
  void headerComparisonOk(String key, List<String> value) { }

  @Override
  void headerComparisonFailed(String key, List<String> value, def comparison) {
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

  @Override
  void metadataComparisonFailed(String key, def value, def comparison) {
    def verification = jsonData.execution.last().interactions.last().verification
    verification.result = FAILED
    verification.metadata = verification.metadata ?: [:]
    verification.metadata[key] = comparison
  }

  @Override
  void includesMetadata() { }

  @Override
  void metadataComparisonOk(@NotNull String key, @Nullable Object value) { }

  @Override
  void metadataComparisonOk() { }
}
