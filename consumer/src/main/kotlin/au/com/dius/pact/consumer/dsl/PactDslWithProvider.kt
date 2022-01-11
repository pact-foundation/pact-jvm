package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.json.JsonValue

open class PactDslWithProvider @JvmOverloads constructor(
  val consumerPactBuilder: ConsumerPactBuilder,
  private val providerName: String,
  val version: PactSpecVersion = PactSpecVersion.V3
) {
  private var defaultRequestValues: PactDslRequestWithoutPath? = null
  private var defaultResponseValues: PactDslResponse? = null
  private val additionalMetadata: MutableMap<String, JsonValue> = mutableMapOf()

  /**
   * Describe the state the provider needs to be in for the pact test to be verified.
   *
   * @param state Provider state
   */
  fun given(state: String): PactDslWithState {
    return PactDslWithState(consumerPactBuilder, consumerPactBuilder.consumerName, providerName,
      ProviderState(state), defaultRequestValues, defaultResponseValues, version, additionalMetadata)
  }

  /**
   * Describe the state the provider needs to be in for the pact test to be verified.
   *
   * @param state Provider state
   * @param params Data parameters for the state
   */
  fun given(state: String, params: Map<String, Any?>): PactDslWithState {
    return PactDslWithState(consumerPactBuilder, consumerPactBuilder.consumerName, providerName,
      ProviderState(state, params), defaultRequestValues, defaultResponseValues, version, additionalMetadata)
  }

  /**
   * Describe the state the provider needs to be in for the pact test to be verified.
   *
   * @param firstKey Key of first parameter element
   * @param firstValue Value of first parameter element
   * @param paramsKeyValuePair Additional parameters in key-value pairs
   */
  fun given(state: String, firstKey: String, firstValue: Any?, vararg paramsKeyValuePair: Any): PactDslWithState {
    require(paramsKeyValuePair.size % 2 == 0) {
      "Pair key value should be provided, but there is one key without value."
    }
    val params = mutableMapOf(firstKey to firstValue)
    var i = 0
    while (i < paramsKeyValuePair.size) {
      params[paramsKeyValuePair[i].toString()] = paramsKeyValuePair[i + 1]
      i += 2
    }
    return PactDslWithState(consumerPactBuilder, consumerPactBuilder.consumerName, providerName,
      ProviderState(state, params), defaultRequestValues, defaultResponseValues, version, additionalMetadata)
  }

  /**
   * Description of the request that is expected to be received
   *
   * @param description request description
   */
  fun uponReceiving(description: String): PactDslRequestWithoutPath {
    return PactDslWithState(consumerPactBuilder, consumerPactBuilder.consumerName, providerName,
      defaultRequestValues, defaultResponseValues, version, additionalMetadata)
      .uponReceiving(description)
  }

  fun setDefaultRequestValues(defaultRequestValues: PactDslRequestWithoutPath) {
    this.defaultRequestValues = defaultRequestValues
  }

  fun setDefaultResponseValues(defaultResponseValues: PactDslResponse) {
    this.defaultResponseValues = defaultResponseValues
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: String): PactDslWithProvider {
    additionalMetadata[key] = JsonValue.StringValue(value)
    return this
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: JsonValue): PactDslWithProvider {
    additionalMetadata[key] = value
    return this
  }
}
