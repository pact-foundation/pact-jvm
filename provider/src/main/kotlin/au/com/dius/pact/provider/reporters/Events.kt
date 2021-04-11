package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.provider.BodyComparisonResult
import com.github.michaelbull.result.Result

sealed class Event {
  data class ErrorHasNoAnnotatedMethodsFoundForInteraction(val interaction: Interaction) : Event()
  data class VerificationFailed(val interaction: Interaction, val e: Exception, val showStacktrace: Boolean): Event()
  object BodyComparisonOk: Event()
  data class BodyComparisonFailed(val comparison: Result<BodyComparisonResult, BodyTypeMismatch>): Event()
  object GeneratesAMessageWhich: Event()
  data class MetadataComparisonOk(val key: String? = null, val mismatches: Any? = null): Event()
  object IncludesMetadata: Event()
  data class MetadataComparisonFailed(val key: String, val value: Any?, val comparison: Any): Event()
  data class InteractionDescription(val interaction: Interaction): Event()
  data class DisplayInteractionComments(val comments: Map<String, JsonValue>) : Event()
}
