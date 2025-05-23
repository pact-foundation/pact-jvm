//! Types for supporting building and executing plans for bodies

use std::fmt::Debug;
use std::sync::{Arc, LazyLock, RwLock};

use bytes::Bytes;
#[cfg(feature = "xml")] use itertools::Itertools;
#[cfg(feature = "xml")] use kiss_xml::dom::Element;
use nom::AsBytes;
use serde_json::Value;
use tracing::trace;

use pact_models::content_types::ContentType;
use pact_models::matchingrules::{MatchingRule, RuleList};
use pact_models::path_exp::{DocPath, PathToken};
#[cfg(feature = "xml")] use pact_models::xml_utils::{group_children, text_nodes};

use crate::engine::{build_matching_rule_node, ExecutionPlanNode, NodeValue, PlanMatchingContext};
#[cfg(feature = "xml")] use crate::engine::xml::name;

/// Trait for implementations of builders for different types of bodies
pub trait PlanBodyBuilder: Debug {
  /// If this builder supports a namespace for nodes.
  fn namespace(&self) -> Option<String> {
    None
  }

  /// If this builder supports the given content type
  fn supports_type(&self, content_type: &ContentType) -> bool;

  /// Build the plan for the expected body
  fn build_plan(&self, content: &Bytes, context: &PlanMatchingContext) -> anyhow::Result<ExecutionPlanNode>;
}

static BODY_PLAN_BUILDERS: LazyLock<RwLock<Vec<Arc<dyn PlanBodyBuilder + Send + Sync>>>> = LazyLock::new(|| {
  let mut builders: Vec<Arc<dyn PlanBodyBuilder + Send + Sync>> = vec![];

  // TODO: Add default implementations here
  builders.push(Arc::new(JsonPlanBuilder::new()));
  #[cfg(feature = "xml")]
  builders.push(Arc::new(XMLPlanBuilder::new()));

  RwLock::new(builders)
});

pub(crate) fn get_body_plan_builder(content_type: &ContentType) -> Option<Arc<dyn PlanBodyBuilder + Send + Sync>> {
  let registered_builders = (*BODY_PLAN_BUILDERS).read().unwrap();
  registered_builders.iter().find(|builder| builder.supports_type(content_type))
    .cloned()
}

/// Plan builder for plain text. This just sets up an equality matcher
#[derive(Clone, Debug)]
pub struct PlainTextBuilder;

impl PlainTextBuilder {
  /// Create a new instance
  pub fn new() -> Self {
    PlainTextBuilder{}
  }
}

impl PlanBodyBuilder for PlainTextBuilder {
  fn supports_type(&self, content_type: &ContentType) -> bool {
    content_type.is_text()
  }

  fn build_plan(&self, content: &Bytes, _context: &PlanMatchingContext) -> anyhow::Result<ExecutionPlanNode> {
    let bytes = content.to_vec();
    let text_content = String::from_utf8_lossy(&bytes);
    let mut node = ExecutionPlanNode::action("match:equality");
    let mut child_node = ExecutionPlanNode::action("convert:UTF8");
    child_node.add(ExecutionPlanNode::resolve_value(DocPath::new_unwrap("$.body")));
    node.add(ExecutionPlanNode::value_node(text_content.to_string()));
    node.add(child_node);
    node.add(ExecutionPlanNode::value_node(NodeValue::NULL));
    Ok(node)
  }
}

/// Plan builder for JSON bodies
#[derive(Clone, Debug)]
pub struct JsonPlanBuilder;

impl JsonPlanBuilder {
  /// Create a new instance
  pub fn new() -> Self {
    JsonPlanBuilder{}
  }

