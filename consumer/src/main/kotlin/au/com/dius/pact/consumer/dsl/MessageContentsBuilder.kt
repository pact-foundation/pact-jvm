package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.isNotEmpty

/**
 * DSL builder for the message contents part of a V4 message
 */
class MessageContentsBuilder(var contents: MessageContents) {
  fun build() = contents

  /**
   *  Adds the expected metadata to the message contents
   */
  fun withMetadata(metadata: Map<String, Any>): MessageContentsBuilder {
    contents = contents.copy(metadata = metadata.mapValues { (key, value) ->
      if (value is Matcher) {
        if (value.matcher != null) {
          contents.matchingRules.addCategory("metadata").addRule(key, value.matcher!!)
        }
        if (value.generator != null) {
          contents.generators.addGenerator(Category.METADATA, key, value.generator!!)
        }
        value.value
      } else {
        value
      }
    }.toMutableMap())
    return this
  }

  /**
   *  Adds the expected metadata to the message using a builder
   */
  fun withMetadata(consumer: java.util.function.Consumer<MetadataBuilder>): MessageContentsBuilder {
    val metadataBuilder = MetadataBuilder()
    consumer.accept(metadataBuilder)
    contents = contents.copy(metadata = metadataBuilder.values)
    contents.matchingRules.addCategory(metadataBuilder.matchers)
    contents.generators.addGenerators(Category.METADATA, metadataBuilder.generators)
    return this
  }

  /**
   * Adds the JSON body as the message content
   */
  fun withContent(body: DslPart): MessageContentsBuilder {
    val metadata = contents.metadata.toMutableMap()
    val contentTypeEntry = metadata.entries.find {
      it.key.lowercase() == "contenttype" || it.key.lowercase() == "content-type"
    }

    var contentType = ContentType.JSON
    if (contentTypeEntry == null) {
      metadata["contentType"] = contentType.toString()
    } else {
      contentType = ContentType(contentTypeEntry.value.toString())
      metadata.remove(contentTypeEntry.key)
      metadata["contentType"] = contentTypeEntry.value
    }

    val parent = body.close()!!
    contents = contents.copy(
      contents = OptionalBody.body(parent.toString().toByteArray(contentType.asCharset()), contentType),
      metadata = metadata
    )
    contents.matchingRules.addCategory(parent.matchers)
    contents.generators.addGenerators(parent.generators)

    return this
  }

  /**
   * Adds the XML body as the message content
   */
  fun withContent(xmlBuilder: PactXmlBuilder): MessageContentsBuilder {
    val metadata = contents.metadata.toMutableMap()
    val contentTypeEntry = metadata.entries.find {
      it.key.lowercase() == "contenttype" || it.key.lowercase() == "content-type"
    }

    var contentType = ContentType.XML
    if (contentTypeEntry == null) {
      metadata["contentType"] = contentType.toString()
    } else {
      contentType = ContentType(contentTypeEntry.value.toString())
      metadata.remove(contentTypeEntry.key)
      metadata["contentType"] = contentTypeEntry.value
    }

    contents = contents.copy(
      contents = OptionalBody.body(xmlBuilder.asBytes(contentType.asCharset()), contentType),
      metadata = metadata
    )
    contents.matchingRules.addCategory(xmlBuilder.matchingRules)
    contents.generators.addGenerators(xmlBuilder.generators)

    return this
  }

  /**
   * Adds the string as the message contents with the given content type. If the content type is not supplied,
   * it will try to detect it otherwise will default to plain text.
   */
  @JvmOverloads
  fun withContent(payload: String, contentType: String? = null): MessageContentsBuilder {
    val contentTypeMetadata = Message.contentType(contents.metadata)
    val ct = if (contentType.isNotEmpty()) {
      ContentType.fromString(contentType)
    } else if (contentTypeMetadata.contentType != null) {
      contentTypeMetadata
    } else {
      OptionalBody.detectContentTypeInByteArray(payload.toByteArray()) ?: ContentType.TEXT_PLAIN
    }
    contents = contents.copy(
      contents = OptionalBody.body(payload.toByteArray(ct.asCharset()), ct),
      metadata = (contents.metadata + Pair("contentType", ct.toString())).toMutableMap()
    )
    return this
  }

  /**
   * Sets the contents of the message as a byte array. If the content type is not provided or already set, will
   * default to application/octet-stream.
   */
  @JvmOverloads
  fun withContent(payload: ByteArray, contentType: String? = null): MessageContentsBuilder {
    val contentTypeMetadata = Message.contentType(contents.metadata)
    val ct = if (contentType.isNotEmpty()) {
      ContentType.fromString(contentType)
    } else if (contentTypeMetadata.contentType != null) {
      contentTypeMetadata
    } else {
      ContentType.OCTET_STEAM
    }

    contents = contents.copy(
      contents = OptionalBody.body(payload, ct),
      metadata = (contents.metadata + Pair("contentType", ct.toString())).toMutableMap()
    )

    return this
  }

  /**
   * Sets up a content type matcher to match any payload of the given content type
   */
  fun withContentsMatchingContentType(contentType: String, exampleContents: ByteArray): MessageContentsBuilder {
    val ct = ContentType(contentType)
    contents.contents = OptionalBody.body(exampleContents, ct)
    contents.metadata["contentType"] = contentType
    contents.matchingRules.addCategory("body").addRule("$", ContentTypeMatcher(contentType))
    return this
  }
}
