package au.com.dius.pact.provider

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapEither
import com.github.michaelbull.result.unwrap
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import groovy.lang.Closure
import mu.KLogging
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

data class StateChangeResult @JvmOverloads constructor (
  val stateChangeResult: Result<Map<String, Any?>, Exception>,
  val message: String = ""
)

interface StateChange {
  fun executeStateChange(
    verifier: IProviderVerifier,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    providerClient: ProviderClient
  ): StateChangeResult

  fun stateChange(
    verifier: IProviderVerifier,
    state: ProviderState,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    providerClient: ProviderClient
  ): Result<Map<String, Any?>, Exception>

  fun executeStateChangeTeardown(
    verifier: IProviderVerifier,
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    providerClient: ProviderClient
  )
}

/**
 * Class containing all the state change logic
 */
object DefaultStateChange : StateChange, KLogging() {

  override fun executeStateChange(
    verifier: IProviderVerifier,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    interaction: Interaction,
    interactionMessage: String,
    failures: MutableMap<String, Any>,
    providerClient: ProviderClient
  ): StateChangeResult {
    var message = interactionMessage
    var stateChangeResult: Result<Map<String, Any?>, Exception> = Ok(emptyMap())

    if (interaction.providerStates.isNotEmpty()) {
      val iterator = interaction.providerStates.iterator()
      var first = true
      while (stateChangeResult is Ok && iterator.hasNext()) {
        val providerState = iterator.next()
        val result = stateChange(verifier, providerState, provider, consumer, true, providerClient)
        logger.debug { "State Change: \"$providerState\" -> $result" }

        stateChangeResult = result.mapEither({
          if (first) {
            message += " Given ${providerState.name}"
            first = false
          } else {
            message += " And ${providerState.name}"
          }
          stateChangeResult.unwrap().plus(it)
        }, {
          failures[message] = it.message.toString()
          it
        })
      }
    }

    return StateChangeResult(stateChangeResult, message)
  }

  @Suppress("TooGenericExceptionCaught")
  override fun stateChange(
    verifier: IProviderVerifier,
    state: ProviderState,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    providerClient: ProviderClient
  ): Result<Map<String, Any?>, Exception> {
    verifier.reportStateForInteraction(state.name.toString(), provider, consumer, isSetup)
    try {
      var stateChangeHandler = consumer.stateChange
      var stateChangeUsesBody = consumer.stateChangeUsesBody
      if (stateChangeHandler == null) {
        stateChangeHandler = provider.stateChangeUrl
        stateChangeUsesBody = provider.stateChangeUsesBody
      }
      if (stateChangeHandler == null || (stateChangeHandler is String && stateChangeHandler.isBlank())) {
        verifier.reporters.forEach { it.warnStateChangeIgnored(state.name.toString(), provider, consumer) }
        return Ok(emptyMap())
      } else if (verifier.checkBuildSpecificTask.apply(stateChangeHandler)) {
        logger.debug { "Invoking build specific task $stateChangeHandler" }
        verifier.executeBuildSpecificTask.accept(stateChangeHandler, state)
        return Ok(emptyMap())
      } else if (stateChangeHandler is Closure<*>) {
        val result = if (provider.stateChangeTeardown) {
          stateChangeHandler.call(state, if (isSetup) "setup" else "teardown")
        } else {
          stateChangeHandler.call(state)
        }
        logger.debug { "Invoked state change closure -> $result" }
        if (result !is URL) {
          return Ok(if (result is Map<*, *>) result as Map<String, Any> else emptyMap())
        }
        stateChangeHandler = result
      }
      return executeHttpStateChangeRequest(verifier, stateChangeHandler, stateChangeUsesBody, state, provider, isSetup,
        providerClient)
    } catch (e: Exception) {
      verifier.reporters.forEach {
        it.stateChangeRequestFailedWithException(state.name.toString(), provider, consumer, isSetup, e,
          verifier.projectHasProperty.apply(ProviderVerifier.PACT_SHOW_STACKTRACE))
      }
      return Err(e)
    }
  }

  override fun executeStateChangeTeardown(
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
  ): Result<Map<String, Any?>, Exception> {
    return try {
      val url = stateChangeHandler as? URI ?: URI(stateChangeHandler.toString())
      val response = providerClient.makeStateChangeRequest(url, state, useBody, isSetup, provider.stateChangeTeardown)
      logger.debug { "Invoked state change $url -> ${response?.statusLine}" }
      response?.use {
        if (response.statusLine.statusCode >= 400) {
          verifier.reporters.forEach {
            it.stateChangeRequestFailed(state.name.toString(), provider, isSetup, response.statusLine.toString())
          }
          Err(Exception("State Change Request Failed - ${response.statusLine}"))
        } else {
          parseJsonResponse(response.entity)
        }
      } ?: Ok(emptyMap())
    } catch (ex: URISyntaxException) {
      verifier.reporters.forEach {
        it.warnStateChangeIgnoredDueToInvalidUrl(state.name.toString(), provider, isSetup, stateChangeHandler)
      }
      Ok(emptyMap())
    }
  }

  private fun parseJsonResponse(entity: HttpEntity?): Result<Map<String, Any?>, Exception> {
    return if (entity != null && ContentType.get(entity).mimeType == ContentType.APPLICATION_JSON.mimeType) {
      val body = EntityUtils.toString(entity)
      Ok(Json.toMap(JsonParser.parseString(body)))
    } else {
      Ok(emptyMap())
    }
  }
}