  fn process_body_node(
    context: &PlanMatchingContext,
    json: &Value,
    path: &DocPath,
    root_node: &mut ExecutionPlanNode
  ) {
    trace!(%json, %path, ">>> process_body_node");
    match &json {
      Value::Array(items) => {
        if context.matcher_is_defined(path) {
          let matchers = context.select_best_matcher(&path);
          root_node.add(ExecutionPlanNode::annotation(format!("{} {}",
            path.last_field().unwrap_or_default(),
            matchers.generate_description(true))));
          root_node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(json.clone()),
            &ExecutionPlanNode::resolve_current_value(path), &matchers, true));

          if let Some(template) = items.first() {
            let mut for_each_node = ExecutionPlanNode::action("for-each");
            let item_path = path.join("[*]");
            for_each_node.add(ExecutionPlanNode::resolve_current_value(path));
            let mut item_node = ExecutionPlanNode::container(&item_path);
            match template {
              Value::Array(_) => Self::process_body_node(context, template, &item_path, &mut item_node),
              Value::Object(_) => Self::process_body_node(context, template, &item_path, &mut item_node),
              _ => {
                let mut presence_check = ExecutionPlanNode::action("if");
                presence_check
                  .add(
                    ExecutionPlanNode::action("check:exists")
                      .add(ExecutionPlanNode::resolve_current_value(&item_path))
                  );
                if context.matcher_is_defined(&item_path) {
                  let matchers = context.select_best_matcher(&item_path);
                  presence_check.add(ExecutionPlanNode::annotation(format!("[*] {}", matchers.generate_description(false))));
                  presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(template),
                                                              &ExecutionPlanNode::resolve_current_value(&item_path), &matchers, false));
                } else {
                  presence_check.add(
                    ExecutionPlanNode::action("match:equality")
                      .add(ExecutionPlanNode::value_node(NodeValue::NAMESPACED("json".to_string(), template.to_string())))
                      .add(ExecutionPlanNode::resolve_current_value(&item_path))
                      .add(ExecutionPlanNode::value_node(NodeValue::NULL))
                  );
                }
                item_node.add(presence_check);
              }
            }
            for_each_node.add(item_node);
            root_node.add(for_each_node);
          }
        } else if items.is_empty() {
          root_node.add(
            ExecutionPlanNode::action("json:expect:empty")
              .add(ExecutionPlanNode::value_node("ARRAY"))
              .add(ExecutionPlanNode::resolve_current_value(path))
          );
        } else {
          root_node.add(
            ExecutionPlanNode::action("json:match:length")
              .add(ExecutionPlanNode::value_node("ARRAY"))
              .add(ExecutionPlanNode::value_node(items.len()))
              .add(ExecutionPlanNode::resolve_current_value(path))
          );

          for (index, item) in items.iter().enumerate() {
            let item_path = path.join_index(index);
            let mut item_node = ExecutionPlanNode::container(&item_path);
            match item {
              Value::Array(_) => Self::process_body_node(context, item, &item_path, &mut item_node),
              Value::Object(_) => Self::process_body_node(context, item, &item_path, &mut item_node),
              _ => {
                let mut presence_check = ExecutionPlanNode::action("if");
                presence_check
                  .add(
                    ExecutionPlanNode::action("check:exists")
                      .add(ExecutionPlanNode::resolve_current_value(&item_path))
                  );
                if context.matcher_is_defined(&item_path) {
                  let matchers = context.select_best_matcher(&item_path);
                  presence_check.add(ExecutionPlanNode::annotation(format!("[{}] {}", index, matchers.generate_description(false))));
                  presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item),
                                                              &ExecutionPlanNode::resolve_current_value(&item_path), &matchers, false));
                } else {
                  presence_check.add(
                    ExecutionPlanNode::action("match:equality")
                      .add(ExecutionPlanNode::value_node(NodeValue::NAMESPACED("json".to_string(), item.to_string())))
                      .add(ExecutionPlanNode::resolve_current_value(&item_path))
                      .add(ExecutionPlanNode::value_node(NodeValue::NULL))
                  );
                }
                item_node.add(presence_check);
                root_node.add(item_node);
              }
            }
          }
        }
      }
      Value::Object(entries) => {
        let rules = context.select_best_matcher(path);
        if !rules.is_empty() && should_apply_to_map_entries(&rules) {
          root_node.add(ExecutionPlanNode::annotation(rules.generate_description(true)));
          root_node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(json.clone()),
            &ExecutionPlanNode::resolve_current_value(path), &rules, true));
        } else if entries.is_empty() {
          root_node.add(
            ExecutionPlanNode::action("json:expect:empty")
              .add(ExecutionPlanNode::value_node("OBJECT"))
              .add(ExecutionPlanNode::resolve_current_value(path))
          );
        } else {
          let keys = NodeValue::SLIST(entries.keys().map(|key| key.clone()).collect());
          root_node.add(
            ExecutionPlanNode::action("json:expect:entries")
              .add(ExecutionPlanNode::value_node("OBJECT"))
              .add(ExecutionPlanNode::value_node(keys.clone()))
              .add(ExecutionPlanNode::resolve_current_value(path))
          );
          if !context.config.allow_unexpected_entries {
            root_node.add(
              ExecutionPlanNode::action("expect:only-entries")
                .add(ExecutionPlanNode::value_node(keys.clone()))
                .add(ExecutionPlanNode::resolve_current_value(path))
            );
          } else {
            root_node.add(
              ExecutionPlanNode::action("json:expect:not-empty")
                .add(ExecutionPlanNode::value_node("OBJECT"))
                .add(ExecutionPlanNode::resolve_current_value(path))
            );
          }
        }

        for (key, value) in entries {
          let mut item_path = path.clone();
          item_path.push_field(key);
          let mut item_node = ExecutionPlanNode::container(&item_path);
          Self::process_body_node(context, value, &item_path, &mut item_node);
          root_node.add(item_node);
        }
      }
      _ => {
        if context.matcher_is_defined(path) {
          let matchers = context.select_best_matcher(path);
          root_node.add(ExecutionPlanNode::annotation(format!("{} {}", path.last_field().unwrap_or_default(), matchers.generate_description(false))));
          root_node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(json),
            &ExecutionPlanNode::resolve_current_value(path), &matchers, false));
        } else {
          let mut match_node = ExecutionPlanNode::action("match:equality");
          match_node
            .add(ExecutionPlanNode::value_node(NodeValue::NAMESPACED("json".to_string(), json.to_string())))
            .add(ExecutionPlanNode::resolve_current_value(path))
            .add(ExecutionPlanNode::value_node(NodeValue::NULL));
          root_node.add(match_node);
        }
      }
    }
  }
}

