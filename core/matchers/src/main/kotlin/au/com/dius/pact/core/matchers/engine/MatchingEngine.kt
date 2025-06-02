package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.support.Result

interface MatchingEngine {
  /**
   * Constructs an execution plan for the HTTP request part.
   */
  fun buildRequestPlan(expectedRequest: HttpRequest, context: PlanMatchingContext): Result<ExecutionPlan, Exception>
}

object V2MatchingEngine: MatchingEngine {
  override fun buildRequestPlan(
    expectedRequest: HttpRequest,
    context: PlanMatchingContext
  ): Result<ExecutionPlan, Exception> {
    val plan = ExecutionPlan("request")

    plan.add(setupMethodPlan(expectedRequest, context.forMethod()))
    //  plan.add(setup_path_plan(expectedRequest, &context.for_path())?);
    //  plan.add(setup_query_plan(expectedRequest, &context.for_query())?);
    //  plan.add(setup_header_plan(expectedRequest, &context.for_headers())?);
    //  plan.add(setup_body_plan(expectedRequest, &context.for_body())?);

    return Result.Ok(plan)
  }

  fun setupMethodPlan(expected: HttpRequest, context: PlanMatchingContext): ExecutionPlanNode {
    val methodContainer = ExecutionPlanNode.container("method")

    val matchMethod = ExecutionPlanNode.action("match:equality")
    val expectedMethod = expected.method.uppercase()
//    match_method
//      .add(ExecutionPlanNode::value_node(expected_method.clone()))
//      .add(ExecutionPlanNode::action("upper-case")
//        .add(ExecutionPlanNode::resolve_value(DocPath::new("$.method")?)))
//      .add(ExecutionPlanNode::value_node(NodeValue::NULL));

    methodContainer.add(ExecutionPlanNode.annotation(Into { "method == $expectedMethod" }))
    methodContainer.add(matchMethod);

    return methodContainer
  }

