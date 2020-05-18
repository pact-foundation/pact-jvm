package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import java.util.function.BiConsumer
import java.util.function.Supplier
import org.apache.commons.lang3.tuple.Pair
import org.apache.http.HttpRequest

/**
 * Run [Interaction] and perform response verification
 *
 * @see HttpTarget out-of-the-box implementation
 */
interface Target {
  /**
   * Run [Interaction] and perform response verification
   *
   *
   * Any exception will be caught by caller and reported as test failure
   * @param consumerName consumer name that generated the interaction
   * @param interaction interaction to be tested
   * @param source Source of the Pact interaction
   * @param context Context map for the test
   */
  fun testInteraction(consumerName: String, interaction: Interaction, source: PactSource, context: Map<String, Any>)

  /**
   * Add a callback to receive the test interaction result
   */
  fun addResultCallback(callback: BiConsumer<VerificationResult, IProviderVerifier>)

  /**
   * Add an additional state change handler to look for state change callbacks
   */
  fun withStateHandler(stateHandler: Pair<Class<out Any>, Supplier<out Any>>): Target

  /**
   * Add additional state change handlers to look for state change callbacks
   */
  fun withStateHandlers(vararg stateHandlers: Pair<Class<out Any>, Supplier<out Any>>): Target

  /**
   * Add additional state change handlers to look for state change callbacks
   */
  fun setStateHandlers(stateHandlers: List<Pair<Class<out Any>, Supplier<out Any>>>)

  /**
   * Additional state change handlers to look for state change callbacks
   */
  fun getStateHandlers(): List<Pair<Class<out Any>, Supplier<out Any>>>

  fun getRequestClass(): Class<*> = HttpRequest::class.java
}
