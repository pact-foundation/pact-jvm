package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.buildMatchingRuleNode
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

/**
 * Plan builder for JSON bodies
 */
object JsonPlanBuilder: PlanBodyBuilder  {
  override fun supportsType(contentType: ContentType) = contentType.isJson()

  override fun buildPlan(
    content: ByteArray,
    context: PlanMatchingContext
  ): ExecutionPlanNode {
    val expectedJson = JsonParser.parseStream(ByteArrayInputStream(content))
    val bodyNode = ExecutionPlanNode.action("tee")
    bodyNode
      .add(ExecutionPlanNode.action("json:parse")
        .add(ExecutionPlanNode.resolveValue(DocPath("$.body"))))

    val path = DocPath.root()
    val rootNode = ExecutionPlanNode.container(path.toString())
    processBodyNode(context, expectedJson, path, rootNode)
    bodyNode.add(rootNode)

    return bodyNode
  }

  private fun shouldApplyToMapEntries(matchers: MatchingRuleGroup): Boolean {
    return matchers.rules.any { rule ->
      return when (rule) {
        is ValuesMatcher -> true
        is EachKeyMatcher -> true
        is EachValueMatcher -> true
        else -> false
      }
    }
  }

  private fun processBodyNode(
    context: PlanMatchingContext,
    expectedJson: JsonValue,
    path: DocPath,
    rootNode: ExecutionPlanNode
  ) {
    logger.trace { "processBodyNode($path, $expectedJson)" }
    when (expectedJson) {
      is JsonValue.Array -> processArray(context, path, rootNode, expectedJson)
      is JsonValue.Object -> processObject(context, path, rootNode, expectedJson)
      else -> {
        if (context.matcherIsDefined(path)) {
          val matchers = context.selectBestMatcher(path)
          rootNode.add(ExecutionPlanNode.annotation(Into {
            "${path.lastField()} ${matchers.generateDescription(false)}"
          }))
          rootNode.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(expectedJson),
            ExecutionPlanNode.resolveCurrentValue(path), matchers, false))
        } else {
          val matchNode = ExecutionPlanNode.action("match:equality")
          matchNode
            .add(ExecutionPlanNode.valueNode(NodeValue.NAMESPACED("json", expectedJson.serialise())))
            .add(ExecutionPlanNode.resolveCurrentValue(path))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
          rootNode.add(matchNode)
        }
      }
    }
  }

  private fun processObject(
    context: PlanMatchingContext,
    path: DocPath,
    rootNode: ExecutionPlanNode,
    expectedJson: JsonValue.Object
  ) {
    val matchers = context.selectBestMatcher(path)
    if (matchers.rules.isNotEmpty() && shouldApplyToMapEntries(matchers)) {
      rootNode.add(ExecutionPlanNode.annotation(Into { matchers.generateDescription(true) }))
      rootNode.add(
        buildMatchingRuleNode(
          ExecutionPlanNode.valueNode(expectedJson),
          ExecutionPlanNode.resolveCurrentValue(path), matchers, true
        )
      )
    } else if (expectedJson.entries.isEmpty()) {
      rootNode.add(
        ExecutionPlanNode.action("json:expect:empty")
          .add(ExecutionPlanNode.valueNode("OBJECT"))
          .add(ExecutionPlanNode.resolveCurrentValue(path))
      )
    } else {
      val keys = NodeValue.SLIST(expectedJson.entries.keys.toList())
      rootNode.add(
        ExecutionPlanNode.action("json:expect:entries")
          .add(ExecutionPlanNode.valueNode("OBJECT"))
          .add(ExecutionPlanNode.valueNode(keys))
          .add(ExecutionPlanNode.resolveCurrentValue(path))
      )
      if (!context.config.allowUnexpectedEntries) {
        rootNode.add(
          ExecutionPlanNode.action("expect:only-entries")
            .add(ExecutionPlanNode.valueNode(keys))
            .add(ExecutionPlanNode.resolveCurrentValue(path))
        )
      } else {
        rootNode.add(
          ExecutionPlanNode.action("json:expect:not-empty")
            .add(ExecutionPlanNode.valueNode("OBJECT"))
            .add(ExecutionPlanNode.resolveCurrentValue(path))
        )
      }

      for ((key, value) in expectedJson.entries) {
        val itemPath = path.pushField(Into { key })
        val itemNode = ExecutionPlanNode.container(itemPath.toString())
        processBodyNode(context, value, itemPath, itemNode)
        rootNode.add(itemNode)
      }
    }
  }

  @Suppress("LongMethod")
  private fun processArray(
    context: PlanMatchingContext,
    path: DocPath,
    rootNode: ExecutionPlanNode,
    expectedJson: JsonValue.Array
  ) {
    if (context.matcherIsDefined(path)) {
      val matchers = context.selectBestMatcher(path)
      rootNode.add(ExecutionPlanNode.annotation(Into {
        "${path.lastField()} ${matchers.generateDescription(true)}"
      }))
      rootNode.add(
        buildMatchingRuleNode(
          ExecutionPlanNode.valueNode(expectedJson),
          ExecutionPlanNode.resolveCurrentValue(path), matchers, true
        )
      )

      val template = expectedJson.values.firstOrNull()
      if (template != null) {
        val forEachNode = ExecutionPlanNode.action("for-each")
        forEachNode.add(ExecutionPlanNode.valueNode(path.lastField()!! + "*"))
        val itemPath = path.parent()!!.join("${path.lastField()}*")
        forEachNode.add(ExecutionPlanNode.resolveCurrentValue(path))
        val itemNode = ExecutionPlanNode.container(itemPath.toString())
        when (template) {
          is JsonValue.Array -> processBodyNode(context, template, itemPath, itemNode)
          is JsonValue.Object -> processBodyNode(context, template, itemPath, itemNode)
          else -> {
            val presenceCheck = ExecutionPlanNode.action("if")
            presenceCheck
              .add(
                ExecutionPlanNode.action("check:exists")
                  .add(ExecutionPlanNode.resolveCurrentValue(itemPath))
              );
            if (context.matcherIsDefined(itemPath)) {
              val matchers = context.selectBestMatcher(itemPath)
              presenceCheck.add(ExecutionPlanNode.annotation(Into { "[*] ${matchers.generateDescription(false)}" }))
              presenceCheck.add(
                buildMatchingRuleNode(
                  ExecutionPlanNode.valueNode(template),
                  ExecutionPlanNode.resolveCurrentValue(itemPath), matchers, false
                )
              )
            } else {
              presenceCheck.add(
                ExecutionPlanNode.action("match:equality")
                  .add(ExecutionPlanNode.valueNode(NodeValue.NAMESPACED("json", template.serialise())))
                  .add(ExecutionPlanNode.resolveCurrentValue(itemPath))
                  .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
              );
            }
            itemNode.add(presenceCheck)
          }
        }
        forEachNode.add(itemNode)
        rootNode.add(forEachNode)
      }
    } else if (expectedJson.values.isEmpty()) {
      rootNode.add(
        ExecutionPlanNode.action("json:expect:empty")
          .add(ExecutionPlanNode.valueNode("ARRAY"))
          .add(ExecutionPlanNode.resolveCurrentValue(path))
      )
    } else {
      rootNode.add(
        ExecutionPlanNode.action("json:match:length")
          .add(ExecutionPlanNode.valueNode("ARRAY"))
          .add(ExecutionPlanNode.valueNode(expectedJson.values.size.toUInt()))
          .add(ExecutionPlanNode.resolveCurrentValue(path))
      )

      for ((index, item) in expectedJson.values.withIndex()) {
        val itemPath = path.pushIndex(index)
        val itemNode = ExecutionPlanNode.container(itemPath.toString())
        when (item) {
          is JsonValue.Array -> processBodyNode(context, item, itemPath, itemNode)
          is JsonValue.Object -> processBodyNode(context, item, itemPath, itemNode)
          else -> {
            val presenceCheck = ExecutionPlanNode.action("if")
            presenceCheck
              .add(
                ExecutionPlanNode.action("check:exists")
                  .add(ExecutionPlanNode.resolveCurrentValue(itemPath))
              )
            if (context.matcherIsDefined(itemPath)) {
              val matchers = context.selectBestMatcher(itemPath)
              presenceCheck.add(ExecutionPlanNode.annotation(Into {
                "[$index] ${matchers.generateDescription(false)}"
              }))
              presenceCheck.add(
                buildMatchingRuleNode(
                  ExecutionPlanNode.valueNode(item),
                  ExecutionPlanNode.resolveCurrentValue(itemPath), matchers, false
                )
              )
            } else {
              presenceCheck.add(
                ExecutionPlanNode.action("match:equality")
                  .add(ExecutionPlanNode.valueNode(NodeValue.NAMESPACED("json", item.serialise())))
                  .add(ExecutionPlanNode.resolveCurrentValue(itemPath))
                  .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
              )
            }
            presenceCheck.add(
              ExecutionPlanNode.action("error")
                .add(ExecutionPlanNode.valueNode(
                  "Expected a value for '${path.asJsonPointer().unwrap()}' but it was missing"))
            )
            itemNode.add(presenceCheck)
            rootNode.add(itemNode)
          }
        }
      }
    }
  }
}