fn should_apply_to_map_entries(rules: &RuleList) -> bool {
  rules.rules.iter().any(|rule| {
    match rule {
      MatchingRule::Values => true,
      MatchingRule::EachKey(_) => true,
      MatchingRule::EachValue(_) => true,
      _ => false
    }
  })
}

impl PlanBodyBuilder for JsonPlanBuilder {
  fn namespace(&self) -> Option<String> {
    Some("json".to_string())
  }

  fn supports_type(&self, content_type: &ContentType) -> bool {
    content_type.is_json()
  }

  fn build_plan(&self, content: &Bytes, context: &PlanMatchingContext) -> anyhow::Result<ExecutionPlanNode> {
    let expected_json: Value = serde_json::from_slice(content.as_bytes())?;
    let mut body_node = ExecutionPlanNode::action("tee");
    body_node
      .add(ExecutionPlanNode::action("json:parse")
        .add(ExecutionPlanNode::resolve_value(DocPath::new_unwrap("$.body"))));

    let path = DocPath::root();
    let mut root_node = ExecutionPlanNode::container(&path);
    Self::process_body_node(context, &expected_json, &path, &mut root_node);
    body_node.add(root_node);

    Ok(body_node)
  }
}

/// Plan builder for XML bodies
#[derive(Clone, Debug)]
#[cfg(feature = "xml")]
pub struct XMLPlanBuilder;

#[cfg(feature = "xml")]
impl XMLPlanBuilder {
  /// Create a new instance
  pub fn new() -> Self {
    XMLPlanBuilder{}
  }

  fn process_element(
    &self,
    context: &PlanMatchingContext,
    element: &Element,
    index: Option<usize>,
    path: &DocPath,
    node: &mut ExecutionPlanNode
  ) {
    let name = name(element);
    let element_path = if path.ends_with(format!("{}[*]", name).as_str()) {
      path.clone()
    } else if let Some(index) = index {
      path.join_field(&name).join_index(index)
    } else {
      path.join_field(&name)
    };

    let mut presence_check = ExecutionPlanNode::action("if");
    presence_check
      .add(ExecutionPlanNode::action("check:exists")
        .add(ExecutionPlanNode::resolve_current_value(element_path.clone())));
    let mut item_node = ExecutionPlanNode::container(&element_path);

    if !element.attributes().is_empty() {
      let mut attributes_node = ExecutionPlanNode::container("attributes");
      self.process_attributes(&element_path, element, &mut attributes_node, context);
      item_node.add(attributes_node);
    }

    let mut text_node = ExecutionPlanNode::container("#text");
    self.process_text(&element_path, element, &mut text_node, context);
    item_node.add(text_node);

    self.process_children(context, &element_path, element, &mut item_node);
    presence_check.add(item_node);

    let mut error_node = ExecutionPlanNode::action("error");
    error_node
      .add(ExecutionPlanNode::value_node(
        format!("Was expecting an XML element {} but it was missing", element_path
          .as_json_pointer().unwrap_or_else(|_| element.name())
        )));
    presence_check.add(error_node);

    node.add(presence_check);
  }

