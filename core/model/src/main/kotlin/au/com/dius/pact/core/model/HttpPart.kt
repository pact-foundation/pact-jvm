package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging
import java.nio.charset.Charset
import java.util.Base64

/**
 * Base trait for an object that represents part of an http message
 */
abstract class HttpPart {

  abstract var body: OptionalBody
  abstract var headers: MutableMap<String, List<String>>
  abstract var matchingRules: MatchingRules
  abstract var generators: Generators

  @Deprecated("use method that returns a content type object",
    replaceWith = ReplaceWith("determineContentType"))
  fun contentType(): String? = contentTypeHeader()?.split(Regex("\\s*;\\s*"))?.first()
    ?: body.contentType.asString()

  fun determineContentType(): ContentType {
    val headerValue = contentTypeHeader()?.split(Regex("\\s*;\\s*"))?.first()
    return if (headerValue.isNullOrEmpty())
      body.contentType
    else
      ContentType(headerValue)
  }

  fun contentTypeHeader(): String? {
    val contentTypeKey = headers.keys.find { CONTENT_TYPE.equals(it, ignoreCase = true) }
    return headers[contentTypeKey]?.first()
  }

  fun jsonBody() = determineContentType().isJson()

  fun xmlBody() = determineContentType().isXml()

  fun setDefaultContentType(contentType: String) {
    if (headers.keys.find { it.equals(CONTENT_TYPE, ignoreCase = true) } == null) {
      headers[CONTENT_TYPE] = listOf(contentType)
    }
  }

  fun charset(): Charset? {
    return when {
      body.isPresent() -> body.contentType.asCharset()
      else -> {
        val contentType = contentTypeHeader()
        if (contentType.isNotEmpty()) {
          ContentType(contentType!!).asCharset()
        } else {
          null
        }
      }
    }
  }

  fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(matchingRules.validateForVersion(pactVersion))
    errors.addAll(generators.validateForVersion(pactVersion))
    return errors
  }

  fun buildGenerators(category: Category): Map<String, Generator> {
    val generators = generators.categories[category] ?: emptyMap()
    val matchingRuleGenerators = matchingRules.rulesForCategory(category.name.toLowerCase()).generators()
    return generators + matchingRuleGenerators
  }

  companion object : KLogging() {
    private const val CONTENT_TYPE = "Content-Type"

    @JvmStatic
    fun extractBody(json: JsonValue.Object, contentType: ContentType): OptionalBody {
      return when (val b = json["body"]) {
        is JsonValue.Null -> OptionalBody.nullBody()
        is JsonValue.StringValue -> decodeBody(b.asString()!!, contentType)
        else -> decodeBody(b.serialise(), contentType)
      }
    }

    private fun decodeBody(body: String, contentType: ContentType): OptionalBody {
      return when {
        contentType.isBinaryType() || contentType.isMultipart() -> try {
          OptionalBody.body(Base64.getDecoder().decode(body), contentType)
        } catch (ex: IllegalArgumentException) {
          logger.warn(ex) { "Expected body for content type $contentType to be base64 encoded" }
          OptionalBody.body(body.toByteArray(contentType.asCharset()), contentType)
        }
        else -> OptionalBody.body(body.toByteArray(contentType.asCharset()), contentType)
      }
    }
  }
}
