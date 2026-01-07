package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.Charset
import java.util.Base64

private val logger = KotlinLogging.logger {}

/**
 * object that represents part of an http message
 */
interface IHttpPart {
  var body: OptionalBody
  val headers: MutableMap<String, List<String>>
  val matchingRules: MatchingRules
  val generators: Generators

  fun determineContentType(): ContentType {
    val headerValue = contentTypeHeader()?.split(Regex("\\s*;\\s*"))?.first()
    return if (headerValue.isNullOrEmpty())
      body.contentType
    else
      ContentType(headerValue)
  }

  fun contentTypeHeader(): String? {
    val contentTypeKey = headers.keys.find { HttpPart.CONTENT_TYPE.equals(it, ignoreCase = true) }
    return headers[contentTypeKey]?.first()
  }

  fun setupGenerators(category: Category, context: Map<String, Any>): Map<String, Generator>

  fun hasHeader(name: String): Boolean

  /**
   * Allows the part of the interaction to transform the config so that it is keyed correctly. For instance,
   * an HTTP interaction may have both a request and response body from a plugin. This allows the request and
   * response parts to set the config for the correct part of the interaction.
   */
  fun transformConfig(config: MutableMap<String, JsonValue>): Map<String, JsonValue> = config
}

/**
 * Base trait for an object that represents part of an http message
 */
abstract class HttpPart: IHttpPart {
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

  fun validateForVersion(pactVersion: PactSpecVersion?): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(matchingRules.validateForVersion(pactVersion))
    errors.addAll(generators.validateForVersion(pactVersion))
    return errors
  }

  override fun setupGenerators(category: Category, context: Map<String, Any>): Map<String, Generator> {
    val generators = generators.categories[category] ?: emptyMap()
    val matchingRuleGenerators = matchingRules.rulesForCategory(category.name.lowercase()).generators(context)
    return generators + matchingRuleGenerators
  }

  companion object {
    const val CONTENT_TYPE = "Content-Type"

    @JvmStatic
    @JvmOverloads
    fun extractBody(
      json: JsonValue.Object,
      contentType: ContentType,
      decoder: Base64.Decoder = Base64.getDecoder()
    ): OptionalBody {
      return when (val b = json["body"]) {
        is JsonValue.Null -> OptionalBody.nullBody()
        is JsonValue.StringValue -> decodeBody(b.toString(), contentType, decoder)
        else -> decodeBody(b.serialise(), contentType, decoder)
      }
    }

    private fun decodeBody(body: String, contentType: ContentType, decoder: Base64.Decoder): OptionalBody {
      return when {
        contentType.isBinaryType() || contentType.isMultipart() -> try {
          OptionalBody.body(decoder.decode(body), contentType)
        } catch (ex: IllegalArgumentException) {
          logger.warn { "Expected body for content type $contentType to be base64 encoded: ${ex.message}" }
          OptionalBody.body(body.toByteArray(contentType.asCharset()), contentType)
        }
        else -> OptionalBody.body(body.toByteArray(contentType.asCharset()), contentType)
      }
    }
  }
}