  //fn setup_path_plan(
  //  expected: &HttpRequest,
  //  context: &PlanMatchingContext
  //) -> anyhow::Result<ExecutionPlanNode> {
  //  let mut plan_node = ExecutionPlanNode::container("path");
  //  let expected_node = ExecutionPlanNode::value_node(expected.path.as_str());
  //  let doc_path = DocPath::new("$.path")?;
  //  let actual_node = ExecutionPlanNode::resolve_value(doc_path.clone());
  //  if context.matcher_is_defined(&doc_path) {
  //    let matchers = context.select_best_matcher(&doc_path);
  //    plan_node.add(ExecutionPlanNode::annotation(format!("path {}", matchers.generate_description(false))));
  //    plan_node.add(build_matching_rule_node(&expected_node, &actual_node, &matchers, false));
  //  } else {
  //    plan_node.add(ExecutionPlanNode::annotation(format!("path == '{}'", expected.path)));
  //    plan_node
  //      .add(
  //        ExecutionPlanNode::action("match:equality")
  //          .add(expected_node)
  //          .add(ExecutionPlanNode::resolve_value(doc_path))
  //          .add(ExecutionPlanNode::value_node(NodeValue::NULL))
  //      );
  //  }
  //  Ok(plan_node)
  //}
  //
  //fn build_matching_rule_node(
  //  expected_node: &ExecutionPlanNode,
  //  actual_node: &ExecutionPlanNode,
  //  matchers: &RuleList,
  //  for_collection: bool
  //) -> ExecutionPlanNode {
  //  if matchers.rules.len() == 1 {
  //    let matcher = if for_collection {
  //      matchers.rules[0].clone()
  //    } else {
  //      matchers.rules[0].for_single_item()
  //    };
  //    let mut plan_node = ExecutionPlanNode::action(format!("match:{}", matcher.name()).as_str());
  //    plan_node
  //      .add(expected_node.clone())
  //      .add(actual_node.clone())
  //      .add(ExecutionPlanNode::value_node(matcher.values()));
  //    plan_node
  //  } else {
  //    let mut logic_node = match matchers.rule_logic {
  //      RuleLogic::And => ExecutionPlanNode::action("and"),
  //      RuleLogic::Or => ExecutionPlanNode::action("or")
  //    };
  //    for rule in &matchers.rules {
  //      let matcher = if for_collection {
  //        rule.clone()
  //      } else {
  //        rule.for_single_item()
  //      };
  //      logic_node
  //        .add(
  //          ExecutionPlanNode::action(format!("match:{}", matcher.name()).as_str())
  //            .add(expected_node.clone())
  //            .add(actual_node.clone())
  //            .add(ExecutionPlanNode::value_node(matcher.values()))
  //        );
  //    }
  //    logic_node
  //  }
  //}
  //
  //fn setup_query_plan(
  //  expected: &HttpRequest,
  //  context: &PlanMatchingContext
  //) -> anyhow::Result<ExecutionPlanNode> {
  //  let mut plan_node = ExecutionPlanNode::container("query parameters");
  //  let doc_path = DocPath::new("$.query")?;
  //
  //  if let Some(query) = &expected.query {
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
  //  } else {
  //    plan_node
  //      .add(
  //        ExecutionPlanNode::action("expect:empty")
  //          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
  //          .add(
  //            ExecutionPlanNode::action("join")
  //              .add(ExecutionPlanNode::value_node("Expected no query parameters but got "))
  //              .add(ExecutionPlanNode::resolve_value(doc_path))
  //          )
  //      );
  //  }
  //
  //  Ok(plan_node)
  //}
  //
  //fn setup_header_plan(
  //  expected: &HttpRequest,
  //  context: &PlanMatchingContext
  //) -> anyhow::Result<ExecutionPlanNode> {
  //  let mut plan_node = ExecutionPlanNode::container("headers");
  //  let doc_path = DocPath::new("$.headers")?;
  //
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
  //
  //  Ok(plan_node)
  //}
  //
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
  //
  //fn setup_body_plan(
  //  expected: &HttpRequest,
  //  context: &PlanMatchingContext
  //) -> anyhow::Result<ExecutionPlanNode> {
  //  // TODO: Look at the matching rules and generators here
  //  let mut plan_node = ExecutionPlanNode::container("body");
  //
  //  match &expected.body {
  //    OptionalBody::Missing => {}
  //    OptionalBody::Empty | OptionalBody::Null => {
  //      plan_node.add(ExecutionPlanNode::action("expect:empty")
  //        .add(ExecutionPlanNode::resolve_value(DocPath::new("$.body")?)));
  //    }
  //    OptionalBody::Present(content, _, _) => {
  //      let content_type = expected.content_type().unwrap_or_else(|| TEXT.clone());
  //      let mut content_type_check_node = ExecutionPlanNode::action("if");
  //      content_type_check_node
  //        .add(
  //          ExecutionPlanNode::action("match:equality")
  //            .add(ExecutionPlanNode::value_node(content_type.to_string()))
  //            .add(ExecutionPlanNode::resolve_value(DocPath::new("$.content-type")?))
  //            .add(ExecutionPlanNode::value_node(NodeValue::NULL))
  //            .add(
  //              ExecutionPlanNode::action("error")
  //                .add(ExecutionPlanNode::value_node(NodeValue::STRING("Body type error - ".to_string())))
  //                .add(ExecutionPlanNode::action("apply"))
  //            )
  //        );
  //      if let Some(plan_builder) = get_body_plan_builder(&content_type) {
  //        content_type_check_node.add(plan_builder.build_plan(content, context)?);
  //      } else {
  //        let plan_builder = PlainTextBuilder::new();
  //        content_type_check_node.add(plan_builder.build_plan(content, context)?);
  //      }
  //      plan_node.add(content_type_check_node);
  //    }
  //  }
  //
  //  Ok(plan_node)
  //}
}