  fn process_children(
    &self,
    context: &PlanMatchingContext,
    path: &DocPath,
    element: &Element,
    parent_node: &mut ExecutionPlanNode
  ) {
    let children = group_children(element);

    if !context.config.allow_unexpected_entries {
      if element.child_elements().next().is_none() {
        parent_node.add(
          ExecutionPlanNode::action("expect:empty")
            .add(ExecutionPlanNode::resolve_current_value(path))
        );
      } else {
        parent_node.add(
          ExecutionPlanNode::action("expect:only-entries")
            .add(ExecutionPlanNode::value_node(children.keys().collect_vec()))
            .add(ExecutionPlanNode::resolve_current_value(path))
        );
      }
    }

    for (child_name, elements) in children {
      let p = path.join(child_name.as_str());

      if !context.type_matcher_defined(&p) {
        parent_node.add(
          ExecutionPlanNode::action("expect:count")
            .add(ExecutionPlanNode::value_node(NodeValue::UINT(elements.len() as u64)))
            .add(ExecutionPlanNode::resolve_current_value(p.clone()))
            .add(
              ExecutionPlanNode::action("join")
                .add(ExecutionPlanNode::value_node(
                  format!("Expected {} <{}> child element{} but there were ",
                          elements.len(), child_name.as_str(), if elements.len() > 1 { "s" } else { "" })))
                .add(ExecutionPlanNode::action("length")
                  .add(ExecutionPlanNode::resolve_current_value(p.clone())))
            )
        );

        if elements.len() == 1 {
          self.process_element(context, elements[0], Some(0), path, parent_node);
        } else {
          for (index, child) in elements.iter().enumerate() {
            self.process_element(context, child, Some(index), path, parent_node);
          }
        }
      } else {
        let rules = context.select_best_matcher(&p)
          .filter(|m| m.is_length_type_matcher());
        if !rules.is_empty() {
          parent_node.add(ExecutionPlanNode::annotation(format!("{} {}",
            path.last_field().unwrap_or_default(),
            rules.generate_description(true))));
          parent_node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(elements[0]),
            &ExecutionPlanNode::resolve_current_value(path), &rules, true));
        }

        let mut for_each_node = ExecutionPlanNode::action("for-each");
        for_each_node.add(ExecutionPlanNode::resolve_current_value(&p));
        let item_path = p.join("[*]");

        self.process_element(context, elements[0], Some(0), &item_path, &mut for_each_node);

        parent_node.add(for_each_node);
      }
    }
  }

  fn process_text(
    &self,
    path: &DocPath,
    element: &Element,
    node: &mut ExecutionPlanNode,
    context: &PlanMatchingContext
  ) {
    let text_nodes = text_nodes(element);
    let p = path.join("#text");
    let no_indices = drop_indices(&p);
    let matchers = context.select_best_matcher(&p)
      .filter(|matcher| !matcher.is_type_matcher())
      .and_rules(&context.select_best_matcher(&no_indices)
        .filter(|matcher| !matcher.is_type_matcher())
      ).remove_duplicates();
    if !matchers.is_empty() {
      node.add(ExecutionPlanNode::annotation(format!("{} {}", p.last_field().unwrap_or_default(),
        matchers.generate_description(false))));
      let mut current_value = ExecutionPlanNode::action("to-string");
      current_value.add(ExecutionPlanNode::resolve_current_value(&p));
      node.add(build_matching_rule_node(&ExecutionPlanNode::value_node(text_nodes.join("")),
        &current_value, &matchers, false));
    } else {
      if text_nodes.is_empty() {
        node.add(ExecutionPlanNode::action("expect:empty")
          .add(ExecutionPlanNode::action("to-string")
            .add(ExecutionPlanNode::resolve_current_value(&p))));
      } else {
        let mut match_node = ExecutionPlanNode::action("match:equality");
        match_node
          .add(ExecutionPlanNode::value_node(NodeValue::STRING(text_nodes.join(""))))
          .add(ExecutionPlanNode::action("to-string")
            .add(ExecutionPlanNode::resolve_current_value(&p)))
          .add(ExecutionPlanNode::value_node(NodeValue::NULL));
        node.add(match_node);
      }
    }
  }

  fn process_attributes(
    &self,
    path: &DocPath,
    element: &Element,
    node: &mut ExecutionPlanNode,
    context: &PlanMatchingContext
  ) {
    let attributes = element.attributes();
    let keys = attributes.keys().cloned().sorted().collect_vec();
    for key in &keys {
      let p = path.join_field(format!("@{}", key));
      let value = attributes.get(key).unwrap();
      let mut item_node = ExecutionPlanNode::container(p.to_string());

      let mut presence_check = ExecutionPlanNode::action("if");
      let item_value = NodeValue::STRING(value.clone());
      presence_check
        .add(
          ExecutionPlanNode::action("check:exists")
            .add(ExecutionPlanNode::resolve_current_value(&p))
        );

      let no_indices = drop_indices(&p);
      let matchers = context.select_best_matcher(&p)
        .and_rules(&context.select_best_matcher(&no_indices))
        .remove_duplicates();
      if !matchers.is_empty() {
        item_node.add(ExecutionPlanNode::annotation(format!("@{} {}", key, matchers.generate_description(true))));
        presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
          ExecutionPlanNode::action("xml:value")
            .add(ExecutionPlanNode::resolve_current_value(&p)),
          &matchers, false));
      } else {
        item_node.add(ExecutionPlanNode::annotation(format!("@{}={}", key, item_value.to_string())));
        let mut item_check = ExecutionPlanNode::action("match:equality");
        item_check
          .add(ExecutionPlanNode::value_node(item_value.clone()))
          .add(ExecutionPlanNode::action("xml:value")
            .add(ExecutionPlanNode::resolve_current_value(&p)))
          .add(ExecutionPlanNode::value_node(NodeValue::NULL));
        presence_check.add(item_check);
      }

      item_node.add(presence_check);
      node.add(item_node);
    }

    node.add(
      ExecutionPlanNode::action("expect:entries")
        .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone())))
        .add(ExecutionPlanNode::action("xml:attributes")
          .add(ExecutionPlanNode::resolve_current_value(path.clone())))
        .add(
          ExecutionPlanNode::action("join")
            .add(ExecutionPlanNode::value_node("The following expected attributes were missing: "))
            .add(ExecutionPlanNode::action("join-with")
              .add(ExecutionPlanNode::value_node(", "))
              .add(
                ExecutionPlanNode::splat()
                  .add(ExecutionPlanNode::action("apply"))
              )
            )
        )
    );

    if !context.config.allow_unexpected_entries {
      if !context.config.allow_unexpected_entries {
        node.add(
          ExecutionPlanNode::action("expect:only-entries")
            .add(ExecutionPlanNode::value_node(keys.clone()))
            .add(ExecutionPlanNode::action("xml:attributes")
              .add(ExecutionPlanNode::resolve_current_value(path)))
        );
      }
    }
  }
}

