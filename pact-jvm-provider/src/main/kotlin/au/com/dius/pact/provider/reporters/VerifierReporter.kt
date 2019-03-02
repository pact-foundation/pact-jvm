package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import java.io.File

/**
 * Interface to verification reporters that can hook into the events of the PactVerifier
 */
interface VerifierReporter {
  /**
   * The extension for the reporter
   */
  val ext: String?

  fun setReportDir(reportDir: File)
  fun setReportFile(reportFile: File)

  fun initialise(provider: IProviderInfo)
  fun finaliseReport()
  fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo)
  fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo)
  fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo)
  fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String)
  fun warnProviderHasNoConsumers(provider: IProviderInfo)
  fun warnPactFileHasNoInteractions(pact: Pact<Interaction>)
  fun interactionDescription(interaction: Interaction)
  fun stateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean)
  fun warnStateChangeIgnored(state: String, IProviderInfo: IProviderInfo, IConsumerInfo: IConsumerInfo)
  fun stateChangeRequestFailedWithException(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  )
  fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String)
  fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  )
  fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  )
  fun returnsAResponseWhich()
  fun statusComparisonOk(status: Int)
  fun statusComparisonFailed(status: Int, comparison: Any)
  fun includesHeaders()
  fun headerComparisonOk(key: String, value: List<String>)
  fun headerComparisonFailed(key: String, value: List<String>, comparison: Any)
  fun bodyComparisonOk()
  fun bodyComparisonFailed(comparison: Any)
  fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction)
  fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean)
  fun generatesAMessageWhich()
  fun displayFailures(failures: Map<String, Any>)
}
