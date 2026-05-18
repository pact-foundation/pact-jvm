package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.buildMatchingRuleNode
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.support.json.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

private val logger = KotlinLogging.logger {}

/** Plan builder for multipart/form-data bodies */
object MultipartFormDataPlanBuilder : PlanBodyBuilder {
  override fun supportsType(contentType: ContentType) = contentType.isMultipart()

  override fun buildPlan(content: ByteArray, context: PlanMatchingContext): ExecutionPlanNode {
    val expectedCt = context.interaction.asSynchronousRequestResponse()
      ?.request?.determineContentType()?.toString()
      ?: context.interaction.asAsynchronousMessage()
        ?.contents?.getContentType()?.toString()
      ?: "multipart/form-data"

    logger.trace { "Building multipart plan for content type: $expectedCt" }

    val multipart = MimeMultipart(ByteArrayDataSource(content, expectedCt))
    val partNames = mutableListOf<String>()

    val bodyNode = ExecutionPlanNode.action("tee")
    bodyNode.add(
      ExecutionPlanNode.action("multipart:parse")
        .add(ExecutionPlanNode.resolveValue(DocPath("$.body")))
        .add(ExecutionPlanNode.resolveValue(DocPath("$.content-type")))
    )

    val rootNode = ExecutionPlanNode.container(DocPath.root().toString())

    for (i in 0 until multipart.count) {
      val part = multipart.getBodyPart(i)
      val cd = ContentDisposition(part.disposition ?: "form-data")
      val name = cd.getParameter("name") ?: continue

      partNames.add(name)
      val partPath = DocPath.root().pushField(name)
      val partNode = ExecutionPlanNode.container(partPath.toString())

      val presenceCheck = ExecutionPlanNode.action("if")
      presenceCheck.add(
        ExecutionPlanNode.action("check:exists")
          .add(ExecutionPlanNode.resolveCurrentValue(partPath))
      )

      val partBytes = part.inputStream.readAllBytes()
      val partCt = part.contentType
      val isJson = partCt?.let { ContentType(it).isJson() } ?: false

      if (isJson) {
        // Delegate to JsonPlanBuilder so paths are $.partname, $.partname.field etc.,
        // matching the body matching rules defined at those paths.
        val json = JsonParser.parseStream(ByteArrayInputStream(partBytes))
        val subNode = ExecutionPlanNode.container(partPath.toString())
        JsonPlanBuilder.processBodyNode(context, json, partPath, subNode)
        presenceCheck.add(subNode)
      } else {
        // Binary/text part — apply matching rules if defined
        if (context.matcherIsDefined(partPath)) {
          val matchers = context.selectBestMatcher(partPath)
          presenceCheck.add(
            buildMatchingRuleNode(
              ExecutionPlanNode.valueNode(NodeValue.NULL),
              ExecutionPlanNode.resolveCurrentValue(partPath),
              matchers,
              false
            )
          )
        }
      }

      presenceCheck.add(
        ExecutionPlanNode.action("error")
          .add(ExecutionPlanNode.valueNode(NodeValue.STRING("Expected multipart part '$name' but was missing")))
      )

      partNode.add(presenceCheck)
      rootNode.add(partNode)
    }

    if (partNames.isNotEmpty()) {
      rootNode.add(
        ExecutionPlanNode.action("expect:entries")
          .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(partNames)))
          .add(ExecutionPlanNode.resolveCurrentValue(DocPath.root()))
      )
    }

    bodyNode.add(rootNode)
    return bodyNode
  }
}
