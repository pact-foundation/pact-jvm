package au.com.dius.pact.core.matchers.generators

import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.PluginData
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.json.JsonValue
import io.pact.plugins.jvm.core.CatalogueManager
import io.github.oshai.kotlinlogging.KLogging

interface ResponseGenerator {
  /**
   * Apply any generators to the response, creating a new response
   */
  fun generateResponse(
    response: IResponse,
    context: MutableMap<String, Any>,
    testMode: GeneratorTestMode,
    pluginData: List<PluginData>,
    interactionData: Map<String, Map<String, JsonValue>>
  ): IResponse
}

interface MessageContentsGenerator {
  /**
   * Apply any generators to the message contents, creating new message content
   */
  fun generateContents(
    contents: MessageContents,
    context: MutableMap<String, Any>,
    testMode: GeneratorTestMode,
    pluginData: List<PluginData>,
    interactionData: Map<String, Map<String, JsonValue>>,
    forRequest: Boolean
  ): MessageContents
}

object DefaultResponseGenerator: ResponseGenerator, MessageContentsGenerator, KLogging() {
  override fun generateResponse(
    response: IResponse,
    context: MutableMap<String, Any>,
    testMode: GeneratorTestMode,
    pluginData: List<PluginData>,
    interactionData: Map<String, Map<String, JsonValue>>
  ): IResponse {
    val r = response.copyResponse()
    val statusGenerators = r.setupGenerators(Category.STATUS, context)
    if (statusGenerators.isNotEmpty()) {
      Generators.applyGenerators(statusGenerators, testMode) { _, g -> r.status = g.generate(context, r.status) as Int }
    }
    val headerGenerators = r.setupGenerators(Category.HEADER, context)
    if (headerGenerators.isNotEmpty()) {
      Generators.applyGenerators(headerGenerators, testMode) { key, g ->
        r.headers[key] = listOf(g.generate(context, r.headers[key]).toString())
      }
    }
    if (r.body.isPresent()) {
      val bodyGenerators = r.setupGenerators(Category.BODY, context)
      if (bodyGenerators.isNotEmpty()) {
        val contentType = r.determineContentType()
        val contentHandler = CatalogueManager.findContentGenerator(contentType)
        if (contentHandler == null || contentHandler.isCore) {
          logger.debug {
            "Either no content generator was found, or is a core one, will use the internal implementation"
          }
          r.body = Generators.applyBodyGenerators(bodyGenerators, r.body, contentType, context, testMode)
        } else {
          logger.debug { "Plugin content generator, will get the plugin to generate the content" }
          r.body = contentHandler.generateContent(contentType, bodyGenerators, r.body, testMode,
            pluginData, interactionData, context.mapValues { toJson(it) }, false)
        }
      }
    }
    return r
  }

  override fun generateContents(
    contents: MessageContents,
    context: MutableMap<String, Any>,
    testMode: GeneratorTestMode,
    pluginData: List<PluginData>,
    interactionData: Map<String, Map<String, JsonValue>>,
    forRequest: Boolean
  ): MessageContents {
    logger.debug { "Generating message contents for message $contents" }
    var copy = contents.copy()
    val metadataGenerators = contents.setupGeneratorsFor(Category.METADATA, context)
    if (metadataGenerators.isNotEmpty()) {
      Generators.applyGenerators(metadataGenerators, testMode) { key, g ->
        copy.metadata[key] = g.generate(context, copy.metadata[key])
      }
    }
    if (contents.contents.isPresent()) {
      var bodyGenerators = contents.setupGeneratorsFor(Category.CONTENT, context)
      if (bodyGenerators.isEmpty()) {
        bodyGenerators = contents.setupGeneratorsFor(Category.BODY, context)
      }
      if (bodyGenerators.isNotEmpty()) {
        val contentType = contents.getContentType()
        val contentHandler = CatalogueManager.findContentGenerator(contentType)
        copy = if (contentHandler == null || contentHandler.isCore) {
          logger.debug {
            "Either no content generator was found, or is a core one, will use the internal implementation"
          }
          copy.copy(contents = Generators.applyBodyGenerators(bodyGenerators, copy.contents, contentType,
            context, testMode))
        } else {
          logger.debug { "Plugin content generator, will get the plugin to generate the content" }
          copy.copy(contents = contentHandler.generateContent(contentType, bodyGenerators, copy.contents, testMode,
            pluginData, interactionData, context.mapValues { toJson(it) }, forRequest))
        }
      }
    }
    return copy
  }
}
