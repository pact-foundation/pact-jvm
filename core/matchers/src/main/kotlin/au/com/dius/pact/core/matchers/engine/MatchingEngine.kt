package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.engine.bodies.PlainTextBuilder
import au.com.dius.pact.core.matchers.engine.bodies.getBodyPlanBuilder
import au.com.dius.pact.core.matchers.engine.interpreter.ExecutionPlanInterpreter
import au.com.dius.pact.core.matchers.engine.resolvers.HttpRequestValueResolver
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.HeaderParser.headerValueToMap
import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.PARAMETERISED_HEADERS
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.support.json.JsonValue
import kotlin.collections.map

interface MatchingEngine {
  /**
   * Constructs an execution plan for the HTTP request part.
   */
  fun buildRequestPlan(expectedRequest: IRequest, context: PlanMatchingContext): ExecutionPlan

  /** Executes the request plan against the actual request. */
  fun executeRequestPlan(plan: ExecutionPlan, actual: IRequest, context: PlanMatchingContext): ExecutionPlan
}

object V2MatchingEngine: MatchingEngine {
  override fun buildRequestPlan(
    expectedRequest: IRequest,
    context: PlanMatchingContext
  ): ExecutionPlan {
    val plan = ExecutionPlan("request")

    plan.add(setupMethodPlan(expectedRequest, context.forMethod()))
    plan.add(setupPathPlan(expectedRequest, context.forPath()))
    plan.add(setupQueryPlan(expectedRequest, context.forQuery()))
    plan.add(setupHeaderPlan(expectedRequest, context.forHeaders()))
    plan.add(setupBodyPlan(expectedRequest, context.forBody()))

    return plan
  }

  override fun executeRequestPlan(
    plan: ExecutionPlan,
    actual: IRequest,
    context: PlanMatchingContext
  ): ExecutionPlan {
    val valueResolver = HttpRequestValueResolver(actual)
    val interpreter = ExecutionPlanInterpreter(context)
    val path = listOf<String>()
    val executedTree = interpreter.walkTree(path, plan.planRoot, valueResolver)
    return ExecutionPlan(executedTree)
  }

  @Suppress("UnusedParameter")
  fun setupMethodPlan(expected: IRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val methodContainer = ExecutionPlanNode.container("method")

    val matchMethod = ExecutionPlanNode.action("match:equality")
    val expectedMethod = expected.method.uppercase()
    matchMethod
      .add(ExecutionPlanNode.valueNode(expectedMethod))
      .add(ExecutionPlanNode.action("upper-case")
        .add(ExecutionPlanNode.resolveValue(DocPath("$.method"))))
      .add(ExecutionPlanNode.valueNode(NodeValue.NULL))

    methodContainer.add(ExecutionPlanNode.annotation(Into { "method == $expectedMethod" }))
    methodContainer.add(matchMethod);

    return methodContainer
  }

  fun setupPathPlan(expected: IRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("path")

    val expectedNode = ExecutionPlanNode.valueNode(expected.path)
    val docPath = DocPath("$.path")
    val actualNode = ExecutionPlanNode.resolveValue(docPath)
    if (context.matcherIsDefined(docPath)) {
      val matchers = context.selectBestMatcher(docPath)
      planNode.add(ExecutionPlanNode.annotation(Into { "path ${matchers.generateDescription(false)}" }))
      planNode.add(buildMatchingRuleNode(expectedNode, actualNode, matchers, false))
    } else {
      planNode.add(ExecutionPlanNode.annotation(Into { "path == '${expected.path}'" }))
      planNode
        .add(
          ExecutionPlanNode.action("match:equality")
            .add(expectedNode)
            .add(ExecutionPlanNode.resolveValue(docPath))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
        )
    }

    return planNode
  }

