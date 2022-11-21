package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.pact.plugins.jvm.core.InteractionContents
import mu.KLogging

/**
 * Content matcher that delegates to a plugin
 */
class PluginContentMatcher(
  val contentMatcher: io.pact.plugins.jvm.core.ContentMatcher,
  val contentType: ContentType
) : ContentMatcher {
  override fun matchBody(expected: OptionalBody, actual: OptionalBody, context: MatchingContext): BodyMatchResult {
    logger.debug { "matchBody: context=$context" }
    val result = contentMatcher.invokeContentMatcher(expected, actual, context.allowUnexpectedKeys,
      context.matchers.matchingRules, context.pluginConfiguration)
    val bodyResults = result.entries.map { mismatch ->
      BodyItemMatchResult(mismatch.key, mismatch.value.map {
        BodyMismatch(it.expected, it.actual, it.mismatch, it.path, it.diff)
      })
    }
    return BodyMatchResult(null, bodyResults)
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Result<List<InteractionContents>, String> {
    return when (val result = contentMatcher.configureContent(contentType.toString(), bodyConfig)) {
      is Ok -> Result.Ok(result.value)
      is Err -> Result.Err(result.error)
    }
  }

  companion object : KLogging()
}
