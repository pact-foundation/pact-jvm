package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.Result
import io.pact.plugins.jvm.core.InteractionContents
import io.pact.plugins.jvm.core.TestContext
import io.github.oshai.kotlinlogging.KLogging
import java.util.UUID

/**
 * Content matcher that delegates to a plugin
 */
class PluginContentMatcher(
  val contentMatcher: io.pact.plugins.jvm.core.ContentMatcher,
  val contentType: ContentType
) : ContentMatcher {
  override fun matchBody(expected: OptionalBody, actual: OptionalBody, context: MatchingContext): BodyMatchResult {
    logger.debug { "matchBody: context=$context" }
    if (TestContext.currentTestRunId() == null) {
      TestContext.setTestRunId(UUID.randomUUID().toString())
    }
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
    TestContext.setTestRunId(UUID.randomUUID().toString())
    return contentMatcher.configureContent(contentType.toString(), bodyConfig)
  }

  companion object : KLogging()
}
