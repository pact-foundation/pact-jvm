//! This module provides the interpreter that can execute a matching plan AST

use std::collections::{HashSet, VecDeque};
use std::iter::once;

use anyhow::anyhow;
use itertools::Itertools;
use maplit::hashset;
use serde_json::{json, Value};
#[cfg(feature = "xml")] use snailquote::unescape;
use tracing::{debug, error, instrument, trace, Level};

use pact_models::matchingrules::MatchingRule;
use pact_models::path_exp::{DocPath, PathToken};
#[cfg(feature = "xml")] use pact_models::xml_utils::resolve_matching_node;
use crate::engine::{ExecutionPlanNode, NodeResult, NodeValue, PlanNodeType};
use crate::engine::context::PlanMatchingContext;
use crate::engine::value_resolvers::ValueResolver;
#[cfg(feature = "xml")] use crate::engine::xml::XmlValue;
use crate::headers::{parse_charset_parameters, strip_whitespace};
use crate::json::type_of;
use crate::matchers::Matches;

/// Main interpreter for the matching plan AST
#[derive(Debug)]
pub struct ExecutionPlanInterpreter {
  /// Stack of intermediate values (used by the pipeline operator and apply action)
  value_stack: Vec<Option<NodeResult>>,
  /// Context to use to execute the plan
  context: PlanMatchingContext
}

impl ExecutionPlanInterpreter {
  /// Creates a new interpreter
  pub fn new() -> Self {
    ExecutionPlanInterpreter {
      value_stack: vec![],
      context: PlanMatchingContext::default()
    }
  }

  /// Creates a new interpreter with a given test context
  pub fn new_with_context(context: &PlanMatchingContext) -> Self {
    ExecutionPlanInterpreter {
      value_stack: vec![],
      context: context.clone()
    }
  }

