package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonException
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.KafkaSchemaRegistryWireFormatter
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging

class KafkaJsonSchemaContentMatcher() : BodyMatcher {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    allowUnexpectedKeys: Boolean,
    matchingRules: MatchingRules
  ): BodyMatchResult {

    val raw = removeMagicBytes(actual)

    if (isInvalidActualValue(expected, raw))
      return getInvalidActualJsonResult(expected, raw)

    return JsonBodyMatcher.matchBody(expected, raw, allowUnexpectedKeys, matchingRules)
  }

  private fun removeMagicBytes(optionalBody: OptionalBody): OptionalBody {
    return optionalBody.copy(value = KafkaSchemaRegistryWireFormatter.removeMagicBytes(optionalBody.value))
  }

  private fun isInvalidActualValue(
      expected: OptionalBody,
      decodedActualOptionalBody: OptionalBody
  ) = expected.isPresent() && !isValidJson(decodedActualOptionalBody.value)

  private fun getInvalidActualJsonResult(
      expected: OptionalBody,
      actual: OptionalBody
  ) = BodyMatchResult(
    null, listOf(
      BodyItemMatchResult(
        "$",
          listOf(
            BodyMismatch(
              expected.valueAsString(),
              actual.valueAsString(),
              "Expected json body but received '${actual.valueAsString()}'"
            )
          )
      )
    )
  )

  private fun isValidJson(value: ByteArray?): Boolean {
    if(value == null)
      return false

    return value.isEmpty() || isParsableJson(value)
  }

  private fun isParsableJson(value: ByteArray): Boolean = try {
    JsonParser.parseString(String(value))
    true
  } catch (e: JsonException) {
    logger.debug("Swallowed Exception deliberately", e)
    false
  }

  companion object : KLogging()
}