fn drop_indices(path: &DocPath) -> DocPath {
  DocPath::from_tokens(path.tokens()
    .iter()
    .filter(|token| match token {
      PathToken::Index(_) | PathToken::StarIndex => false,
      _ => true
    })
    .cloned())
}

#[cfg(feature = "xml")]
impl PlanBodyBuilder for XMLPlanBuilder {
  fn namespace(&self) -> Option<String> {
    Some("xml".to_string())
  }
  fn supports_type(&self, content_type: &ContentType) -> bool {
    content_type.is_xml()
  }

  fn build_plan(&self, content: &Bytes, context: &PlanMatchingContext) -> anyhow::Result<ExecutionPlanNode> {
    let dom = kiss_xml::parse_str(String::from_utf8_lossy(content.as_bytes()))?;
    let root_element = dom.root_element();

    let mut body_node = ExecutionPlanNode::action("tee");
    body_node
      .add(ExecutionPlanNode::action("xml:parse")
        .add(ExecutionPlanNode::resolve_value(DocPath::new_unwrap("$.body"))));

    let path = DocPath::root();
    let mut root_node = ExecutionPlanNode::container(&path);
    self.process_element(context, root_element, None, &path, &mut root_node);

    body_node.add(root_node);

    Ok(body_node)
  }
}

