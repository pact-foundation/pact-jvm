package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DocPath

/**
 * Plan builder for plain text. This just sets up an equality matcher
 */
object PlainTextBuilder: PlanBodyBuilder {
  override fun supportsType(contentType: ContentType) = contentType.isText()

  override fun buildPlan(content: ByteArray, context: PlanMatchingContext): ExecutionPlanNode {
    val textContent = content.decodeToString()
    val node = ExecutionPlanNode.action("match:equality")
    val childNode = ExecutionPlanNode.action("convert:UTF8")
    childNode.add(ExecutionPlanNode.resolveValue(DocPath("$.body")))
    node.add(ExecutionPlanNode.valueNode(textContent))
    node.add(childNode)
    node.add(ExecutionPlanNode.valueNode(NodeValue.NULL))
    return node
  }
}
