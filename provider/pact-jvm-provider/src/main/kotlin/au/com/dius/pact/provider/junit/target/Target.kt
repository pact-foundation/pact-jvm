package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.IProviderVerifier
import java.util.function.BiConsumer

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
  fun addResultCallback(callback: BiConsumer<Boolean, IProviderVerifier>)
}
