package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.buildMatchingRuleNode
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Into
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object FormUrlencodedPlanBuilder : PlanBodyBuilder {
  override fun supportsType(contentType: ContentType) =
    contentType.getBaseType() == "application/x-www-form-urlencoded"

  override fun buildPlan(content: ByteArray, context: PlanMatchingContext): ExecutionPlanNode {
    val params = parseFormUrlencoded(content)

    val bodyNode = ExecutionPlanNode.action("tee")
    bodyNode.add(
      ExecutionPlanNode.action("form:parse")
        .add(ExecutionPlanNode.resolveValue(DocPath("$.body")))
    )

    val rootPath = DocPath.root()
    val rootNode = ExecutionPlanNode.container(rootPath.toString())
    val keys = params.keys.sorted()

    for (key in keys) {
      val values = params[key].orEmpty()
      val keyPath = rootPath.pushField(key)
      val expectedValue = if (values.size == 1) {
        NodeValue.STRING(values[0])
      } else {
        NodeValue.SLIST(values)
      }

      val itemNode = ExecutionPlanNode.container(keyPath.toString())
      val presenceCheck = ExecutionPlanNode.action("if")
      presenceCheck.add(
        ExecutionPlanNode.action("check:exists")
          .add(ExecutionPlanNode.resolveCurrentValue(keyPath))
      )

      if (context.matcherIsDefined(keyPath)) {
        val matchers = context.selectBestMatcher(keyPath)
        itemNode.add(ExecutionPlanNode.annotation(Into { "$key ${matchers.generateDescription(true)}" }))
        presenceCheck.add(
          buildMatchingRuleNode(
            ExecutionPlanNode.valueNode(expectedValue),
            ExecutionPlanNode.resolveCurrentValue(keyPath),
            matchers,
            true
          )
        )
      } else {
        itemNode.add(ExecutionPlanNode.annotation(Into { "$key=${expectedValue.strForm()}" }))
        presenceCheck.add(
          ExecutionPlanNode.action("match:equality")
            .add(ExecutionPlanNode.valueNode(expectedValue))
            .add(ExecutionPlanNode.resolveCurrentValue(keyPath))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
        )
      }

      itemNode.add(presenceCheck)
      rootNode.add(itemNode)
    }

    if (keys.isNotEmpty()) {
      rootNode.add(
        ExecutionPlanNode.action("expect:entries")
          .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
          .add(ExecutionPlanNode.resolveCurrentValue(rootPath))
          .add(
            ExecutionPlanNode.action("join")
              .add(ExecutionPlanNode.valueNode("The following expected form parameters were missing: "))
              .add(
                ExecutionPlanNode.action("join-with")
                  .add(ExecutionPlanNode.valueNode(", "))
                  .add(ExecutionPlanNode.splat().add(ExecutionPlanNode.action("apply")))
              )
          )
      )
      if (!context.config.allowUnexpectedEntries) {
        rootNode.add(
          ExecutionPlanNode.action("expect:only-entries")
            .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
            .add(ExecutionPlanNode.resolveCurrentValue(rootPath))
            .add(
              ExecutionPlanNode.action("join")
                .add(ExecutionPlanNode.valueNode("The following form parameters were not expected: "))
                .add(
                  ExecutionPlanNode.action("join-with")
                    .add(ExecutionPlanNode.valueNode(", "))
                    .add(ExecutionPlanNode.splat().add(ExecutionPlanNode.action("apply")))
                )
            )
        )
      }
    }

    bodyNode.add(rootNode)
    return bodyNode
  }
}

internal fun parseFormUrlencoded(bytes: ByteArray): Map<String, List<String>> =
  parseFormUrlencoded(bytes.toString(StandardCharsets.UTF_8))

internal fun parseFormUrlencoded(body: String): Map<String, List<String>> {
  val result = linkedMapOf<String, MutableList<String>>()
  for (pair in body.split('&')) {
    if (pair.isNotEmpty()) {
      val eqIdx = pair.indexOf('=')
      val key = if (eqIdx >= 0) pair.substring(0, eqIdx) else pair
      val value = if (eqIdx >= 0) pair.substring(eqIdx + 1) else ""
      result.getOrPut(URLDecoder.decode(key, StandardCharsets.UTF_8)) { mutableListOf() }
        .add(URLDecoder.decode(value, StandardCharsets.UTF_8))
    }
  }
  return result
}
