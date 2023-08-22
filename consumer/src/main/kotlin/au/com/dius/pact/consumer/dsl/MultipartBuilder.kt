package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.Headers
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.HttpEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Builder class for constructing multipart/\* bodies.
 */
open class MultipartBuilder: BodyBuilder {
  private val builder = MultipartEntityBuilder.create()
  private var entity: HttpEntity? = null
  val matchingRules: MatchingRules = MatchingRulesImpl()
  private val generators = Generators()

  init {
    builder.setMode(HttpMultipartMode.EXTENDED)
  }

  override fun getMatchers(): MatchingRuleCategory {
    build()
    return matchingRules.rulesForCategory("body")
  }

  override fun getHeaderMatchers(): MatchingRuleCategory {
    build()
    return matchingRules.rulesForCategory("header")
  }

  override fun getGenerators(): Generators {
    build()
    return generators
  }

  override fun getContentType(): ContentType {
    build()
    return ContentType(entity!!.contentType)
  }

  private fun build() {
    if (entity == null) {
      entity = builder.build()
      val headerRules = matchingRules.addCategory("header")
      headerRules.addRule("Content-Type", RegexMatcher(Headers.MULTIPART_HEADER_REGEX, entity!!.contentType))
    }
  }

  override fun buildBody(): ByteArray {
    build()
    val stream = ByteArrayOutputStream()
    entity!!.writeTo(stream)
    return stream.toByteArray()
  }

  /**
   * Adds the contents of an input stream as a binary part with the given name and file name
   */
  @JvmOverloads
  fun filePart(
    partName: String,
    fileName: String? = null,
    inputStream: InputStream,
    contentType: String? = null
  ): MultipartBuilder {
    val ct = if (contentType.isNullOrEmpty()) {
      null
    } else {
      org.apache.hc.core5.http.ContentType.create(contentType)
    }
    builder.addBinaryBody(partName, inputStream.use { it.readAllBytes() }, ct, fileName)
    return this
  }

  /**
   * Adds the contents of a byte array as a binary part with the given name and file name
   */
  @JvmOverloads
  fun binaryPart(
    partName: String,
    fileName: String? = null,
    bytes: ByteArray,
    contentType: String? = null
  ): MultipartBuilder {
    val ct = if (contentType.isNullOrEmpty()) {
      null
    } else {
      org.apache.hc.core5.http.ContentType.create(contentType)
    }
    builder.addBinaryBody(partName, bytes, ct, fileName)
    return this
  }

  /**
   * Adds a JSON document as a part, using the standard Pact JSON DSL
   */
  fun jsonPart(partName: String, part: DslPart): MultipartBuilder {
    val parent = part.close()!!
    matchingRules.addCategory(parent.matchers.copyWithUpdatedMatcherRootPrefix("\$.$partName"))
    generators.addGenerators(parent.generators)
    builder.addTextBody(partName, part.body.toString(), org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
    return this
  }

  /**
   * Adds the contents of a string as a text part with the given name
   */
  @JvmOverloads
  fun textPart(
    partName: String,
    value: String,
    contentType: String? = null
  ): MultipartBuilder {
    val ct = if (contentType.isNullOrEmpty()) {
      null
    } else {
      org.apache.hc.core5.http.ContentType.create(contentType)
    }
    builder.addTextBody(partName, value, ct)
    return this
  }
}