  @Suppress("LongMethod")
  fun setupQueryPlan(expected: IRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("query parameters")
    val docPath = DocPath("$.query")

    if (expected.query.isNotEmpty()) {
      val keys = expected.query.keys.sorted()
      for (key in keys) {
        val value = expected.query[key]!!
        val itemNode = ExecutionPlanNode.container(key)

        val presenceCheck = ExecutionPlanNode.action("if")
        val itemValue = if (value.size == 1) {
          NodeValue.STRING(value[0].orEmpty())
        } else {
          NodeValue.SLIST(value.map { it.orEmpty() })
        }
        val path = docPath.join(Into { key })
        presenceCheck
          .add(
            ExecutionPlanNode.action("check:exists")
              .add(ExecutionPlanNode.resolveValue(path))
          )

        val itemPath = DocPath(listOf(PathToken.Field(key)), key)
        if (context.matcherIsDefined(itemPath)) {
          val matchers = context.selectBestMatcher(itemPath)
          itemNode.add(ExecutionPlanNode.annotation(Into { "$key ${matchers.generateDescription(true)}" }))
          presenceCheck.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(itemValue),
            ExecutionPlanNode.resolveValue(path), matchers, true))
        } else {
          itemNode.add(ExecutionPlanNode.annotation(Into { "$key=${itemValue.strForm()}" }))
          val itemCheck = ExecutionPlanNode.action("match:equality")
          itemCheck
            .add(ExecutionPlanNode.valueNode(itemValue))
            .add(ExecutionPlanNode.resolveValue(path))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
          presenceCheck.add(itemCheck)
        }

        itemNode.add(presenceCheck)
        planNode.add(itemNode)
      }

      planNode.add(
        ExecutionPlanNode.action("expect:entries")
          .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
          .add(ExecutionPlanNode.resolveValue(docPath))
          .add(
            ExecutionPlanNode.action("join")
              .add(ExecutionPlanNode.valueNode("The following expected query parameters were missing: "))
              .add(ExecutionPlanNode.action("join-with")
                .add(ExecutionPlanNode.valueNode(", "))
                .add(
                  ExecutionPlanNode.splat()
                    .add(ExecutionPlanNode.action("apply"))
                )
              )
          )
      )