  /// Walks the tree from a given node, executing all visited nodes
  pub fn walk_tree(
    &mut self,
    path: &[String],
    node: &ExecutionPlanNode,
    value_resolver: &dyn ValueResolver
  ) -> anyhow::Result<ExecutionPlanNode> {
    match &node.node_type {
      PlanNodeType::EMPTY => {
        trace!(?path, "walk_tree ==> Empty node");
        Ok(node.clone())
      },
      PlanNodeType::CONTAINER(label) => {
        trace!(?path, %label, "walk_tree ==> Container node");

        let mut result = vec![];
        let mut child_path = path.to_vec();
        child_path.push(label.clone());
        let mut status = NodeResult::OK;
        let mut loop_items = VecDeque::from(node.children.clone());

        while !loop_items.is_empty() {
          let child = loop_items.pop_front().unwrap();
          let child_result = self.walk_tree(&child_path, &child, value_resolver)?;
          status = status.and(&child_result.result);
          result.push(child_result.clone());
          if child_result.is_splat() {
            for item in child_result.children.iter().rev() {
              loop_items.push_front(item.clone());
            }
          }
        }

        Ok(ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(status.truthy()),
          children: result
        })
      }
      PlanNodeType::ACTION(action) => {
        trace!(?path, %action, "walk_tree ==> Action node");
        Ok(self.execute_action(action.as_str(), value_resolver, node, path))
      }
      PlanNodeType::VALUE(val) => {
        trace!(?path, ?val, "walk_tree ==> Value node");
        let value = match val {
          NodeValue::NAMESPACED(namespace, value) => match namespace.as_str() {
            "json" => serde_json::from_str(value.as_str())
              .map(|v| NodeValue::JSON(v))
              .map_err(|err| anyhow!(err)),
            #[cfg(feature = "xml")]
            "xml" => kiss_xml::parse_str(unescape(value).unwrap_or_else(|_| value.clone()))
              .map(|doc| NodeValue::XML(XmlValue::Element(doc.root_element().clone())))
              .map_err(|err| anyhow!("Failed to parse XML value: {}", err)),
            _ => Err(anyhow!("'{}' is not a known namespace", namespace))
          }
          _ => Ok(val.clone())
        }?;
        Ok(ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::VALUE(value)),
          children: vec![]
        })
      }
      PlanNodeType::RESOLVE(resolve_path) => {
        trace!(?path, %resolve_path, "walk_tree ==> Resolve node");
        match value_resolver.resolve(resolve_path, &self.context) {
          Ok(val) => {
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::VALUE(val.clone())),
              children: vec![]
            })
          }
          Err(err) => {
            trace!(?path, %resolve_path, %err, "Resolve node failed");
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![]
            })
          }
        }
      }
      PlanNodeType::PIPELINE => {
        trace!(?path, "walk_tree ==> Apply pipeline node");

        let child_path = path.to_vec();
        self.push_result(None);
        let mut child_results = vec![];
        let mut loop_items = VecDeque::from(node.children.clone());

        // TODO: Need a short circuit here if any child results in an error
        while !loop_items.is_empty() {
          let child = loop_items.pop_front().unwrap();
          let child_result = self.walk_tree(&child_path, &child, value_resolver)?;
          self.update_result(child_result.result.clone());
          child_results.push(child_result.clone());
          if child_result.is_splat() {
            for item in child_result.children.iter().rev() {
              loop_items.push_front(item.clone());
            }
          }
        }

        let result = self.pop_result();
        match result {
          Some(value) => {
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(value),
              children: child_results
            })
          }
          None => {
            trace!(?path, "Value from stack is empty");
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR("Value from stack is empty".to_string())),
              children: child_results
            })
          }
        }
      }
      PlanNodeType::RESOLVE_CURRENT(expression) => {
        trace!(?path, %expression, "walk_tree ==> Resolve current node");
        match self.resolve_stack_value(expression) {
          Ok(val) => {
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::VALUE(val.clone())),
              children: vec![]
            })
          }
          Err(err) => {
            debug!(?path, %expression, %err, "Resolve node failed");
            Ok(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![]
            })
          }
        }
      }
      PlanNodeType::SPLAT => {
        trace!(?path, "walk_tree ==> Apply splat node");

        let child_path = path.to_vec();
        let mut child_results = vec![];

        // TODO: Need a short circuit here if any child results in an error
        for child in &node.children {
          let child_result = self.walk_tree(&child_path, child, value_resolver)?;
          match &child_result.result {
            None => child_results.push(child_result.clone()),
            Some(result) => match result {
              NodeResult::OK => child_results.push(child_result.clone()),
              NodeResult::VALUE(value) => match value {
                NodeValue::MMAP(map) => {
                  for (key, value) in map {
                    child_results.push(child_result.clone_with_value(NodeValue::ENTRY(key.clone(), Box::new(NodeValue::SLIST(value.clone())))));
                  }
                }
                NodeValue::SLIST(list) => {
                  for item in list {
                    child_results.push(child_result.clone_with_value(NodeValue::STRING(item.clone())));
                  }
                }
                _ => child_results.push(child_result.clone())
              }
              NodeResult::ERROR(_) => child_results.push(child_result.clone())
            }
          }
        }

        Ok(ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::OK),
          children: child_results
        })
      }
      PlanNodeType::ANNOTATION(_) => Ok(node.clone())
    }
  }

  /// Execute the action
  #[instrument(ret, skip_all, level = Level::TRACE, fields(action, path, node))]
  pub fn execute_action(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &[String]
  ) -> ExecutionPlanNode {
    trace!(%action, "Executing action");

    let mut action_path = path.to_vec();
    action_path.push(action.to_string());

    if action.starts_with("match:") {
      match action.strip_prefix("match:") {
        None => {
          ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(format!("'{}' is not a valid action", action))),
            children: node.children.clone()
          }
        }
        Some(matcher) => self.execute_match(action, matcher, value_resolver, node, &action_path)
          .unwrap_or_else(|node| node)
      }
    } else {
      match action {
        "upper-case" => self.execute_change_case(action, value_resolver, node, &action_path, true),
        "lower-case" => self.execute_change_case(action, value_resolver, node, &action_path, false),
        "to-string" => self.execute_to_string(action, value_resolver, node, &action_path),
        "length" => self.execute_length(action, value_resolver, node, &action_path),
        "expect:empty" => self.execute_expect_empty(action, value_resolver, node, &action_path),
        "convert:UTF8" => self.execute_convert_utf8(action, value_resolver, node, &action_path),
        "if" => self.execute_if(value_resolver, node, &action_path),
        "and" => self.execute_and(value_resolver, node, &action_path),
        "or" => self.execute_or(value_resolver, node, &action_path),
        "tee" => self.execute_tee(value_resolver, node, &action_path),
        "apply" => self.execute_apply(node),
        "push" => self.execute_push(node),
        "pop" => self.execute_pop(node),
        "json:parse" => self.execute_json_parse(action, value_resolver, node, &action_path),
        #[cfg(feature = "xml")]
        "xml:parse" => self.execute_xml_parse(action, value_resolver, node, &action_path),
        #[cfg(feature = "xml")]
        "xml:tag-name" => self.execute_xml_tag_name(action, value_resolver, node, &action_path),
        #[cfg(feature = "xml")]
        "xml:value" => self.execute_xml_value(action, value_resolver, node, &action_path),
        #[cfg(feature = "xml")]
        "xml:attributes" => self.execute_xml_attributes(action, value_resolver, node, &action_path),
        "json:expect:empty" => self.execute_json_expect_empty(action, value_resolver, node, &action_path),
        "json:match:length" => self.execute_json_match_length(action, value_resolver, node, &action_path),
        "json:expect:entries" => self.execute_json_expect_entries(action, value_resolver, node, &action_path),
        "check:exists" => self.execute_check_exists(action, value_resolver, node, &action_path),
        "expect:entries" => self.execute_check_entries(action, value_resolver, node, &action_path),
        "expect:only-entries" => self.execute_check_entries(action, value_resolver, node, &action_path),
        "expect:count" => self.execute_expect_count(action, value_resolver, node, &action_path),
        "join" => self.execute_join(action, value_resolver, node, &action_path),
        "join-with" => self.execute_join(action, value_resolver, node, &action_path),
        "error" => self.execute_error(action, value_resolver, node, &action_path),
        "header:parse" => self.execute_header_parse(action, value_resolver, node, &action_path),
        "for-each" => self.execute_for_each(value_resolver, node, &action_path),
        _ => {
          ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(format!("'{}' is not a valid action", action))),
            children: node.children.clone()
          }
        }
      }
    }
  }

  fn execute_json_expect_entries(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_three_args(node, action, value_resolver, &action_path) {
      Ok((first_node, second_node, third_node)) => {
        let result1 = first_node.value().unwrap_or_default();
        let expected_json_type = match result1.as_string() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("'{}' is not a valid JSON type", result1))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(str) => str
        };
        let result2 = second_node.value().unwrap_or_default();
        let expected_keys = match result2.as_slist() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("'{}' is not a list of Strings", result2))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(list) => list.iter()
            .cloned()
            .collect::<HashSet<_>>()
        };
        let result3 = third_node.value().unwrap_or_default();
        let value = match result3.as_value() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {}", result3))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(value) => value
        };
        let json_value = match &value {
          NodeValue::JSON(json) => json,
          _ => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {:?}", value))),
              children: vec![first_node, second_node, third_node]
            }
          }
        };
        if let Err(err) = json_check_type(expected_json_type, json_value) {
          return ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: vec![first_node, second_node, third_node]
          }
        }

        match json_value {
          Value::Object(o) => {
            let actual_keys = o.keys()
              .cloned()
              .collect::<HashSet<_>>();
            let diff = &expected_keys - &actual_keys;
            if diff.is_empty() {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(NodeResult::VALUE(NodeValue::BOOL(true))),
                children: vec![first_node, second_node, third_node]
              }
            } else {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(
                  NodeResult::ERROR(
                    format!("The following expected entries were missing from the actual Object: {}",
                            diff.iter().join(", "))
                  )
                ),
                children: vec![first_node, second_node, third_node]
              }
            }
          }
          _ => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON Object, but got {:?}", json_value))),
              children: vec![first_node, second_node, third_node]
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_json_match_length(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_three_args(node, action, value_resolver, &action_path) {
      Ok((first_node, second_node, third_node)) => {
        let result1 = first_node.value().unwrap_or_default();
        let expected_json_type = match result1.as_string() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("'{}' is not a valid JSON type", result1))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(str) => str
        };
        let result2 = second_node.value().unwrap_or_default();
        let expected_length = match result2.as_number() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("'{}' is not a valid number", result2))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(length) => length
        };
        let result3 = third_node.value().unwrap_or_default();
        let value = match result3.as_value() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {}", result3))),
              children: vec![first_node, second_node, third_node]
            }
          }
          Some(value) => value
        };
        let json_value = match value {
          NodeValue::JSON(json) => json,
          _ => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {:?}", value))),
              children: vec![first_node, second_node, third_node]
            }
          }
        };
        if let Err(err) = json_check_type(expected_json_type, &json_value) {
          return ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: vec![first_node, second_node, third_node]
          }
        }
        if let Err(err) = json_check_length(expected_length as usize, &json_value) {
          return ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: vec![first_node, second_node, third_node]
          }
        }
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::VALUE(NodeValue::BOOL(true))),
          children: vec![first_node, second_node, third_node]
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_json_expect_empty(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_two_args(node, action, value_resolver, &action_path) {
      Ok((first_node, second_node)) => {
        let result1 = first_node.value().unwrap_or_default();
        let expected_json_type = match result1.as_string() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("'{}' is not a valid JSON type", result1))),
              children: vec![first_node, second_node]
            }
          }
          Some(str) => str
        };
        let result2 = second_node.value().unwrap_or_default();
        let value = match result2.as_value() {
          None => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {}", result2))),
              children: vec![first_node, second_node]
            }
          }
          Some(value) => value
        };
        let json_value = match value {
          NodeValue::JSON(json) => json,
          _ => {
            return ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(format!("Was expecting a JSON value, but got {:?}", value))),
              children: vec![first_node, second_node]
            }
          }
        };
        if let Err(err) = json_check_type(expected_json_type, &json_value) {
          return ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: vec![first_node, second_node]
          }
        };
        let result = match &json_value {
          Value::Null => Ok(NodeResult::VALUE(NodeValue::BOOL(true))),
          Value::String(s) => if s.is_empty() {
            Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
          } else {
            Err(anyhow!("Expected JSON String ({}) to be empty", json_value))
          }
          Value::Array(a) => if a.is_empty() {
            Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
          } else {
            Err(anyhow!("Expected JSON Array ({}) to be empty", json_value))
          }
          Value::Object(o) => if o.is_empty() {
            Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
          } else {
            Err(anyhow!("Expected JSON Object ({}) to be empty", json_value))
          }
          _ => Err(anyhow!("Expected json ({}) to be empty", json_value))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![first_node, second_node]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![first_node, second_node]
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_json_parse(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::NULL => Ok(NodeResult::VALUE(NodeValue::NULL)),
            NodeValue::STRING(s) => serde_json::from_str(s.as_str())
              .map(|json| NodeResult::VALUE(NodeValue::JSON(json)))
              .map_err(|err| anyhow!("json parse error - {}", err)),
            NodeValue::BARRAY(b) => serde_json::from_slice(b.as_slice())
              .map(|json| NodeResult::VALUE(NodeValue::JSON(json)))
              .map_err(|err| anyhow!("json parse error - {}", err)),
            _ => Err(anyhow!("json:parse can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::NULL))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  #[cfg(feature = "xml")]
  fn execute_xml_parse(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::NULL => Ok(NodeResult::VALUE(NodeValue::NULL)),
            NodeValue::STRING(s) => {
              kiss_xml::parse_str(s)
                .map(|doc| NodeResult::VALUE(NodeValue::XML(XmlValue::Element(doc.root_element().clone()))))
                .map_err(|err| anyhow!("XML parse error - {}", err))
            }
            NodeValue::BARRAY(b) => {
              kiss_xml::parse_str(String::from_utf8_lossy(b.as_slice()))
                .map(|doc| NodeResult::VALUE(NodeValue::XML(XmlValue::Element(doc.root_element().clone()))))
                .map_err(|err| anyhow!("XML parse error - {}", err))
            }
            _ => Err(anyhow!("xml:parse can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::NULL))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => node.clone_with_result(NodeResult::ERROR(err.to_string()))
    }
  }

  #[cfg(feature = "xml")]
  fn execute_xml_tag_name(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::XML(xml) => match xml {
              XmlValue::Element(element) => Ok(NodeResult::VALUE(NodeValue::STRING(element.name()))),
              _ => Err(anyhow!("xml:tag-name can not be used with {}", xml))
            }
            _ => Err(anyhow!("xml:tag-name can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::NULL))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => node.clone_with_result(NodeResult::ERROR(err.to_string()))
    }
  }

  #[cfg(feature = "xml")]
  fn execute_xml_value(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::XML(xml) => match xml {
              XmlValue::Attribute(_, value) => Ok(NodeResult::VALUE(NodeValue::STRING(value.clone()))),
              _ => Err(anyhow!("xml:value can not be used with {}", xml))
            }
            _ => Err(anyhow!("xml:value can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::NULL))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => node.clone_with_result(NodeResult::ERROR(err.to_string()))
    }
  }

  #[cfg(feature = "xml")]
  fn execute_xml_attributes(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::XML(xml) => match xml {
              XmlValue::Attribute(name, value) => Ok(NodeResult::VALUE(NodeValue::ENTRY(name.clone(), Box::new(NodeValue::STRING(value.clone()))))),
              XmlValue::Element(element) => Ok(NodeResult::VALUE(NodeValue::MMAP(element.attributes().iter()
                .map(|(k, v)| (k.clone(), vec![v.clone()]))
                .collect()))),
              _ => Err(anyhow!("xml:attributes can not be used with {}", xml))
            }
            _ => Err(anyhow!("xml:attributes can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::NULL))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => node.clone_with_result(NodeResult::ERROR(err.to_string()))
    }
  }

  fn execute_pop(&mut self, node: &ExecutionPlanNode) -> ExecutionPlanNode {
    if let Some(_value) = self.value_stack.pop() {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: self.value_stack.last().cloned().flatten(),
        children: node.children.clone()
      }
    } else {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: Some(NodeResult::ERROR("No value to pop (stack is empty)".to_string())),
        children: node.children.clone()
      }
    }
  }

  fn execute_push(&mut self, node: &ExecutionPlanNode) -> ExecutionPlanNode {
    let last_value = self.value_stack.last().cloned();
    if let Some(value) = last_value {
      self.value_stack.push(value.clone());
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: value,
        children: node.children.clone()
      }
    } else {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: Some(NodeResult::ERROR("No value to push (value is empty)".to_string())),
        children: node.children.clone()
      }
    }
  }

  fn execute_apply(&mut self, node: &ExecutionPlanNode) -> ExecutionPlanNode {
    if let Some(value) = self.value_stack.last() {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: value.clone(),
        children: node.children.clone()
      }
    } else {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: Some(NodeResult::ERROR("No value to apply (stack is empty)".to_string())),
        children: node.children.clone()
      }
    }
  }

  fn execute_if(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    if let Some(first_node) = node.children.first() {
      match self.walk_tree(action_path.as_slice(), first_node, value_resolver) {
        Ok(first) => {
          let node_result = first.value().unwrap_or_default();
          let mut children = node.children.clone();
          children[0] = first.clone();
          if !node_result.is_truthy() {
            if node.children.len() > 2 {
              match self.walk_tree(action_path.as_slice(), &node.children[2], value_resolver) {
                Ok(else_node) => {
                  children[2] = else_node.clone();
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: else_node.result.clone(),
                    children
                  }
                }
                Err(err) => {
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(err.to_string())),
                    children
                  }
                }
              }
            } else {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(NodeResult::VALUE(NodeValue::BOOL(false))),
                children
              }
            }
          } else if let Some(second_node) = node.children.get(1) {
            match self.walk_tree(action_path.as_slice(), second_node, value_resolver) {
              Ok(second) => {
                let second_result = second.value().unwrap_or_default();
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(second_result.truthy()),
                  children: vec![first, second].iter()
                    .chain(node.children.iter().dropping(2))
                    .cloned()
                    .collect()
                }
              }
              Err(err) => {
                error!("Failed to evaluate the second child - {}", err);
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::VALUE(NodeValue::BOOL(false))),
                  children: vec![first, second_node.clone()].iter()
                    .chain(node.children.iter().dropping(2))
                    .cloned()
                    .collect()
                }
              }
            }
          } else {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(node_result),
              children: vec![first].iter().chain(node.children.iter().dropping(1))
                .cloned()
                .collect()
            }
          }
        }
        Err(err) => {
          ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: node.children.clone()
          }
        }
      }
    } else {
      ExecutionPlanNode {
        node_type: node.node_type.clone(),
        result: Some(NodeResult::ERROR("'if' action requires at least one argument".to_string())),
        children: node.children.clone()
      }
    }
  }

  fn execute_tee(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    if let Some(first_node) = node.children.first() {
      match self.walk_tree(action_path.as_slice(), first_node, value_resolver) {
        Ok(first) => {
          let first_result = first.value().unwrap_or_default();
          if first_result.is_err() {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(first_result.clone()),
              children: once(first).chain(node.children.iter().dropping(1).cloned()).collect()
            }
          } else {
            let mut result = NodeResult::OK;
            self.push_result(first.result.clone());
            let mut child_results = vec![first.clone()];
            for child in node.children.iter().dropping(1) {
              match self.walk_tree(&action_path, &child, value_resolver) {
                Ok(value) => {
                  result = result.and(&value.result);
                  child_results.push(value.clone());
                }
                Err(err) => {
                  let node_result = NodeResult::ERROR(err.to_string());
                  result = result.and(&Some(node_result.clone()));
                  child_results.push(child.clone_with_result(node_result));
                }
              }
            }

            self.pop_result();
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result.truthy()),
              children: child_results
            }
          }
        }
        Err(err) => node.clone_with_result(NodeResult::ERROR(err.to_string()))
      }
    } else {
      node.clone_with_result(NodeResult::OK)
    }
  }

  fn execute_convert_utf8(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value().unwrap_or_default().as_value();
        let result = if let Some(value) = &arg_value {
          match value {
            NodeValue::NULL => Ok(NodeResult::VALUE(NodeValue::STRING("".to_string()))),
            NodeValue::STRING(s) => Ok(NodeResult::VALUE(NodeValue::STRING(s.clone()))),
            NodeValue::BARRAY(b) => Ok(NodeResult::VALUE(NodeValue::STRING(String::from_utf8_lossy(b).to_string()))),
            _ => Err(anyhow!("convert:UTF8 can not be used with {}", value.value_type()))
          }
        } else {
          Ok(NodeResult::VALUE(NodeValue::STRING("".to_string())))
        };
        match result {
          Ok(result) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result),
              children: vec![value]
            }
          }
          Err(err) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: vec![value]
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_expect_empty(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_args(1, 1, node, action, value_resolver, &action_path) {
      Ok((values, optional)) => {
        let first = values.first().unwrap().value().unwrap_or_default();
        if let NodeResult::ERROR(err) = first  {
          ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: values.iter().chain(optional.iter()).cloned().collect()
          }
        } else {
          let arg_value = first.as_value();
          let result = if let Some(value) = &arg_value {
            match value {
              NodeValue::NULL => Ok(NodeResult::VALUE(NodeValue::BOOL(true))),
              NodeValue::STRING(s) => if s.is_empty() {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected {:?} to be empty", value))
              }
              NodeValue::BOOL(b) => Ok(NodeResult::VALUE(NodeValue::BOOL(*b))),
              NodeValue::MMAP(m) => if m.is_empty() {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected {} to be empty", value))
              }
              NodeValue::SLIST(l) => if l.is_empty() {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected {} to be empty", value))
              },
              NodeValue::BARRAY(bytes) => if bytes.is_empty() {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected byte array ({} bytes) to be empty", bytes.len()))
              },
              NodeValue::NAMESPACED(_, _) => { todo!("Not Implemented: Need a way to resolve NodeValue::NAMESPACED") }
              NodeValue::UINT(ui) => if *ui == 0 {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected {:?} to be empty", value))
              },
              NodeValue::JSON(json) => match json {
                Value::Null => Ok(NodeResult::VALUE(NodeValue::BOOL(true))),
                Value::String(s) => if s.is_empty() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected JSON String ({}) to be empty", json))
                }
                Value::Array(a) => if a.is_empty() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected JSON Array ({}) to be empty", json))
                }
                Value::Object(o) => if o.is_empty() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected JSON Object ({}) to be empty", json))
                }
                _ => Err(anyhow!("Expected json ({}) to be empty", json))
              },
              NodeValue::ENTRY(_, _) =>  Ok(NodeResult::VALUE(NodeValue::BOOL(false))),
              NodeValue::LIST(l) => if l.is_empty() {
                Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
              } else {
                Err(anyhow!("Expected {} to be empty", value))
              },
              #[cfg(feature = "xml")]
              NodeValue::XML(xml) => match xml {
                XmlValue::Element(element) => if element.child_elements().next().is_none() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected {} to be empty", element))
                }
                XmlValue::Text(text) => if text.is_empty() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected {:?} to be empty", value))
                }
                XmlValue::Attribute(name, value) => if value.is_empty() {
                  Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
                } else {
                  Err(anyhow!("Expected {}={} to be empty", name, value))
                }
              }
            }
          } else {
            Ok(NodeResult::VALUE(NodeValue::BOOL(true)))
          };
          match result {
            Ok(result) => {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(result),
                children: values.iter().chain(optional.iter()).cloned().collect()
              }
            }
            Err(err) => {
              debug!("expect:empty failed with an error: {}", err);
              if optional.len() > 0 {
                if let Ok(value) = self.walk_tree(action_path.as_slice(), &optional[0], value_resolver) {
                  let message = value.value().unwrap_or_default().as_string().unwrap_or_default();
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(message)),
                    children: values.iter().chain(once(&value)).cloned().collect()
                  }
                } else {
                  // There was an error generating the optional message, so just return the
                  // original error
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(err.to_string())),
                    children: values.iter().chain(optional.iter()).cloned().collect()
                  }
                }
              } else {
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::ERROR(err.to_string())),
                  children: values.iter().chain(optional.iter()).cloned().collect()
                }
              }
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_match(
    &mut self,
    action: &str,
    matcher: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> Result<ExecutionPlanNode, ExecutionPlanNode> {
    match self.validate_args(3, 1, node, action, value_resolver, &action_path) {
      Ok((args, optional)) => {
        let first_node = &args[0];
        let second_node = &args[1];
        let third_node = &args[2];

        let exepected_value = first_node.value()
          .unwrap_or_default()
          .value_or_error()
          .map_err(|err| {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: [first_node.clone(), second_node.clone(), third_node.clone()]
                .iter()
                .chain(optional.iter())
                .cloned()
                .collect()
            }
          })?;

        let actual_value = second_node.value()
          .unwrap_or_default()
          .value_or_error()
          .map_err(|err| {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: [first_node.clone(), second_node.clone(), third_node.clone()]
                .iter()
                .chain(optional.iter())
                .cloned()
                .collect()
            }
          })?;

        let matcher_params = third_node.value()
          .unwrap_or_default()
          .value_or_error()
          .map_err(|err| {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: [first_node.clone(), second_node.clone(), third_node.clone()]
                .iter()
                .chain(optional.iter())
                .cloned()
                .collect()
            }
          })?
          .as_json()
          .unwrap_or_default();

        match MatchingRule::create(matcher, &matcher_params) {
          Ok(rule) => {
            match exepected_value.matches_with(actual_value, &rule, false) {
              Ok(_) => {
                Ok(ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::VALUE(NodeValue::BOOL(true))),
                  children: [first_node.clone(), second_node.clone(), third_node.clone()]
                    .iter()
                    .chain(optional.iter())
                    .cloned()
                    .collect()
                })
              }
              Err(err) => {
                if let Some(error_node) = optional.first() {
                  self.push_result(Some(NodeResult::ERROR(err.to_string())));
                  match self.walk_tree(action_path.as_slice(), error_node, value_resolver) {
                    Ok(error_node) => {
                      let message = error_node.value().unwrap_or_default().as_string().unwrap_or_default();
                      Err(ExecutionPlanNode {
                        node_type: node.node_type.clone(),
                        result: Some(NodeResult::ERROR(message)),
                        children: vec![first_node.clone(), second_node.clone(), third_node.clone(), error_node.clone()]
                      })
                    }
                    Err(_) => {
                      error!("Failed to generate error node - {}", err);
                      // There was an error generating the optional error node, so just return the
                      // original error
                      Err(ExecutionPlanNode {
                        node_type: node.node_type.clone(),
                        result: Some(NodeResult::ERROR(err.to_string())),
                        children: [first_node.clone(), second_node.clone(), third_node.clone()]
                          .iter()
                          .chain(optional.iter())
                          .cloned()
                          .collect()
                      })
                    }
                  }
                } else {
                  Err(ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(err.to_string())),
                    children: [first_node.clone(), second_node.clone(), third_node.clone()]
                      .iter()
                      .chain(optional.iter())
                      .cloned()
                      .collect()
                  })
                }
              }
            }
          }
          Err(err) => Err(node.clone_with_result(NodeResult::ERROR(err.to_string())))
        }
      }
      Err(err) => Err(node.clone_with_result(NodeResult::ERROR(err.to_string())))
    }
  }

  fn execute_change_case(
    &mut self,
    _action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>,
    upper_case: bool
  ) -> ExecutionPlanNode {
    let (children, values) = match self.evaluate_children(value_resolver, node, action_path) {
      Ok(value) => value,
      Err(value) => return value
    };

    let results = values.iter()
      .map(|v| {
        if upper_case {
          match v {
            NodeValue::STRING(s) => NodeValue::STRING(s.to_uppercase()),
            NodeValue::SLIST(list) => NodeValue::SLIST(list.iter().map(|s| s.to_uppercase()).collect()),
            NodeValue::JSON(json) => match json {
              Value::String(s) => NodeValue::STRING(s.to_uppercase()),
              _ => NodeValue::STRING(json.to_string())
            }
            _ => v.clone()
          }
        } else {
          match v {
            NodeValue::STRING(s) => NodeValue::STRING(s.to_lowercase()),
            NodeValue::SLIST(list) => NodeValue::SLIST(list.iter().map(|s| s.to_lowercase()).collect()),
            NodeValue::JSON(json) => match json {
              Value::String(s) => NodeValue::STRING(s.to_lowercase()),
              _ => NodeValue::STRING(json.to_string())
            }
            _ => v.clone()
          }
        }
      })
      .collect_vec();
    let result = if results.len() == 1 {
      results[0].clone()
    } else {
      NodeValue::LIST(results)
    };
    ExecutionPlanNode {
      node_type: node.node_type.clone(),
      result: Some(NodeResult::VALUE(result)),
      children
    }
  }

  fn execute_to_string(
    &mut self,
    _action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    let (children, values) = match self.evaluate_children(value_resolver, node, action_path) {
      Ok(value) => value,
      Err(value) => return value
    };

    let results = values.iter()
      .map(|v| {
        match v {
          NodeValue::NULL => NodeValue::STRING(String::default()),
          NodeValue::STRING(_) => v.clone(),
          NodeValue::SLIST(_) => v.clone(),
          NodeValue::JSON(json) => match json {
            Value::String(s) => NodeValue::STRING(s.clone()),
            _ => NodeValue::STRING(json.to_string())
          }
          #[cfg(feature = "xml")]
          NodeValue::XML(xml) => match xml {
            XmlValue::Element(element) => NodeValue::STRING(element.to_string()),
            XmlValue::Text(text) => NodeValue::STRING(text.clone()),
            XmlValue::Attribute(name, value) => NodeValue::STRING(format!("@{}='{}'", name, value))
          }
          _ => NodeValue::STRING(v.str_form())
        }
      })
      .collect_vec();
    let result = if results.len() == 1 {
      results[0].clone()
    } else {
      NodeValue::LIST(results)
    };
    ExecutionPlanNode {
      node_type: node.node_type.clone(),
      result: Some(NodeResult::VALUE(result)),
      children
    }
  }

  fn execute_length(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let result = value.value()
          .unwrap_or_default()
          .as_value()
          .unwrap_or_default();
        let result = match result {
          NodeValue::NULL => NodeResult::VALUE(NodeValue::UINT(0)),
          NodeValue::STRING(s) => NodeResult::VALUE(NodeValue::UINT(s.len() as u64)),
          NodeValue::MMAP(m) => NodeResult::VALUE(NodeValue::UINT(m.len() as u64)),
          NodeValue::SLIST(l) => NodeResult::VALUE(NodeValue::UINT(l.len() as u64)),
          NodeValue::BARRAY(a) => NodeResult::VALUE(NodeValue::UINT(a.len() as u64)),
          NodeValue::JSON(json) => match json {
            Value::String(s) => NodeResult::VALUE(NodeValue::UINT(s.len() as u64)),
            Value::Array(a) => NodeResult::VALUE(NodeValue::UINT(a.len() as u64)),
            Value::Object(m) => NodeResult::VALUE(NodeValue::UINT(m.len() as u64)),
            _ => NodeResult::ERROR(format!("'length' can't be used with a {:?} node", value))
          }
          NodeValue::LIST(l) => NodeResult::VALUE(NodeValue::UINT(l.len() as u64)),
          #[cfg(feature = "xml")]
          NodeValue::XML(xml) => match xml {
            XmlValue::Element(_) => NodeResult::VALUE(NodeValue::UINT(1)),
            XmlValue::Text(text) => NodeResult::VALUE(NodeValue::UINT(text.len() as u64)),
            XmlValue::Attribute(_, _) => NodeResult::VALUE(NodeValue::UINT(1))
          }
          _ => NodeResult::ERROR(format!("'length' can't be used with a {:?} node", value))
        };
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(result),
          children: vec![ value.clone() ]
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_check_exists(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let result = if let NodeResult::VALUE(value) = value.value().unwrap_or_default() {
          match value {
            NodeValue::NULL => NodeResult::VALUE(NodeValue::BOOL(false)),
            _ => NodeResult::VALUE(NodeValue::BOOL(true))
          }
        } else {
          NodeResult::VALUE(NodeValue::BOOL(false))
        };
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(result),
          children: vec![value]
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  /// Push a result value onto the value stack
  fn push_result(&mut self, value: Option<NodeResult>) {
    self.value_stack.push(value);
  }

  /// Replace the top value of the stack with the new value
  fn update_result(&mut self, value: Option<NodeResult>) {
    if let Some(current) = self.value_stack.last_mut() {
      *current = value;
    } else {
      self.value_stack.push(value);
    }
  }

  /// Return the value on the top if the stack
  fn pop_result(&mut self) -> Option<NodeResult> {
    self.value_stack.pop().flatten()
  }

  /// Return the current stack value
  fn stack_value(&self) -> Option<NodeResult> {
    self.value_stack.last().cloned().flatten()
  }

  fn validate_one_arg(
    &mut self,
    node: &ExecutionPlanNode,
    action: &str,
    value_resolver: &dyn ValueResolver,
    path: &Vec<String>
  ) -> anyhow::Result<ExecutionPlanNode> {
    if node.children.len() > 1 {
      Err(anyhow!("{} takes only one argument, got {}", action, node.children.len()))
    } else if let Some(argument) = node.children.first() {
      self.walk_tree(path.as_slice(), argument, value_resolver)
    } else {
      Err(anyhow!("{} requires one argument, got none", action))
    }
  }

  fn validate_two_args(
    &mut self,
    node: &ExecutionPlanNode,
    action: &str,
    value_resolver: &dyn ValueResolver,
    path: &Vec<String>
  ) -> anyhow::Result<(ExecutionPlanNode, ExecutionPlanNode)> {
    if node.children.len() == 2 {
      let first = self.walk_tree(path.as_slice(), &node.children[0], value_resolver)?;
      let second = self.walk_tree(path.as_slice(), &node.children[1], value_resolver)?;
      Ok((first, second))
    } else {
      Err(anyhow!("Action '{}' requires two arguments, got {}", action, node.children.len()))
    }
  }

  fn validate_three_args(
    &mut self,
    node: &ExecutionPlanNode,
    action: &str,
    value_resolver: &dyn ValueResolver,
    path: &Vec<String>
  ) -> anyhow::Result<(ExecutionPlanNode, ExecutionPlanNode, ExecutionPlanNode)> {
    if node.children.len() == 3 {
      let first = self.walk_tree(path.as_slice(), &node.children[0], value_resolver)?;
      let second = self.walk_tree(path.as_slice(), &node.children[1], value_resolver)?;
      let third = self.walk_tree(path.as_slice(), &node.children[2], value_resolver)?;
      Ok((first, second, third))
    } else {
      Err(anyhow!("Action '{}' requires three arguments, got {}", action, node.children.len()))
    }
  }

  fn validate_args(
    &mut self,
    required: usize,
    optional: usize,
    node: &ExecutionPlanNode,
    action: &str,
    value_resolver: &dyn ValueResolver,
    path: &Vec<String>
  ) -> anyhow::Result<(Vec<ExecutionPlanNode>, Vec<ExecutionPlanNode>)> {
    if node.children.len() < required {
      Err(anyhow!("{} requires {} arguments, got {}", action, required, node.children.len()))
    } else if node.children.len() > required + optional {
      Err(anyhow!("{} supports at most {} arguments, got {}", action, optional, node.children.len()))
    } else {
      let mut required_args = vec![];
      for child in node.children.iter().take(required) {
        let value = self.walk_tree(path.as_slice(), child, value_resolver)?;
        required_args.push(value);
      }
      Ok((required_args, node.children.iter().dropping(required).cloned().collect()))
    }
  }

  fn execute_join(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &Vec<String>
  ) -> ExecutionPlanNode {
    let (children, str_values) = match self.evaluate_children(value_resolver, node, path) {
      Ok((children, values)) => {
        (children, values.iter().flat_map(|v| {
          match v {
            NodeValue::STRING(s) => vec![s.clone()],
            NodeValue::BOOL(b) => vec![b.to_string()],
            NodeValue::MMAP(_) => vec![v.str_form()],
            NodeValue::SLIST(list) => list.clone(),
            NodeValue::BARRAY(_) => vec![v.str_form()],
            NodeValue::NAMESPACED(_, _) => vec![v.str_form()],
            NodeValue::UINT(u) => vec![u.to_string()],
            NodeValue::JSON(json) => vec![json.to_string()],
            _ => vec![]
          }
        }).collect_vec())
      },
      Err(value) => return value
    };

    let result = if action == "join-with" && !str_values.is_empty() {
      let first = &str_values[0];
      str_values.iter().dropping(1).join(first.as_str())
    } else {
      str_values.iter().join("")
    };

    ExecutionPlanNode {
      node_type: node.node_type.clone(),
      result: Some(NodeResult::VALUE(NodeValue::STRING(result))),
      children
    }
  }

  fn execute_error(
    &mut self,
    _action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &Vec<String>
  ) -> ExecutionPlanNode {
    let (children, str_values) = match self.evaluate_children(value_resolver, node, path) {
      Ok((children, values)) => {
        (children, values.iter().flat_map(|v| {
          match v {
            NodeValue::STRING(s) => vec![s.clone()],
            NodeValue::BOOL(b) => vec![b.to_string()],
            NodeValue::MMAP(_) => vec![v.str_form()],
            NodeValue::SLIST(list) => list.clone(),
            NodeValue::BARRAY(_) => vec![v.str_form()],
            NodeValue::NAMESPACED(_, _) => vec![v.str_form()],
            NodeValue::UINT(u) => vec![u.to_string()],
            NodeValue::JSON(json) => vec![json.to_string()],
            _ => vec![]
          }
        }).collect_vec())
      },
      Err(value) => return value
    };

    let result = str_values.iter().join("");
    ExecutionPlanNode {
      node_type: node.node_type.clone(),
      result: Some(NodeResult::ERROR(result)),
      children
    }
  }

  fn evaluate_children(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &Vec<String>
  ) -> Result<(Vec<ExecutionPlanNode>, Vec<NodeValue>), ExecutionPlanNode> {
    let mut children = vec![];
    let mut values = vec![];
    let mut loop_items = VecDeque::from(node.children.clone());

    while !loop_items.is_empty() {
      let child = loop_items.pop_front().unwrap();
      let value = if let Some(child_value) = child.value() {
        child_value
      } else {
        match &self.walk_tree(path.as_slice(), &child, value_resolver) {
          Ok(value) => if value.is_splat() {
            children.push(value.clone());
            for splat_child in value.children.iter().rev() {
              loop_items.push_front(splat_child.clone());
            }
            NodeResult::OK
          } else {
            children.push(value.clone());
            value.value().unwrap_or_default()
          },
          Err(err) => {
            return Err(ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::ERROR(err.to_string())),
              children: children.clone()
            })
          }
        }
      };

      match value {
        NodeResult::OK => {
          // no-op
        }
        NodeResult::VALUE(value) => {
          values.push(value);
        }
        NodeResult::ERROR(err) => {
          return Err(ExecutionPlanNode {
            node_type: node.node_type.clone(),
            result: Some(NodeResult::ERROR(err.to_string())),
            children: children.clone()
          })
        }
      }
    }
    Ok((children, values))
  }

  fn execute_check_entries(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_args(2, 1, node, action, value_resolver, &action_path) {
      Ok((values, optional)) => {
        let first = values[0].value()
          .unwrap_or_default()
          .as_value()
          .unwrap_or_default()
          .as_slist()
          .unwrap_or_default();
        let expected_keys = first.iter()
          .cloned()
          .collect::<HashSet<_>>();
        let second = values[1].value()
          .unwrap_or_default()
          .as_value()
          .unwrap_or_default();
        let result = match &second {
          NodeValue::MMAP(map) => {
            let actual_keys = map.keys()
              .cloned()
              .collect::<HashSet<_>>();
            Self::check_diff(action, &expected_keys, &actual_keys)
          }
          NodeValue::SLIST(list) => {
            let actual_keys = list.iter()
              .cloned()
              .collect::<HashSet<_>>();
            Self::check_diff(action, &expected_keys, &actual_keys)
          }
          NodeValue::STRING(str) => {
            let actual_keys = hashset![str.clone()];
            Self::check_diff(action, &expected_keys, &actual_keys)
          }
          NodeValue::JSON(json) => match json {
            Value::Object(map) => {
              let actual_keys = map.keys()
                .cloned()
                .collect::<HashSet<_>>();
              Self::check_diff(action, &expected_keys, &actual_keys)
            }
            Value::Array(list) => {
              let actual_keys = list.iter()
                .map(|v| v.to_string())
                .collect::<HashSet<_>>();
              Self::check_diff(action, &expected_keys, &actual_keys)
            }
            _ => Err((format!("'{}' can't be used with a {:?} node", action, second), None))
          }
          #[cfg(feature = "xml")]
          NodeValue::XML(xml) => match xml {
            XmlValue::Element(element) => {
              let actual_keys = element.child_elements()
                .map(|child| child.name())
                .collect::<HashSet<_>>();
              Self::check_diff(action, &expected_keys, &actual_keys)
            }
            _ => Err((format!("'{}' can't be used with a {:?} node", action, second), None))
          }
          _ => Err((format!("'{}' can't be used with a {:?} node", action, second), None))
        };

        match result {
          Ok(_) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::OK),
              children: values.iter().chain(optional.iter()).cloned().collect()
            }
          }
          Err((err, diff)) => {
            debug!("expect:empty failed with an error: {}", err);
            if optional.len() > 0 {
              if let Some(diff) = diff {
                self.push_result(Some(NodeResult::VALUE(NodeValue::SLIST(diff.iter().cloned().collect()))));
                let result = if let Ok(value) = self.walk_tree(action_path.as_slice(), &optional[0], value_resolver) {
                  let message = value.value().unwrap_or_default().as_string().unwrap_or_default();
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(message)),
                    children: values.iter().chain(once(&value)).cloned().collect()
                  }
                } else {
                  // There was an error generating the optional message, so just return the
                  // original error
                  ExecutionPlanNode {
                    node_type: node.node_type.clone(),
                    result: Some(NodeResult::ERROR(err.to_string())),
                    children: values.iter().chain(optional.iter()).cloned().collect()
                  }
                };
                self.pop_result();
                result
              } else {
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::ERROR(err.to_string())),
                  children: values.iter().chain(optional.iter()).cloned().collect()
                }
              }
            } else {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(NodeResult::ERROR(err.to_string())),
                children: values.iter().chain(optional.iter()).cloned().collect()
              }
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_expect_count(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_args(2, 1, node, action, value_resolver, &action_path) {
      Ok((values, optional)) => {
        let expected_length = values[0].value()
          .unwrap_or_default()
          .as_value()
          .unwrap_or_default()
          .as_uint()
          .unwrap_or_default() as usize;
        let second = values[1].value()
          .unwrap_or_default()
          .as_value()
          .unwrap_or_default();
        let result = match &second {
          NodeValue::MMAP(map) => {
            if map.len() == expected_length {
              Ok(())
            } else {
              Err(format!("Expected {} map entries but there were {}", expected_length, map.len()))
            }
          }
          NodeValue::SLIST(list) => {
            if list.len() == expected_length {
              Ok(())
            } else {
              Err(format!("Expected {} items but there were {}", expected_length, list.len()))
            }
          }
          NodeValue::STRING(str) => {
            if str.len() == expected_length {
              Ok(())
            } else {
              Err(format!("Expected a string with a length of {} but it was {}", expected_length, str.len()))
            }
          }
          NodeValue::LIST(list) => {
            if list.len() == expected_length {
              Ok(())
            } else {
              Err(format!("Expected {} items but there were {}", expected_length, list.len()))
            }
          }
          NodeValue::JSON(json) => match json {
            Value::Object(map) => {
              if map.len() == expected_length {
                Ok(())
              } else {
                Err(format!("Expected {} object entries but there were {}", expected_length, map.len()))
              }
            }
            Value::Array(list) => {
              if list.len() == expected_length {
                Ok(())
              } else {
                Err(format!("Expected {} array items but there were {}", expected_length, list.len()))
              }
            }
            _ => Err(format!("'{}' can't be used with a {:?} node", action, second))
          }
          #[cfg(feature = "xml")]
          NodeValue::XML(xml) => match xml {
            XmlValue::Element(_) => {
              if expected_length == 1 {
                Ok(())
              } else {
                Err(format!("Expected {} elements but there were 1", expected_length))
              }
            }
            _ => Err(format!("'{}' can't be used with a {:?} node", action, second))
          }
          _ => Err(format!("'{}' can't be used with a {:?} node", action, second))
        };

        match result {
          Ok(_) => {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(NodeResult::OK),
              children: values.iter().chain(optional.iter()).cloned().collect()
            }
          }
          Err(err) => {
            debug!("expect:count failed with an error: {}", err);
            if optional.len() > 0 {
              if let Ok(value) = self.walk_tree(action_path.as_slice(), &optional[0], value_resolver) {
                let message = value.value().unwrap_or_default().as_string().unwrap_or_default();
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::ERROR(message)),
                  children: values.iter().chain(once(&value)).cloned().collect()
                }
              } else {
                // There was an error generating the optional message, so just return the
                // original error
                ExecutionPlanNode {
                  node_type: node.node_type.clone(),
                  result: Some(NodeResult::ERROR(err.to_string())),
                  children: values.iter().chain(optional.iter()).cloned().collect()
                }
              }
            } else {
              ExecutionPlanNode {
                node_type: node.node_type.clone(),
                result: Some(NodeResult::ERROR(err.to_string())),
                children: values.iter().chain(optional.iter()).cloned().collect()
              }
            }
          }
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_and(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.evaluate_children(value_resolver, node, path) {
      Ok((children, values)) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::VALUE(values.iter().fold(NodeValue::NULL, |result, value| {
            result.and(value)
          }))),
          children
        }
      }
      Err(err) => err
    }
  }

  fn execute_or(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.evaluate_children(value_resolver, node, path) {
      Ok((children, values)) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::VALUE(values.iter().fold(NodeValue::NULL, |result, value| {
            result.or(value)
          }))),
          children
        }
      }
      Err(err) => err
    }
  }

  fn check_diff(
    action: &str,
    expected_keys: &HashSet<String>,
    actual_keys: &HashSet<String>
  ) -> Result<(), (String, Option<HashSet<String>>)> {
    match action {
      "expect:entries" => {
        let diff = expected_keys - actual_keys;
        if diff.is_empty() {
          Ok(())
        } else {
          let keys = NodeValue::SLIST(diff.iter().cloned().collect_vec());
          Err((format!("The following expected entries were missing: {}", keys), Some(diff)))
        }
      }
      "expect:only-entries" => {
        let diff = actual_keys - expected_keys;
        if diff.is_empty() {
          Ok(())
        } else {
          let keys = NodeValue::SLIST(diff.iter().cloned().collect_vec());
          Err((format!("The following unexpected entries were received: {}", keys), Some(diff)))
        }
      }
      _ => Err((format!("'{}' is not a valid action", action), None))
    }
  }

  fn execute_header_parse(
    &mut self,
    action: &str,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    match self.validate_one_arg(node, action, value_resolver, &action_path) {
      Ok(value) => {
        let arg_value = value.value()
          .unwrap_or_default()
          .as_string()
          .unwrap_or_default();
        let values: Vec<&str> = strip_whitespace(arg_value.as_str(), ";");
        let (header_value, header_params) = values.as_slice()
          .split_first()
          .unwrap_or((&"", &[]));
        let parameter_map = parse_charset_parameters(header_params);

        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::VALUE(NodeValue::JSON(json!({
            "value": header_value,
            "parameters": parameter_map
          })))),
          children: vec![value]
        }
      }
      Err(err) => {
        ExecutionPlanNode {
          node_type: node.node_type.clone(),
          result: Some(NodeResult::ERROR(err.to_string())),
          children: node.children.clone()
        }
      }
    }
  }

  fn execute_for_each(
    &mut self,
    value_resolver: &dyn ValueResolver,
    node: &ExecutionPlanNode,
    action_path: &Vec<String>
  ) -> ExecutionPlanNode {
    if let Some(first_node) = node.children.first() {
      match self.walk_tree(action_path.as_slice(), first_node, value_resolver) {
        Ok(first) => {
          let first_result = first.value().unwrap_or_default();
          if first_result.is_err() {
            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(first_result),
              children: once(first.clone()).chain(node.children.iter().dropping(1).cloned()).collect()
            }
          } else {
            let mut result = NodeResult::OK;
            let mut child_results = vec![first.clone()];

            let loop_items = first_result
              .as_value()
              .unwrap_or_default()
              .to_list();
            for (index, _) in loop_items.iter().enumerate() {
              for child in node.children.iter().dropping(1) {
                let updated_child = inject_index(child, index);
                match self.walk_tree(&action_path, &updated_child, value_resolver) {
                  Ok(value) => {
                    result = result.and(&value.result);
                    child_results.push(value.clone());
                  }
                  Err(err) => {
                    let node_result = NodeResult::ERROR(err.to_string());
                    result = result.and(&Some(node_result.clone()));
                    child_results.push(updated_child.clone_with_result(node_result));
                  }
                }
              }
            }

            ExecutionPlanNode {
              node_type: node.node_type.clone(),
              result: Some(result.truthy()),
              children: child_results
            }
          }
        }
        Err(err) => {
          node.clone_with_result(NodeResult::ERROR(err.to_string()))
        }
      }
    } else {
      node.clone_with_result(NodeResult::OK)
    }
  }

  #[instrument(ret, skip_all, fields(%path), level = "trace")]
  fn resolve_stack_value(&self, path: &DocPath) -> anyhow::Result<NodeValue> {
    if let Some(result) = self.stack_value() {
      if let NodeResult::VALUE(value) = result {
        match value {
          NodeValue::NULL => {
            Err(anyhow!("Can not resolve '{}', current stack value does not contain a value (is NULL)", path))
          }
          NodeValue::JSON(json) => {
            if path.is_root() {
              Ok(NodeValue::JSON(json))
            } else {
              let json_paths = pact_models::json_utils::resolve_path(&json, path);
              trace!("resolved path {} -> {:?}", path, json_paths);
              if json_paths.is_empty() {
                Ok(NodeValue::NULL)
              } else if json_paths.len() == 1 {
                if let Some(value) = json.pointer(json_paths[0].as_str()) {
                  Ok(NodeValue::JSON(value.clone()))
                } else {
                  Ok(NodeValue::NULL)
                }
              } else {
                let values = json_paths.iter()
                  .map(|path| json.pointer(path.as_str()).cloned().unwrap_or_default())
                  .collect();
                Ok(NodeValue::JSON(Value::Array(values)))
              }
            }
          }
          #[cfg(feature = "xml")]
          NodeValue::XML(value) => {
            if path.is_root() {
              Ok(NodeValue::XML(value.clone()))
            } else if let Some(element) = value.as_element() {
              let xml_paths = pact_models::xml_utils::resolve_path(&element, path);
              trace!("resolved path {} -> {:?}", path, xml_paths);
              if xml_paths.is_empty() {
                Ok(NodeValue::NULL)
              } else if xml_paths.len() == 1 {
                if let Some(value) = resolve_matching_node(&element, xml_paths[0].as_str()) {
                  Ok(NodeValue::XML(value.into()))
                } else {
                  Ok(NodeValue::NULL)
                }
              } else {
                let values = xml_paths.iter()
                  .map(|path| {
                    resolve_matching_node(&element, path.as_str())
                      .map(|node| NodeValue::XML(node.into()))
                      .unwrap_or_default()
                  })
                  .collect();
                Ok(NodeValue::LIST(values))
              }
            } else {
              todo!("Deal with other XML types: {}", value)
            }
          }
          _ => {
            Err(anyhow!("Can not resolve '{}', current stack value does not contain a value that is resolvable", path))
          }
        }
      } else {
        Err(anyhow!("Can not resolve '{}', current stack value does not contain a value", path))
      }
    } else {
      Err(anyhow!("Can not resolve '{}', current value stack is either empty or contains an empty value", path))
    }
  }
}

