package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.Matcher
import au.com.dius.pact.consumer.dsl.MetadataBuilder
import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.v4.MessageContents

/**
 * DSL builder for the message contents part of a V4 message
 */
class MessageContentsBuilder(var contents: MessageContents) {
  /**
   *  Adds the expected metadata to the message contents
   */
  fun withMetadata(metadata: Map<String, Any>): MessageContentsBuilder {
    contents = contents.copy(metadata = metadata.mapValues { (key, value) ->
      if (value is Matcher) {
        contents.matchingRules.addCategory("metadata").addRule(key, value.matcher!!)
        if (value.generator != null) {
          contents.generators.addGenerator(category = Category.METADATA, generator = value.generator!!)
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
}
