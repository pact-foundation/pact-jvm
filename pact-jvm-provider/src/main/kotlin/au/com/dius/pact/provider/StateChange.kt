package au.com.dius.pact.provider

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.ProviderState
import groovy.lang.Closure
import mu.KLogging
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

data class StateChangeResult @JvmOverloads constructor (val stateChangeOk: Any, val message: String = "")

/**
 * Class containing all the state change logic
 */
object StateChange : KLogging() {

  @JvmStatic
  fun executeStateChange(
    verifier: IProviderVerifier,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    providerClient: ProviderClient
  ): StateChangeResult {
    var message = interactionMessage
    var stateChangeOk: Any = true

    if (interaction.providerStates.isNotEmpty()) {
      val iterator = interaction.providerStates.iterator()
      var first = true
      while (stateChangeOk is Boolean && stateChangeOk && iterator.hasNext()) {
        val providerState = iterator.next()
        stateChangeOk = stateChange(verifier, providerState, provider, consumer, true, providerClient)
        logger.debug { "State Change: \"$providerState\" -> $stateChangeOk" }
        if (stateChangeOk !is Boolean || !stateChangeOk) {
          failures[message] = stateChangeOk
        } else if (first) {
          message += " Given ${providerState.name}"
          first = false
        } else {
          message += " And ${providerState.name}"
        }
      }
    }

    return StateChangeResult(stateChangeOk, message)
  }

  @JvmStatic
  fun stateChange(
    verifier: IProviderVerifier,
    state: ProviderState,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    providerClient: ProviderClient
  ): Any {
    verifier.reportStateForInteraction(state.name, provider, consumer, isSetup)
    try {
      var stateChangeHandler = consumer.stateChange
      var stateChangeUsesBody = consumer.stateChangeUsesBody
      if (stateChangeHandler == null) {
        stateChangeHandler = provider.stateChangeUrl
        stateChangeUsesBody = provider.stateChangeUsesBody
      }
      if (stateChangeHandler == null || (stateChangeHandler is String && stateChangeHandler.isBlank())) {
        verifier.reporters.forEach { it.warnStateChangeIgnored(state.name, provider, consumer) }
        return true
      } else if (stateChangeHandler is Closure<*>) {
        val result = if (provider.stateChangeTeardown) {
          stateChangeHandler.call(state, if (isSetup) "setup" else "teardown")
        } else {
          stateChangeHandler.call(state)
        }
        logger.debug { "Invoked state change closure -> $result" }
        if (result !is URL) {
          return result
        }
        stateChangeHandler = result
      } else if (verifier.checkBuildSpecificTask.apply(stateChangeHandler)) {
        logger.debug { "Invoking build specific task $stateChangeHandler" }
        verifier.executeBuildSpecificTask.accept(stateChangeHandler, state)
        return true
      }
      return executeHttpStateChangeRequest(verifier, stateChangeHandler, stateChangeUsesBody, state, provider, isSetup,
        providerClient)
    } catch (e: Exception) {
      verifier.reporters.forEach {
        it.stateChangeRequestFailedWithException(state.name, provider, consumer, isSetup, e,
          verifier.projectHasProperty.apply(ProviderVerifierBase.PACT_SHOW_STACKTRACE))
      }
      return e
    }
  }

  @JvmStatic
  fun executeStateChangeTeardown(
    verifier: IProviderVerifier,
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    providerClient: ProviderClient
  ) {
    interaction.providerStates.forEach {
      stateChange(verifier, it, provider, consumer, false, providerClient)
    }
  }

  private fun executeHttpStateChangeRequest(
    verifier: IProviderVerifier,
    stateChangeHandler: Any,
    useBody: Boolean,
    state: ProviderState,
    provider: IProviderInfo,
    isSetup: Boolean,
    providerClient: ProviderClient
  ): Any {
    try {
      val url = stateChangeHandler as? URI ?: URI(stateChangeHandler.toString())
      val response = providerClient.makeStateChangeRequest(url, state, useBody, isSetup, provider.stateChangeTeardown)
      logger.debug { "Invoked state change $url -> ${response?.statusLine}" }
      response?.use {
        if (response.statusLine.statusCode >= 400) {
          verifier.reporters.forEach {
            it.stateChangeRequestFailed(state.name, provider, isSetup, response.statusLine.toString())
          }
          return "State Change Request Failed - ${response.statusLine}"
        }
      }
    } catch (ex: URISyntaxException) {
      verifier.reporters.forEach {
        it.warnStateChangeIgnoredDueToInvalidUrl(state.name, provider, isSetup, stateChangeHandler)
      }
    }
    return true
  }
}
