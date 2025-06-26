package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.engine.bodies.PlainTextBuilder
import au.com.dius.pact.core.matchers.engine.bodies.getBodyPlanBuilder
import au.com.dius.pact.core.matchers.engine.interpreter.ExecutionPlanInterpreter
import au.com.dius.pact.core.matchers.engine.resolvers.HttpRequestValueResolver
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.Into
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.support.json.JsonValue

interface MatchingEngine {
  /**
   * Constructs an execution plan for the HTTP request part.
   */
  fun buildRequestPlan(expectedRequest: HttpRequest, context: PlanMatchingContext): ExecutionPlan

  /** Executes the request plan against the actual request. */
  fun executeRequestPlan(plan: ExecutionPlan, actual: HttpRequest, context: PlanMatchingContext): ExecutionPlan
}

object V2MatchingEngine: MatchingEngine {
  override fun buildRequestPlan(
    expectedRequest: HttpRequest,
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
    actual: HttpRequest,
    context: PlanMatchingContext
  ): ExecutionPlan {
    val valueResolver = HttpRequestValueResolver(actual)
    val interpreter = ExecutionPlanInterpreter(context)
    val path = listOf<String>()
    val executedTree = interpreter.walkTree(path, plan.planRoot, valueResolver)
    return ExecutionPlan(executedTree)
  }

  @Suppress("UnusedParameter")
  fun setupMethodPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
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

  fun setupPathPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("path")