fn inject_index(node: &ExecutionPlanNode, index: usize) -> ExecutionPlanNode {
  match &node.node_type {
    PlanNodeType::CONTAINER(label) => {
      if let Ok(path) = DocPath::new(label) {
        ExecutionPlanNode {
          node_type: PlanNodeType::CONTAINER(inject_index_in_path(&path, index).to_string()),
          result: node.result.clone(),
          children: node.children.iter()
            .map(|child| inject_index(child, index))
            .collect()
        }
      } else {
        node.clone_with_children(node.children.iter()
          .map(|child| inject_index(child, index)))
      }
    }
    PlanNodeType::ACTION(_) => node.clone_with_children(node.children.iter()
      .map(|child| inject_index(child, index))),
    PlanNodeType::PIPELINE => node.clone_with_children(node.children.iter()
      .map(|child| inject_index(child, index))),
    PlanNodeType::RESOLVE_CURRENT(exp) => {
      ExecutionPlanNode {
        node_type: PlanNodeType::RESOLVE_CURRENT(inject_index_in_path(exp, index)),
        result: node.result.clone(),
        children: vec![]
      }
    }
    PlanNodeType::SPLAT => node.clone_with_children(node.children.iter()
      .map(|child| inject_index(child, index))),
    _ => node.clone()
  }
}

