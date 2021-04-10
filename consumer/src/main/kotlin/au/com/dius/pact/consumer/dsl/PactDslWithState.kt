package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState

open class PactDslWithState @JvmOverloads constructor(
  private val consumerPactBuilder: ConsumerPactBuilder,
  var consumerName: String,
  var providerName: String,
  private val defaultRequestValues: PactDslRequestWithoutPath?,
  private val defaultResponseValues: PactDslResponse?,
  val version: PactSpecVersion = PactSpecVersion.V3
) {
  @JvmField
  var state: MutableList<ProviderState> = mutableListOf()
  val comments = mutableListOf<String>()

  internal constructor(
    consumerPactBuilder: ConsumerPactBuilder,
    consumerName: String,
    providerName: String,
    state: ProviderState,
    defaultRequestValues: PactDslRequestWithoutPath?,
    defaultResponseValues: PactDslResponse?,
    version: PactSpecVersion
  ) : this(consumerPactBuilder, consumerName, providerName, defaultRequestValues, defaultResponseValues, version) {
    this.state.add(state)
  }

  /**
   * Description of the request that is expected to be received
   *
   * @param description request description
   */
  fun uponReceiving(description: String): PactDslRequestWithoutPath {
    return PactDslRequestWithoutPath(consumerPactBuilder, this, description, defaultRequestValues,
      defaultResponseValues, version)
  }

  /**
   * Adds another provider state to this interaction
   * @param stateDesc Description of the state
   */
  fun given(stateDesc: String): PactDslWithState {
    state.add(ProviderState(stateDesc))
    return this
  }

  /**
   * Adds another provider state to this interaction
   * @param stateDesc Description of the state
   * @param params State data parameters
   */
  fun given(stateDesc: String, params: Map<String, Any?>): PactDslWithState {
    state.add(ProviderState(stateDesc, params))
    return this
  }

  /**
   * Adds a comment to this interaction
   */
  fun comment(comment: String): PactDslWithState {
    comments.add(comment)
    return this
  }
}
