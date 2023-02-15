package au.com.dius.pact.provider

import au.com.dius.pact.core.model.OptionalBody
import io.pact.plugins.jvm.core.InteractionVerificationData

/**
 * Request data that is going to be used by the plugin to create the request to be verified
 */
interface RequestData {
  /**
   * Data for the request of the interaction
   */
  val requestData: OptionalBody

  /**
   * Metadata associated with the request
   */
  val metadata: Map<String, Any?>
}

/**
 * Data used by a plugin to create a request to be verified
 */
data class RequestDataToBeVerified(
  /**
   * Data for the request of the interaction
   */
  override val requestData: OptionalBody,

  /**
   * Metadata associated with the request
   */
  override val metadata: Map<String, Any?>
): RequestData {
  constructor(requestData: InteractionVerificationData) : this(requestData.requestData, requestData.metadata)

  fun asInteractionVerificationData() = InteractionVerificationData(requestData, metadata)
}
