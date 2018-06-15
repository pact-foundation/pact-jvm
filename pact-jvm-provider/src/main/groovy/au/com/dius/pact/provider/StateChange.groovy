package au.com.dius.pact.provider

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.ProviderState
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

/**
 * Class containing all the state change logic
 */
@Slf4j
class StateChange {

  @Canonical
  static class StateChangeResult {
    def stateChangeOk
    String message
  }

  @SuppressWarnings('ParameterCount')
  static StateChangeResult executeStateChange(ProviderVerifier verifier, ProviderInfo provider, ConsumerInfo consumer,
                                              Interaction interaction, String interactionMessage, Map failures,
                                              ProviderClient providerClient) {
    def stateChangeOk = true
    if (interaction.providerStates) {
      def iter = interaction.providerStates.iterator()
      boolean first = true
      while (stateChangeOk && iter.hasNext()) {
        def providerState = iter.next()
        stateChangeOk = stateChange(verifier, providerState, provider, consumer, true, providerClient)
        log.debug "State Change: \"${providerState}\" -> ${stateChangeOk}"
        if (!stateChangeOk) {
          failures[interactionMessage] = stateChangeOk
        } else if (first) {
          interactionMessage += " Given ${providerState.name}"
          first = false
        } else {
          interactionMessage += " And ${providerState.name}"
        }
      }
    }
    new StateChangeResult(stateChangeOk, interactionMessage)
  }

  @SuppressWarnings('ParameterCount')
  static stateChange(ProviderVerifier verifier, ProviderState state, ProviderInfo provider,
                     ConsumerInfo consumer, boolean isSetup, ProviderClient providerClient) {
    verifier.reportStateForInteraction(state.name, provider, consumer, isSetup)
    try {
      def stateChangeHandler = consumer.stateChange
      def stateChangeUsesBody = consumer.stateChangeUsesBody
      if (stateChangeHandler == null) {
        stateChangeHandler = provider.stateChangeUrl
        stateChangeUsesBody = provider.stateChangeUsesBody
      }
      if (stateChangeHandler == null || (stateChangeHandler instanceof String
        && StringUtils.isBlank(stateChangeHandler))) {
        verifier.reporters.each { it.warnStateChangeIgnored(state.name, provider, consumer) }
        return true
      } else if (stateChangeHandler instanceof Closure) {
        def result
        if (provider.stateChangeTeardown) {
          result = stateChangeHandler.call(state, isSetup ? 'setup' : 'teardown')
        } else {
          result = stateChangeHandler.call(state)
        }
        log.debug "Invoked state change closure -> ${result}"
        if (!(result instanceof URL)) {
          return result
        }
        stateChangeHandler = result
      } else if (verifier.isBuildSpecificTask.apply(stateChangeHandler)) {
        log.debug "Invokeing build specific task ${stateChangeHandler}"
        verifier.executeBuildSpecificTask.accept(stateChangeHandler, state)
        return true
      }
      return executeHttpStateChangeRequest(verifier, stateChangeHandler, stateChangeUsesBody, state, provider, isSetup,
        providerClient)
    } catch (e) {
      verifier.reporters.each {
        it.stateChangeRequestFailedWithException(state.name, provider, consumer, isSetup, e,
          verifier.projectHasProperty.apply(verifier.PACT_SHOW_STACKTRACE))
      }
      return e
    }
  }

  static void executeStateChangeTeardown(ProviderVerifier verifier, Interaction interaction, ProviderInfo provider,
                                         ConsumerInfo consumer, ProviderClient providerClient) {
    for (ProviderState providerState: interaction.providerStates) {
      stateChange(verifier, providerState, provider, consumer, false, providerClient)
    }
  }

  @SuppressWarnings('ParameterCount')
  private static executeHttpStateChangeRequest(ProviderVerifier verifier, stateChangeHandler, useBody,
                                               ProviderState state, ProviderInfo provider, boolean isSetup,
                                               ProviderClient providerClient) {
    try {
      def url = stateChangeHandler instanceof URI ? stateChangeHandler
        : new URI(stateChangeHandler.toString())
      def response = providerClient.makeStateChangeRequest(url, state, useBody, isSetup, provider.stateChangeTeardown)
      log.debug "Invoked state change $url -> ${response?.statusLine}"
      if (response) {
        try {
          if (response.statusLine.statusCode >= 400) {
            verifier.reporters.each {
              it.stateChangeRequestFailed(state.name, provider, isSetup, response.statusLine.toString())
            }
            return 'State Change Request Failed - ' + response.statusLine.toString()
          }
        } finally {
          response.close()
        }
      }
    } catch (URISyntaxException ex) {
      verifier.reporters.each {
        it.warnStateChangeIgnoredDueToInvalidUrl(state.name, provider, isSetup, stateChangeHandler)
      }
    }
    true
  }
}