#[cfg(test)]
mod tests {
  use bytes::Bytes;
  use pretty_assertions::assert_eq;
  use serde_json::{json, Value};

  use pact_models::matchingrules;
  use pact_models::matchingrules::MatchingRule;

  use crate::engine::bodies::{JsonPlanBuilder, PlanBodyBuilder, XMLPlanBuilder};
  use crate::engine::context::{MatchingConfiguration, PlanMatchingContext};

  #[test]
  fn json_plan_builder_with_null() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(Value::Null.to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %match:equality (
      json:null,
      ~>$,
      NULL
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_boolean() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(Value::Bool(true).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %match:equality (
      json:true,
      ~>$,
      NULL
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_string() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(Value::String("I am a string!".to_string()).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %match:equality (
      json:"I am a string!",
      ~>$,
      NULL
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_int() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!(1000).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %match:equality (
      json:1000,
      ~>$,
      NULL
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_float() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!(1000.3).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %match:equality (
      json:1000.3,
      ~>$,
      NULL
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_empty_array() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!([]).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:expect:empty (
      'ARRAY',
      ~>$
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_array() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!([100, 200, 300]).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:match:length (
      'ARRAY',
      UINT(3),
      ~>$
    ),
    :$[0] (
      %if (
        %check:exists (
          ~>$[0]
        ),
        %match:equality (
          json:100,
          ~>$[0],
          NULL
        )
      )
    ),
    :$[1] (
      %if (
        %check:exists (
          ~>$[1]
        ),
        %match:equality (
          json:200,
          ~>$[1],
          NULL
        )
      )
    ),
    :$[2] (
      %if (
        %check:exists (
          ~>$[2]
        ),
        %match:equality (
          json:300,
          ~>$[2],
          NULL
        )
      )
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_empty_object() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!({}).to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:expect:empty (
      'OBJECT',
      ~>$
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_object() {
    let builder = JsonPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let content = Bytes::copy_from_slice(json!({"a": 100, "b": 200, "c": 300})
      .to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:expect:entries (
      'OBJECT',
      ['a', 'b', 'c'],
      ~>$
    ),
    %expect:only-entries (
      ['a', 'b', 'c'],
      ~>$
    ),
    :$.a (
      %match:equality (
        json:100,
        ~>$.a,
        NULL
      )
    ),
    :$.b (
      %match:equality (
        json:200,
        ~>$.b,
        NULL
      )
    ),
    :$.c (
      %match:equality (
        json:300,
        ~>$.c,
        NULL
      )
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_object_with_matching_rule() {
    let builder = JsonPlanBuilder::new();
    let matching_rules = matchingrules! {
      "body" => { "$.a" => [ MatchingRule::Regex("^[0-9]+$".to_string()) ] }
    };
    let context = PlanMatchingContext {
      matching_rules: matching_rules.rules_for_category("body").unwrap_or_default(),
      .. PlanMatchingContext::default()
    };
    let content = Bytes::copy_from_slice(json!({"a": 100, "b": 200, "c": 300})
      .to_string().as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:expect:entries (
      'OBJECT',
      ['a', 'b', 'c'],
      ~>$
    ),
    %expect:only-entries (
      ['a', 'b', 'c'],
      ~>$
    ),
    :$.a (
      #{'a must match the regular expression /^[0-9]+$/'},
      %match:regex (
        json:100,
        ~>$.a,
        json:{"regex":"^[0-9]+$"}
      )
    ),
    :$.b (
      %match:equality (
        json:200,
        ~>$.b,
        NULL
      )
    ),
    :$.c (
      %match:equality (
        json:300,
        ~>$.c,
        NULL
      )
    )
  )
)"#, buffer);
  }

  #[test]
  fn json_plan_builder_with_array_and_type_matcher() {
    let builder = JsonPlanBuilder::new();
    let matching_rules = matchingrules! {
      "body" => { "$.item" => [ MatchingRule::MinType(2) ] }
    };
    let context = PlanMatchingContext {
      matching_rules: matching_rules.rules_for_category("body").unwrap_or_default(),
      .. PlanMatchingContext::default()
    };
    let content = Bytes::copy_from_slice(
      json!({
        "item": [
          { "a": 100 },
          { "a": 200 },
          { "a": 300 }
        ]
      }).to_string().as_bytes()
    );
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %json:parse (
    $.body
  ),
  :$ (
    %json:expect:entries (
      'OBJECT',
      ['item'],
      ~>$
    ),
    %expect:only-entries (
      ['item'],
      ~>$
    ),
    :$.item (
      #{'item must match by type and have at least 2 items'},
      %match:min-type (
        json:[{"a":100},{"a":200},{"a":300}],
        ~>$.item,
        json:{"min":2}
      ),
      %for-each (
        ~>$.item,
        :$.item[*] (
          %json:expect:entries (
            'OBJECT',
            ['a'],
            ~>$.item[*]
          ),
          %expect:only-entries (
            ['a'],
            ~>$.item[*]
          ),
          :$.item[*].a (
            #{'a must match by type'},
            %match:type (
              json:100,
              ~>$.item[*].a,
              json:{}
            )
          )
        )
      )
    )
  )
)"#, buffer);
  }

  #[test]
  #[cfg(feature = "xml")]
  fn xml_plan_builder_with_very_simple_xml() {
    let builder = XMLPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let xml = r#"<?xml version="1.0" encoding="UTF-8"?> <blah/>"#;
    let content = Bytes::copy_from_slice(xml.as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %xml:parse (
    $.body
  ),
  :$ (
    %if (
      %check:exists (
        ~>$.blah
      ),
      :$.blah (
        :#text (
          %expect:empty (
            %to-string (
              ~>$.blah['#text']
            )
          )
        ),
        %expect:empty (
          ~>$.blah
        )
      ),
      %error (
        'Was expecting an XML element /blah but it was missing'
      )
    )
  )
)"#, buffer);
  }

  #[test]
  #[cfg(feature = "xml")]
  fn xml_plan_builder_with_allowed_unexpected_values() {
    let builder = XMLPlanBuilder::new();
    let context = PlanMatchingContext {
      config: MatchingConfiguration {
        allow_unexpected_entries: true,
        .. MatchingConfiguration::default()
      },
      .. PlanMatchingContext::default()
    };
    let xml = r#"<?xml version="1.0" encoding="UTF-8"?> <blah/>"#;
    let content = Bytes::copy_from_slice(xml.as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %xml:parse (
    $.body
  ),
  :$ (
    %if (
      %check:exists (
        ~>$.blah
      ),
      :$.blah (
        :#text (
          %expect:empty (
            %to-string (
              ~>$.blah['#text']
            )
          )
        )
      ),
      %error (
        'Was expecting an XML element /blah but it was missing'
      )
    )
  )
)"#, buffer);
  }

  #[test]
  #[cfg(feature = "xml")]
  fn xml_plan_builder_with_simple_xml() {
    let builder = XMLPlanBuilder::new();
    let context = PlanMatchingContext::default();
    let xml = r#"<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name>My Settings</name>
        <sound>
          <property name="volume" value="11" />
          <property name="mixer" value="standard" />
        </sound>
      </config>
    "#;
    let content = Bytes::copy_from_slice(xml.as_bytes());
    let node = builder.build_plan(&content, &context).unwrap();
    let mut buffer = String::new();
    node.pretty_form(&mut buffer, 0);
    assert_eq!(r#"%tee (
  %xml:parse (
    $.body
  ),
  :$ (
    %if (
      %check:exists (
        ~>$.config
      ),
      :$.config (
        :#text (
          %expect:empty (
            %to-string (
              ~>$.config['#text']
            )
          )
        ),
        %expect:only-entries (
          ['name', 'sound'],
          ~>$.config
        ),
        %expect:count (
          UINT(1),
          ~>$.config.name,
          %join (
            'Expected 1 <name> child element but there were ',
            %length (
              ~>$.config.name
            )
          )
        ),
        %if (
          %check:exists (
            ~>$.config.name[0]
          ),
          :$.config.name[0] (
            :#text (
              %match:equality (
                'My Settings',
                %to-string (
                  ~>$.config.name[0]['#text']
                ),
                NULL
              )
            ),
            %expect:empty (
              ~>$.config.name[0]
            )
          ),
          %error (
            'Was expecting an XML element /config/name/0 but it was missing'
          )
        ),
        %expect:count (
          UINT(1),
          ~>$.config.sound,
          %join (
            'Expected 1 <sound> child element but there were ',
            %length (
              ~>$.config.sound
            )
          )
        ),
        %if (
          %check:exists (
            ~>$.config.sound[0]
          ),
          :$.config.sound[0] (
            :#text (
              %expect:empty (
                %to-string (
                  ~>$.config.sound[0]['#text']
                )
              )
            ),
            %expect:only-entries (
              ['property'],
              ~>$.config.sound[0]
            ),
            %expect:count (
              UINT(2),
              ~>$.config.sound[0].property,
              %join (
                'Expected 2 <property> child elements but there were ',
                %length (
                  ~>$.config.sound[0].property
                )
              )
            ),
            %if (
              %check:exists (
                ~>$.config.sound[0].property[0]
              ),
              :$.config.sound[0].property[0] (
                :attributes (
                  :$.config.sound[0].property[0]['@name'] (
                    #{"@name='volume'"},
                    %if (
                      %check:exists (
                        ~>$.config.sound[0].property[0]['@name']
                      ),
                      %match:equality (
                        'volume',
                        %xml:value (
                          ~>$.config.sound[0].property[0]['@name']
                        ),
                        NULL
                      )
                    )
                  ),
                  :$.config.sound[0].property[0]['@value'] (
                    #{"@value='11'"},
                    %if (
                      %check:exists (
                        ~>$.config.sound[0].property[0]['@value']
                      ),
                      %match:equality (
                        '11',
                        %xml:value (
                          ~>$.config.sound[0].property[0]['@value']
                        ),
                        NULL
                      )
                    )
                  ),
                  %expect:entries (
                    ['name', 'value'],
                    %xml:attributes (
                      ~>$.config.sound[0].property[0]
                    ),
                    %join (
                      'The following expected attributes were missing: ',
                      %join-with (
                        ', ',
                        ** (
                          %apply ()
                        )
                      )
                    )
                  ),
                  %expect:only-entries (
                    ['name', 'value'],
                    %xml:attributes (
                      ~>$.config.sound[0].property[0]
                    )
                  )
                ),
                :#text (
                  %expect:empty (
                    %to-string (
                      ~>$.config.sound[0].property[0]['#text']
                    )
                  )
                ),
                %expect:empty (
                  ~>$.config.sound[0].property[0]
                )
              ),
              %error (
                'Was expecting an XML element /config/sound/0/property/0 but it was missing'
              )
            ),
            %if (
              %check:exists (
                ~>$.config.sound[0].property[1]
              ),
              :$.config.sound[0].property[1] (
                :attributes (
                  :$.config.sound[0].property[1]['@name'] (
                    #{"@name='mixer'"},
                    %if (
                      %check:exists (
                        ~>$.config.sound[0].property[1]['@name']
                      ),
                      %match:equality (
                        'mixer',
                        %xml:value (
                          ~>$.config.sound[0].property[1]['@name']
                        ),
                        NULL
                      )
                    )
                  ),
                  :$.config.sound[0].property[1]['@value'] (
                    #{"@value='standard'"},
                    %if (
                      %check:exists (
                        ~>$.config.sound[0].property[1]['@value']
                      ),
                      %match:equality (
                        'standard',
                        %xml:value (
                          ~>$.config.sound[0].property[1]['@value']
                        ),
                        NULL
                      )
                    )
                  ),
                  %expect:entries (
                    ['name', 'value'],
                    %xml:attributes (
                      ~>$.config.sound[0].property[1]
                    ),
                    %join (
                      'The following expected attributes were missing: ',
                      %join-with (
                        ', ',
                        ** (
                          %apply ()
                        )
                      )
                    )
                  ),
                  %expect:only-entries (
                    ['name', 'value'],
                    %xml:attributes (
                      ~>$.config.sound[0].property[1]
                    )
                  )
                ),
                :#text (
                  %expect:empty (
                    %to-string (
                      ~>$.config.sound[0].property[1]['#text']
                    )
                  )
                ),
                %expect:empty (
                  ~>$.config.sound[0].property[1]
                )
              ),
              %error (
                'Was expecting an XML element /config/sound/0/property/1 but it was missing'
              )
            )
          ),
          %error (
            'Was expecting an XML element /config/sound/0 but it was missing'
          )
        )
      ),
      %error (
        'Was expecting an XML element /config but it was missing'
      )
    )
  )
)"#, buffer);
  }
}
