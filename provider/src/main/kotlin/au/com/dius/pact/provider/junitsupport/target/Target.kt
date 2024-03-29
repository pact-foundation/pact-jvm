package au.com.dius.pact.provider.junitsupport.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import java.util.function.BiConsumer
import java.util.function.Supplier
import org.apache.commons.lang3.tuple.Pair
import org.apache.hc.core5.http.HttpRequest

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
   * @param pending if the Pact or Interaction is pending
   */
  fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: MutableMap<String, Any>,
    pending: Boolean
  )

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

  fun configureVerifier(source: PactSource, consumerName: String, interaction: Interaction)

  /**
   * If this target can verify the interaction
   */
  fun validForInteraction(interaction: Interaction): Boolean

  val verifier: IProviderVerifier
}