      planNode.add(
        ExecutionPlanNode.action("expect:only-entries")
          .add(ExecutionPlanNode.valueNode(NodeValue.SLIST(keys)))
          .add(ExecutionPlanNode.resolveValue(docPath))
          .add(
            ExecutionPlanNode.action("join")
              .add(ExecutionPlanNode.valueNode("The following query parameters were not expected: "))
              .add(ExecutionPlanNode.action("join-with")
                .add(ExecutionPlanNode.valueNode(", "))
                .add(
                  ExecutionPlanNode.splat()
                    .add(ExecutionPlanNode.action("apply"))
                )
              )
          )
      )
    } else {
      planNode
        .add(
          ExecutionPlanNode.action("expect:empty")
            .add(ExecutionPlanNode.resolveValue(docPath))
            .add(
              ExecutionPlanNode.action("join")
                .add(ExecutionPlanNode.valueNode("Expected no query parameters but got "))
                .add(ExecutionPlanNode.resolveValue(docPath))
            )
        )
    }

    return planNode
  }

  @Suppress("LongMethod")
  fun setupHeaderPlan(expected: IRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("headers")
    val docPath = DocPath("$.headers")

    if (expected.headers.isNotEmpty()) {
      val keys = expected.headers.keys.sorted()
      for (key in keys) {
        val value = expected.headers[key]!!
        val itemNode = ExecutionPlanNode.container(key)
        val path = docPath.join(Into { key })

        val presenceCheck = ExecutionPlanNode.action("if")
        val itemValue = if (value.size == 1) {
          NodeValue.STRING(value[0])
        } else {
          NodeValue.SLIST(value)
        }
        presenceCheck
          .add(
            ExecutionPlanNode.action("check:exists")
              .add(ExecutionPlanNode.resolveValue(path))
          )

        if (context.matcherIsDefined(path)) {
          val matchers = context.selectBestMatcher(path)
          itemNode.add(ExecutionPlanNode.annotation(Into { "$key ${matchers.generateDescription(true)}" }))
          presenceCheck.add(buildMatchingRuleNode(ExecutionPlanNode.valueNode(itemValue),
            ExecutionPlanNode.resolveValue(path), matchers, true))
        } else if (PARAMETERISED_HEADERS.contains(key.lowercase())) {
          itemNode.add(ExecutionPlanNode.annotation(Into { "$key=${itemValue.strForm()}" }))
          if (value.size == 1) {
            val applyNode = buildParameterisedHeaderPlan(path, value[0])
            presenceCheck.add(applyNode)
          } else {
            for ((index, itemValue) in value.withIndex()) {
              val itemPath = docPath.join(Into { key }).pushIndex(index)
              val itemNode = ExecutionPlanNode.container(index.toString())
              val applyNode = buildParameterisedHeaderPlan(itemPath, itemValue)
              itemNode.add(applyNode)
              presenceCheck.add(itemNode)
            }
          }
        } else {
          itemNode.add(ExecutionPlanNode.annotation(Into { "$key=${itemValue.strForm()}" }))
          val itemCheck = ExecutionPlanNode.action("match:equality")
          itemCheck
            .add(ExecutionPlanNode.valueNode(itemValue))
            .add(ExecutionPlanNode.resolveValue(path))
            .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
          presenceCheck.add(itemCheck)
        }

        itemNode.add(presenceCheck)
        planNode.add(itemNode)
      }

      planNode.add(
        ExecutionPlanNode.action("expect:entries")
          .add(ExecutionPlanNode.action("lower-case")
            .add(ExecutionPlanNode.valueNode(Into { NodeValue.SLIST(keys) })))
          .add(ExecutionPlanNode.resolveValue(docPath))
          .add(
            ExecutionPlanNode.action("join")
              .add(ExecutionPlanNode.valueNode("The following expected headers were missing: "))
              .add(ExecutionPlanNode.action("join-with")
                .add(ExecutionPlanNode.valueNode(", "))
                .add(
                  ExecutionPlanNode.splat()
                    .add(ExecutionPlanNode.action("apply"))
                )
              )
          )
      )

      if (!context.config.allowUnexpectedEntries) {
        planNode.add(
          ExecutionPlanNode.action("expect:only-entries")
            .add(ExecutionPlanNode.action("lower-case")
              .add(ExecutionPlanNode.valueNode(Into { NodeValue.SLIST(keys) })))
            .add(ExecutionPlanNode.resolveValue(docPath))
            .add(
              ExecutionPlanNode.action("join")
                .add(ExecutionPlanNode.valueNode("The following headers were unexpected: "))
                .add(ExecutionPlanNode.action("join-with")
                  .add(ExecutionPlanNode.valueNode(", "))
                  .add(
                    ExecutionPlanNode.splat()
                      .add(ExecutionPlanNode.action("apply"))
                  )
                )
            )
        )
      }
    }

    return planNode
  }

  private fun buildParameterisedHeaderPlan(docPath: DocPath, value: String): ExecutionPlanNode {
    val (headerValue, headerParams) = headerValueToMap(value)

    val applyNode = ExecutionPlanNode.action("tee")
    applyNode
      .add(ExecutionPlanNode.action("header:parse")
        .add(ExecutionPlanNode.resolveValue(docPath)))
    applyNode.add(
      ExecutionPlanNode.action("match:equality")
        .add(ExecutionPlanNode.valueNode(headerValue))
        .add(ExecutionPlanNode.action("to-string")
          .add(ExecutionPlanNode.resolveCurrentValue(Into { DocPath(listOf(PathToken.Field("value")), "value") })))
        .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
    )

    if (headerParams.isNotEmpty()) {
      val parameterPath = DocPath(listOf(PathToken.Field("parameters")), "parameters")
      for ((k, v) in headerParams) {
        val parameterNode = ExecutionPlanNode.container(k)
        parameterNode.add(
          ExecutionPlanNode.action("if")
            .add(ExecutionPlanNode.action("check:exists")
              .add(ExecutionPlanNode.resolveCurrentValue(parameterPath.join(Into { k }))))
            .add(ExecutionPlanNode.action("match:equality")
              .add(ExecutionPlanNode.valueNode(v.lowercase()))
              .add(ExecutionPlanNode.action("lower-case")
                .add(ExecutionPlanNode.resolveCurrentValue(parameterPath.join(Into { k }))))
              .add(ExecutionPlanNode.valueNode(NodeValue.NULL)))
            .add(ExecutionPlanNode.action("error")
              .add(ExecutionPlanNode.valueNode("Expected a $k value of '$v' but it was missing"))
            )
        )
        applyNode.add(parameterNode);
      }
    }
    return applyNode
  }

  fun setupBodyPlan(expected: IRequest, context: PlanMatchingContext): ExecutionPlanNode {
    // TODO: Look at the matching rules and generators here
    val planNode = ExecutionPlanNode.container("body")

    when {
      expected.body.isMissing() -> {}
      expected.body.isNull() || expected.body.isEmpty() -> {
        planNode.add(
          ExecutionPlanNode.action("expect:empty")
            .add(ExecutionPlanNode.resolveValue(DocPath("$.body")))
        )
      }
      expected.body.isPresent() -> {
        val contentType = expected.determineContentType()
        val contentTypeCheckNode = ExecutionPlanNode.action("if")
        contentTypeCheckNode
          .add(
            ExecutionPlanNode.action("match:equality")
              .add(ExecutionPlanNode.valueNode(contentType.toString()))
              .add(ExecutionPlanNode.resolveValue(DocPath("$.content-type")))
              .add(ExecutionPlanNode.valueNode(NodeValue.NULL))
              .add(
                ExecutionPlanNode.action("error")
                  .add(ExecutionPlanNode.valueNode(NodeValue.STRING("Body type error - ")))
                  .add(ExecutionPlanNode.action("apply"))
              )
          )

        val planBuilder = getBodyPlanBuilder(contentType)
        if (planBuilder != null) {
          contentTypeCheckNode.add(planBuilder.buildPlan(expected.body.unwrap(), context))
        } else {
          val builder = PlainTextBuilder
          contentTypeCheckNode.add(builder.buildPlan(expected.body.unwrap(), context))
        }
        planNode.add(contentTypeCheckNode)
      }
    }

    return planNode
  }
}