    val expectedNode = ExecutionPlanNode.valueNode(expected.path)
    val docPath = DocPath("$.path")
    @Suppress("UnusedPrivateProperty")
    val actualNode = ExecutionPlanNode.resolveValue(docPath)
    if (context.matcherIsDefined(docPath)) {
      @Suppress("UnusedPrivateProperty")
      val matchers = context.selectBestMatcher(docPath)
//      planNode.add(ExecutionPlanNode.annotation(Into { "path ${matchers.generateDescription(false)}" }))
//      planNode.add(buildMatchingRuleNode(expectedNode, actualNode, matchers, false))
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

  @Suppress("UnusedParameter")
  fun setupQueryPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("query parameters")
    val docPath = DocPath("$.query")

    if (expected.query.isNotEmpty()) {
  //    if query.is_empty() {
  //      plan_node
  //        .add(
  //          ExecutionPlanNode::action("expect:empty")
  //            .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
  //            .add(
  //              ExecutionPlanNode::action("join")
  //                .add(ExecutionPlanNode::value_node("Expected no query parameters but got "))
  //                .add(ExecutionPlanNode::resolve_value(doc_path))
  //            )
  //        );
  //    } else {
  //      let keys = query.keys().cloned().sorted().collect_vec();
  //      for key in &keys {
  //        let value = query.get(key).unwrap();
  //        let mut item_node = ExecutionPlanNode::container(key);
  //
  //        let mut presence_check = ExecutionPlanNode::action("if");
  //        let item_value = if value.len() == 1 {
  //          NodeValue::STRING(value[0].clone().unwrap_or_default())
  //        } else {
  //          NodeValue::SLIST(value.iter()
  //            .map(|v| v.clone().unwrap_or_default()).collect())
  //        };
  //        presence_check
  //          .add(
  //            ExecutionPlanNode::action("check:exists")
  //              .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
  //          );
  //
  //        let item_path = DocPath::root().join(key);
  //        let path = doc_path.join(key);
  //        if context.matcher_is_defined(&item_path) {
  //          let matchers = context.select_best_matcher(&item_path);
  //          item_node.add(ExecutionPlanNode::annotation(format!("{} {}", key, matchers.generate_description(true))));
  //          presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
  //                                                      &ExecutionPlanNode::resolve_value(&path), &matchers, true));
  //        } else {
  //          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
  //          let mut item_check = ExecutionPlanNode::action("match:equality");
  //          item_check
  //            .add(ExecutionPlanNode::value_node(item_value))
  //            .add(ExecutionPlanNode::resolve_value(&path))
  //            .add(ExecutionPlanNode::value_node(NodeValue::NULL));
  //          presence_check.add(item_check);
  //        }
  //
  //        item_node.add(presence_check);
  //        plan_node.add(item_node);
  //      }
  //
  //      plan_node.add(
  //        ExecutionPlanNode::action("expect:entries")
  //          .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone())))
  //          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
  //          .add(
  //            ExecutionPlanNode::action("join")
  //              .add(ExecutionPlanNode::value_node("The following expected query parameters were missing: "))
  //              .add(ExecutionPlanNode::action("join-with")
  //                .add(ExecutionPlanNode::value_node(", "))
  //                .add(
  //                  ExecutionPlanNode::splat()
  //                    .add(ExecutionPlanNode::action("apply"))
  //                )
  //              )
  //          )
  //      );
  //
  //      plan_node.add(
  //        ExecutionPlanNode::action("expect:only-entries")
  //          .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone())))
  //          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
  //          .add(
  //            ExecutionPlanNode::action("join")
  //              .add(ExecutionPlanNode::value_node("The following query parameters were not expected: "))
  //              .add(ExecutionPlanNode::action("join-with")
  //                .add(ExecutionPlanNode::value_node(", "))
  //                .add(
  //                  ExecutionPlanNode::splat()
  //                    .add(ExecutionPlanNode::action("apply"))
  //                )
  //              )
  //          )
  //      );
  //    }
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

  @Suppress("UnusedParameter")
  fun setupHeaderPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val planNode = ExecutionPlanNode.container("headers")
    val docPath = DocPath("$.headers")

  //  if let Some(headers) = &expected.headers {
  //    if !headers.is_empty() {
  //      let keys = headers.keys().cloned().sorted().collect_vec();
  //      for key in &keys {
  //        let value = headers.get(key).unwrap();
  //        let mut item_node = ExecutionPlanNode::container(key);
  //
  //        let mut presence_check = ExecutionPlanNode::action("if");
  //        let item_value = if value.len() == 1 {
  //          NodeValue::STRING(value[0].clone())
  //        } else {
  //          NodeValue::SLIST(value.clone())
  //        };
  //        presence_check
  //          .add(
  //            ExecutionPlanNode::action("check:exists")
  //              .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
  //          );
  //
  //        let item_path = DocPath::root().join(key);
  //        let path = doc_path.join(key);
  //        if context.matcher_is_defined(&item_path) {
  //          let matchers = context.select_best_matcher(&item_path);
  //          item_node.add(ExecutionPlanNode::annotation(format!("{} {}", key, matchers.generate_description(true))));
  //          presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
  //                                                      &ExecutionPlanNode::resolve_value(&path), &matchers, true));
  //        } else if PARAMETERISED_HEADERS.contains(&key.to_lowercase().as_str()) {
  //          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
  //          if value.len() == 1 {
  //            let apply_node = build_parameterised_header_plan(&path, value[0].as_str());
  //            presence_check.add(apply_node);
  //          } else {
  //            for (index, item_value) in value.iter().enumerate() {
  //              let item_path = doc_path.join(key).join_index(index);
  //              let mut item_node = ExecutionPlanNode::container(index.to_string());
  //              let apply_node = build_parameterised_header_plan(
  //                &item_path, item_value.as_str());
  //              item_node.add(apply_node);
  //              presence_check.add(item_node);
  //            }
  //          }
  //        } else {
  //          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
  //          let mut item_check = ExecutionPlanNode::action("match:equality");
  //          item_check
  //            .add(ExecutionPlanNode::value_node(item_value))
  //            .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
  //            .add(ExecutionPlanNode::value_node(NodeValue::NULL));
  //          presence_check.add(item_check);
  //        }
  //
  //        item_node.add(presence_check);
  //        plan_node.add(item_node);
  //      }
  //
  //      plan_node.add(
  //        ExecutionPlanNode::action("expect:entries")
  //          .add(ExecutionPlanNode::action("lower-case")
  //            .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone()))))
  //          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
  //          .add(
  //            ExecutionPlanNode::action("join")
  //              .add(ExecutionPlanNode::value_node("The following expected headers were missing: "))
  //              .add(ExecutionPlanNode::action("join-with")
  //                .add(ExecutionPlanNode::value_node(", "))
  //                .add(
  //                  ExecutionPlanNode::splat()
  //                    .add(ExecutionPlanNode::action("apply"))
  //                )
  //              )
  //          )
  //      );
  //    }
  //  }

    return planNode
  }

  //fn build_parameterised_header_plan(doc_path: &DocPath, val: &str) -> ExecutionPlanNode {
  //  let values: Vec<&str> = strip_whitespace(val, ";");
  //  let (header_value, header_params) = values.as_slice()
  //    .split_first()
  //    .unwrap_or((&"", &[]));
  //  let parameter_map = parse_charset_parameters(header_params);
  //
  //  let mut apply_node = ExecutionPlanNode::action("tee");
  //  apply_node
  //    .add(ExecutionPlanNode::action("header:parse")
  //      .add(ExecutionPlanNode::resolve_value(doc_path)));
  //  apply_node.add(
  //    ExecutionPlanNode::action("match:equality")
  //      .add(ExecutionPlanNode::value_node(*header_value))
  //      .add(ExecutionPlanNode::action("to-string")
  //        .add(ExecutionPlanNode::resolve_current_value(&DocPath::new_unwrap("value"))))
  //      .add(ExecutionPlanNode::value_node(NodeValue::NULL))
  //  );
  //
  //  if !parameter_map.is_empty() {
  //    let parameter_path = DocPath::new_unwrap("parameters");
  //    for (k, v) in &parameter_map {
  //      let mut parameter_node = ExecutionPlanNode::container(k.as_str());
  //      parameter_node.add(
  //        ExecutionPlanNode::action("if")
  //          .add(ExecutionPlanNode::action("check:exists")
  //            .add(ExecutionPlanNode::resolve_current_value(&parameter_path.join(k.as_str()))))
  //          .add(ExecutionPlanNode::action("match:equality")
  //            .add(ExecutionPlanNode::value_node(v.to_lowercase()))
  //            .add(ExecutionPlanNode::action("lower-case")
  //              .add(ExecutionPlanNode::resolve_current_value(&parameter_path.join(k.as_str()))))
  //            .add(ExecutionPlanNode::value_node(NodeValue::NULL)))
  //          .add(ExecutionPlanNode::action("error")
  //            .add(ExecutionPlanNode::value_node(
  //              format!("Expected a {} value of '{}' but it was missing", k, v)))
  //          )
  //      );
  //      apply_node.add(parameter_node);
  //    }
  //  }
  //  apply_node
  //}

  fun setupBodyPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
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
