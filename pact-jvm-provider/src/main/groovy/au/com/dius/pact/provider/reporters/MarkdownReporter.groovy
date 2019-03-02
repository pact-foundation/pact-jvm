package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo

/**
 * Pact verifier reporter that displays the results of the verification in a markdown document
 */
@SuppressWarnings(['DuplicateStringLiteral', 'UnnecessaryObjectReferences', 'MethodCount', 'ParameterName'])
class MarkdownReporter implements VerifierReporter {

  String name
  File reportDir
  File reportFile
  PrintWriter writer
  String ext = '.md'

  @Override
  void initialise(IProviderInfo provider) {
    reportDir.mkdirs()
    reportFile = new File(reportDir, (provider.name + ext))
    writer = reportFile.newPrintWriter()
    writer.println "# $provider.name"
    writer.println()
    writer.println '| Description    | Value |'
    writer.println '| -------------- | ----- |'
    writer.println "| Date Generated | ${new Date()} |"
    writer.println "| Pact Version   | ${BasePact.lookupVersion()} |"
    writer.println()
  }

  @Override
  void finaliseReport() {
    writer.close()
  }

  @Override
  void reportVerificationForConsumer(IConsumerInfo consumer, IProviderInfo provider) {
    writer.println "## Verifying a pact between _${consumer.name}_ and _${provider.name}_"
    writer.println()
  }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, IConsumerInfo consumer) {
    writer.println "From ${pactUrl.description()}"
  }

  @Override
  void verifyConsumerFromFile(PactSource pactFile, IConsumerInfo consumer) {
    writer.println "From ${pactFile.description()}"
    writer.println()
  }

  @Override
  void pactLoadFailureForConsumer(IConsumerInfo IConsumerInfo, String message) { }

  @Override
  void warnProviderHasNoConsumers(IProviderInfo IProviderInfo) { }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) { }

  @Override
  void interactionDescription(Interaction interaction) {
    writer.println "$interaction.description  "
  }

  @Override
  void stateForInteraction(String state, IProviderInfo provider, IConsumerInfo consumer, boolean isSetup) {
    writer.println "Given **$state**  "
  }

  @Override
  void warnStateChangeIgnored(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo) {
    writer.println '&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as there is' +
      ' no stateChange URL</span>  '
  }

  @Override
  @SuppressWarnings('ParameterCount')
  void stateChangeRequestFailedWithException(String state, IProviderInfo IProviderInfo, IConsumerInfo IConsumerInfo,
                                             boolean isSetup, Exception e, boolean printStackTrace) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $e.message</span>"
    writer.println()
    writer.println '```'
    e.printStackTrace(writer)
    writer.println '```'
    writer.println()
  }

  @Override
  void stateChangeRequestFailed(String state, IProviderInfo IProviderInfo, boolean isSetup, String httpStatus) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>State Change Request Failed - $httpStatus</span>  "
  }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String state, IProviderInfo IProviderInfo, boolean isSetup,
                                             Object stateChangeHandler) {
    writer.println '&nbsp;&nbsp;&nbsp;&nbsp;<span style=\'color: yellow\'>WARNING: State Change ignored as there is ' +
      "no stateChange URL, received `$stateChangeHandler`</span>  "
  }

  @Override
  void requestFailed(IProviderInfo IProviderInfo, Interaction interaction, String interactionMessage, Exception e,
                     boolean printStackTrace) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Request Failed - $e.message</span>"
    writer.println()
    writer.println '```'
    e.printStackTrace(writer)
    writer.println '```'
    writer.println()
  }

  @Override
  void returnsAResponseWhich() {
    writer.println '&nbsp;&nbsp;returns a response which  '
  }

  @Override
  void statusComparisonOk(int status) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** (<span style='color:green'>OK</span>)  "
  }

  @Override
  void statusComparisonFailed(int status, def comparison) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;has status code **$status** (<span style='color:red'>FAILED</span>)"
    writer.println()
    writer.println '```'
    if (comparison.hasProperty('message')) {
      writer.println comparison.message
    } else {
      writer.println comparison
    }
    writer.println '```'
    writer.println()
  }

  @Override
  void includesHeaders() {
    writer.println '&nbsp;&nbsp;&nbsp;&nbsp;includes headers  '
  }

  @Override
  void headerComparisonOk(String key, List<String> value) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:green\'>OK</span>)  '
  }

  @Override
  void headerComparisonFailed(String key, List<String> value, def comparison) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"**$key**\" with value \"**$value**\" " +
      '(<span style=\'color:red\'>FAILED</span>)  '
    writer.println()
    writer.println '```'
    writer.println comparison
    writer.println '```'
    writer.println()
  }

  @Override
  void bodyComparisonOk() {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  "
  }

  @Override
  void bodyComparisonFailed(def comparison) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  "
    writer.println()
    writer.println '| Path | Failure |'
    writer.println '| ---- | ------- |'
    if (comparison instanceof String) {
      writer.println "|\$|$comparison|"
    } else if (comparison.comparison instanceof Map) {
      writer.println comparison.comparison.collect { "|$it.key|${it.value*.mismatch.join('; ')}|" }.join('\n')
    } else {
      writer.println "|\$|$comparison.comparison|"
    }
    writer.println()
    if (comparison.diff) {
      writer.println 'Diff:'
      writer.println()
      renderDiff comparison.diff
      writer.println()
    }
  }

  void renderDiff(def diff) {
    writer.println '```diff'
    writer.println diff.join('\n')
    writer.println '```'
  }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) { }

  @Override
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) {
    writer.println "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color: red'>Verification Failed - $e.message</span>"
    writer.println()
    writer.println '```'
    e.printStackTrace(writer)
    writer.println '```'
    writer.println()
  }

  @Override
  void generatesAMessageWhich() {
    writer.println '&nbsp;&nbsp;generates a message which  '
  }

  @Override
  void displayFailures(Map failures) { }
}