fun buildMatchingRuleNode(
  expectedNode: ExecutionPlanNode,
  actualNode: ExecutionPlanNode,
  matchers: MatchingRuleGroup,
  forCollection: Boolean
): ExecutionPlanNode {
  return if (matchers.rules.size == 1) {
    val matcher = if (forCollection) {
      matchers.rules[0]
    } else {
      matchers.rules[0].forSingleItem()
    }
    val planNode = ExecutionPlanNode.action("match:${matcher.name}")
    planNode
      .add(expectedNode)
      .add(actualNode)
      .add(ExecutionPlanNode.valueNode(Into {
        NodeValue.JSON(JsonValue.Object(matcher.attributes.toMutableMap()))
      }))
    planNode
  } else {
    val logicNode = when (matchers.ruleLogic) {
      RuleLogic.AND -> ExecutionPlanNode.action("and")
      RuleLogic.OR -> ExecutionPlanNode.action("or")
    }
    for (rule in matchers.rules) {
      val matcher = if (forCollection) {
        rule
      } else {
        rule.forSingleItem()
      };
      logicNode
        .add(
          ExecutionPlanNode.action("match:${matcher.name}")
            .add(expectedNode)
            .add(actualNode)
            .add(ExecutionPlanNode.valueNode(Into {
              NodeValue.JSON(JsonValue.Object(matcher.attributes.toMutableMap()))
            }))
        )
    }
    logicNode
  }
}
