package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import io.pact.plugins.jvm.core.CatalogueManager
import mu.KLogging

interface ResponseGenerator {
  fun generateResponse(response: IResponse, context: MutableMap<String, Any>, testMode: GeneratorTestMode): IResponse
}

object DefaultResponseGenerator: ResponseGenerator, KLogging() {
  override fun generateResponse(
    response: IResponse,
    context: MutableMap<String, Any>,
    testMode: GeneratorTestMode
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
          r.body = contentHandler.generateContent(contentType, bodyGenerators, r.body)
        }
      }
    }
    return r
  }
}
