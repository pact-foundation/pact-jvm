package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Pact verifier reporter that displays the results of the verification in a markdown document
 */
@SuppressWarnings(['DuplicateStringLiteral', 'UnnecessaryObjectReferences', 'MethodCount', 'ParameterName'])
class MarkdownReporter implements VerifierReporter {

  String name
  File reportDir
  File reportFile
  String ext = '.md'

  @Override
  void initialise(IProviderInfo provider) {
    reportDir.mkdirs()
    reportFile = new File(reportDir, (provider.name + ext))
    reportFile.append "# $provider.name\n"
    reportFile.append '\n'
    reportFile.append '| Description    | Value |\n'
    reportFile.append '| -------------- | ----- |\n'
    reportFile.append "| Date Generated | ${new Date()} |\n"
    reportFile.append "| Pact Version   | ${BasePact.lookupVersion()} |\n"
    reportFile.append '\n'
  }

  @Override
  void finaliseReport() {

  }

  @Override
  void reportVerificationForConsumer(IConsumerInfo consumer, IProviderInfo provider) {
    reportFile.append "## Verifying a pact between _${consumer.name}_ and _${provider.name}_\n"
    reportFile.append '\n'
  }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, IConsumerInfo consumer) {
    reportFile.append "From ${pactUrl.description()}\n"
  }

  @Override
  void verifyConsumerFromFile(PactSource pactFile, IConsumerInfo consumer) {
    reportFile.append "From ${pactFile.description()}\n"
    reportFile.append '\n'
  }

  @Override
  void pactLoadFailureForConsumer(IConsumerInfo IConsumerInfo, String message) { }

  @Override
  void warnProviderHasNoConsumers(IProviderInfo IProviderInfo) { }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) { }

  @Override
  void interactionDescription(Interaction interaction) {
    reportFile.append "$interaction.description  \n"
  }

  @Override
  void stateForInteraction(String state, IProviderInfo provider, IConsumerInfo consumer, boolean isSetup) {
    reportFile.append "Given **$state**  \n"
  }

  @Override
  void warnStateChangeIgnored(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo) {
    reportFile.append '&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as ' +
      'there is no stateChange URL</span>  \n'
  }

  @Override
  @SuppressWarnings('ParameterCount')
  void stateChangeRequestFailedWithException(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo,
                                             boolean isSetup, Exception e, boolean printStackTrace) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $e.message" +
      '</span>\n'
    reportFile.append '\n'
    reportFile.append '```\n'
    reportFile.withWriterAppend { w -> w.withPrintWriter { e.printStackTrace(it) } }
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void stateChangeRequestFailed(String state, IProviderInfo IProviderInfo, boolean isSetup, String httpStatus) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $httpStatus" +
      '</span>  \n'
  }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String state, IProviderInfo IProviderInfo, boolean isSetup,
                                             Object stateChangeHandler) {
    reportFile.append '&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as ' +
      "there is no stateChange URL, received `$stateChangeHandler`</span>  \n"
  }

  @Override
  void requestFailed(IProviderInfo IProviderInfo, Interaction interaction, String interactionMessage, Exception e,
                     boolean printStackTrace) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Request Failed - $e.message</span>\n"
    reportFile.append '\n'
    reportFile.append '```\n'
    reportFile.withWriterAppend { w -> w.withPrintWriter { e.printStackTrace(it) } }
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void returnsAResponseWhich() {
    reportFile.append '&nbsp;&nbsp;returns a response which  \n'
  }

  @Override
  void statusComparisonOk(int status) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** (<span style='color:green'>OK</span>)  \n"
  }

  @Override
  void statusComparisonFailed(int status, def comparison) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** (<span style='color:red'>FAILED</span>)\n"
    reportFile.append '\n'
    reportFile.append '```\n'
    if (comparison.hasProperty('message')) {
      reportFile.append comparison.message
    } else {
      reportFile.append comparison
    }
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void includesHeaders() {
    reportFile.append '&nbsp;&nbsp;&nbsp;&nbsp;includes headers  \n'
  }

  @Override
  void headerComparisonOk(String key, List<String> value) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:green\'>OK</span>)  \n'
  }

  @Override
  void headerComparisonFailed(String key, List<String> value, def comparison) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:red\'>FAILED</span>)  \n'
    reportFile.append '\n'
    reportFile.append '```\n'
    reportFile.append comparison
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void bodyComparisonOk() {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  \n"
  }

  @Override
  void bodyComparisonFailed(def comparison) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  \n"
    reportFile.append '\n'
    reportFile.append '| Path | Failure |\n'
    reportFile.append '| ---- | ------- |\n'
    if (comparison instanceof String) {
      reportFile.append "|\$|$comparison|\n"
    } else if (comparison.comparison instanceof Map) {
      reportFile.append comparison.comparison.collect { "|$it.key|${it.value*.mismatch.join('; ')}|" }.join('\n')
    } else {
      reportFile.append "|\$|$comparison.comparison|"
    }
    reportFile.append '\n\n'
    if (comparison.diff) {
      reportFile.append 'Diff:\n'
      reportFile.append '\n'
      renderDiff comparison.diff
      reportFile.append '\n\n'
    }
  }

  void renderDiff(def diff) {
    reportFile.append '```diff\n'
    reportFile.append diff.join('\n')
    reportFile.append '\n```\n'
  }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) { }

  @Override
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Verification Failed - $e.message</span>\n"
    reportFile.append '\n'
    reportFile.append '```\n'
    reportFile.withWriterAppend { w -> w.withPrintWriter { e.printStackTrace(it) } }
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void generatesAMessageWhich() {
    reportFile.append '&nbsp;&nbsp;generates a message which  \n'
  }

  @Override
  void displayFailures(Map failures) { }

  @Override
  void metadataComparisonFailed(String key, def value, def comparison) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:red\'>FAILED</span>)  \n'
    reportFile.append '\n'
    reportFile.append '```\n'
    reportFile.append comparison
    reportFile.append '\n```\n'
    reportFile.append '\n'
  }

  @Override
  void includesMetadata() {
    reportFile.append '&nbsp;&nbsp;&nbsp;&nbsp;includes metadata  \n'
  }

  @Override
  void metadataComparisonOk(@NotNull String key, @Nullable Object value) {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:green\'>OK</span>)  \n'
  }

  @Override
  void metadataComparisonOk() {
    reportFile.append "&nbsp;&nbsp;&nbsp;&nbsp;has matching metadata (<span style='color:green'>OK</span>)\n"
  }
}