fn inject_index_in_path(path: &DocPath, index: usize) -> DocPath {
  let mut tokens = path.tokens().clone();
  for token in &mut tokens {
    if *token == PathToken::StarIndex {
      *token = PathToken::Index(index);
      break;
    }
  }
  DocPath::from_tokens(tokens)
}

fn json_check_length(length: usize, json: &Value) -> anyhow::Result<()> {
  match json {
    Value::Array(a) => if a.len() == length {
      Ok(())
    } else {
      Err(anyhow!("Was expecting a length of {}, but actual length is {}", length, a.len()))
    }
    Value::Object(o) => if o.len() == length {
      Ok(())
    } else {
      Err(anyhow!("Was expecting a length of {}, but actual length is {}", length, o.len()))
    }
    _ => Ok(())
  }
}

fn json_check_type(expected_type: String, json_value: &Value) -> anyhow::Result<()> {
  match expected_type.as_str() {
    "NULL" => json_value.as_null()
      .ok_or_else(|| anyhow!("Was expecting a JSON NULL but got a {}", type_of(&json_value))),
    "BOOL" => json_value.as_bool()
      .ok_or_else(|| anyhow!("Was expecting a JSON Bool but got a {}", type_of(&json_value)))
      .map(|_| ()),
    "NUMBER" => json_value.as_number()
      .ok_or_else(|| anyhow!("Was expecting a JSON Number but got a {}", type_of(&json_value)))
      .map(|_| ()),
    "STRING" => json_value.as_str()
      .ok_or_else(|| anyhow!("Was expecting a JSON String but got a {}", type_of(&json_value)))
      .map(|_| ()),
    "ARRAY" => json_value.as_array()
      .ok_or_else(|| anyhow!("Was expecting a JSON Array but got a {}", type_of(&json_value)))
      .map(|_| ()),
    "OBJECT" => json_value.as_object()
      .ok_or_else(|| anyhow!("Was expecting a JSON Object but got a {}", type_of(&json_value)))
      .map(|_| ()),
    _ => Err(anyhow!("'{}' is not a valid JSON type", expected_type))
  }
}
