
/// Enum for the value stored in a leaf node
#[derive(Clone, Debug, Default, PartialEq)]
pub enum NodeValue {
  /// Default is no value
  #[default]
  NULL,
  /// A string value
  STRING(String),
  /// Boolean value
  BOOL(bool),
  /// Multi-string map (String key to one or more string values)
  MMAP(HashMap<String, Vec<String>>),
  /// List of String values
  SLIST(Vec<String>),
  /// Byte Array
  BARRAY(Vec<u8>),
  /// Namespaced value
  NAMESPACED(String, String),
  /// Unsigned integer
  UINT(u64),
  /// JSON
  JSON(Value),
  /// Key/Value Pair
  ENTRY(String, Box<NodeValue>),
  /// List of values
  LIST(Vec<NodeValue>),
  /// XML
  #[cfg(feature = "xml")]
  XML(XmlValue)
}

impl NodeValue {
  /// Returns the encoded string form of the node value
  pub fn str_form(&self) -> String {
    match self {
      NodeValue::NULL => "NULL".to_string(),
      NodeValue::STRING(str) => {
        Self::escape_string(str)
      }
      NodeValue::BOOL(b) => {
        format!("BOOL({})", b)
      }
      NodeValue::MMAP(map) => {
        let mut buffer = String::new();
        buffer.push('{');

        let mut first = true;
        for (key, values) in map.iter().sorted_by(|a, b| Ord::cmp(&a.0, &b.0)) {
          if first {
            first = false;
          } else {
            buffer.push_str(", ");
          }
          buffer.push_str(Self::escape_string(key).as_str());
          if values.is_empty() {
            buffer.push_str(": []");
          } else if values.len() == 1 {
            buffer.push_str(": ");
            buffer.push_str(Self::escape_string(&values[0]).as_str());
          } else {
            buffer.push_str(": [");
            buffer.push_str(values.iter().map(|v| Self::escape_string(v)).join(", ").as_str());
            buffer.push(']');
          }
        }

        buffer.push('}');
        buffer
      }
      NodeValue::SLIST(list) => {
        let mut buffer = String::new();
        buffer.push('[');
        buffer.push_str(list.iter().map(|v| Self::escape_string(v)).join(", ").as_str());
        buffer.push(']');
        buffer
      }
      NodeValue::BARRAY(bytes) => {
        let mut buffer = String::new();
        buffer.push_str("BYTES(");
        buffer.push_str(bytes.len().to_string().as_str());
        buffer.push_str(", ");
        buffer.push_str(BASE64.encode(bytes).as_str());
        buffer.push(')');
        buffer
      }
      NodeValue::NAMESPACED(name, value) => {
        let mut buffer = String::new();
        buffer.push_str(name);
        buffer.push(':');
        buffer.push_str(value);
        buffer
      }
      NodeValue::UINT(i) => format!("UINT({})", i),
      NodeValue::JSON(json) => format!("json:{}", json),
      NodeValue::ENTRY(key, value) => {
        let mut buffer = String::new();
        buffer.push_str(Self::escape_string(key).as_str());
        buffer.push_str(" -> ");
        buffer.push_str(value.str_form().as_str());
        buffer
      }
      NodeValue::LIST(list) => {
        let mut buffer = String::new();
        buffer.push('[');
        buffer.push_str(list.iter().map(|v| v.str_form()).join(", ").as_str());
        buffer.push(']');
        buffer
      }
      #[cfg(feature = "xml")]
      NodeValue::XML(node) => match node {
        XmlValue::Element(element) => format!("xml:{}", escape(element.to_string().as_str())),
        XmlValue::Text(text) => format!("xml:text:{}", escape(text.as_str())),
        XmlValue::Attribute(name, value) => format!("xml:attribute:{}={}",
          escape(name.as_str()), escape(value.as_str()))
      }
    }
  }

  fn escape_string(str: &String) -> String {
    let escaped_str = escape(str);
    if let Cow::Borrowed(_) = &escaped_str {
      format!("'{}'", escaped_str)
    } else {
      escaped_str.to_string()
    }
  }

  /// Returns the type of the value
  pub fn value_type(&self) -> &str {
    match self {
      NodeValue::NULL => "NULL",
      NodeValue::STRING(_) => "String",
      NodeValue::BOOL(_) => "Boolean",
      NodeValue::MMAP(_) => "Multi-Value String Map",
      NodeValue::SLIST(_) => "String List",
      NodeValue::BARRAY(_) => "Byte Array",
      NodeValue::NAMESPACED(_, _) => "Namespaced Value",
      NodeValue::UINT(_) => "Unsigned Integer",
      NodeValue::JSON(_) => "JSON",
      NodeValue::ENTRY(_, _) => "Entry",
      NodeValue::LIST(_) => "List",
      #[cfg(feature = "xml")]
      NodeValue::XML(_) => "XML"
    }
  }

  /// If this value is a JSON value, returns it, otherwise returns None
  pub fn as_json(&self) -> Option<Value> {
    match self {
      NodeValue::JSON(json) => Some(json.clone()),
      _ => None
    }
  }

  /// If this value is an XML value, returns it, otherwise returns None
  #[cfg(feature = "xml")]
  pub fn as_xml(&self) -> Option<XmlValue> {
    match self {
      NodeValue::XML(xml) => Some(xml.clone()),
      _ => None
    }
  }

  /// If this value is a String value, returns it, otherwise returns None
  pub fn as_string(&self) -> Option<String> {
    match self {
      NodeValue::STRING(s) => Some(s.clone()),
      _ => None
    }
  }

  /// If this value is a bool value, returns it, otherwise returns None
  pub fn as_bool(&self) -> Option<bool> {
    match self {
      NodeValue::BOOL(b) => Some(*b),
      _ => None
    }
  }

  /// If this value is an UInt value, returns it, otherwise returns None
  pub fn as_uint(&self) -> Option<u64> {
    match self {
      NodeValue::UINT(u) => Some(*u),
      _ => None
    }
  }

  /// If this value is a string value, returns it, otherwise returns None
  pub fn as_slist(&self) -> Option<Vec<String>> {
    match self {
      NodeValue::SLIST(list) => Some(list.clone()),
      _ => None
    }
  }

  /// Calculates an AND of two values
  pub fn and(&self, other: &Self) -> Self {
    match self {
      NodeValue::NULL => other.clone(),
      NodeValue::BOOL(b) => NodeValue::BOOL(*b && other.truthy()),
      _ => NodeValue::BOOL(self.truthy() && other.truthy())
    }
  }

  /// Calculates an OR of two values
  pub fn or(&self, other: &Self) -> Self {
    match self {
      NodeValue::NULL => other.clone(),
      NodeValue::BOOL(b) => NodeValue::BOOL(*b || other.truthy()),
      _ => NodeValue::BOOL(self.truthy() || other.truthy())
    }
  }

  /// Convert this value into a boolean using a "truthy" test
  pub fn truthy(&self) -> bool {
    match self {
      NodeValue::STRING(s) => !s.is_empty(),
      NodeValue::BOOL(b) => *b,
      NodeValue::MMAP(m) => !m.is_empty(),
      NodeValue::SLIST(s) => !s.is_empty(),
      NodeValue::BARRAY(b) => !b.is_empty(),
      NodeValue::UINT(u) => *u != 0,
      NodeValue::LIST(l) => !l.is_empty(),
      _ => false
    }
  }

  /// Converts this value into a list of values
  pub fn to_list(&self) -> Vec<NodeValue> {
    match self {
      NodeValue::MMAP(entries) => {
        entries.iter()
          .map(|(k, v)| NodeValue::ENTRY(k.clone(), Box::new(NodeValue::SLIST(v.clone()))))
          .collect()
      }
      NodeValue::SLIST(list) => {
        list.iter()
          .map(|v| NodeValue::STRING(v.clone()))
          .collect()
      }
      NodeValue::JSON(json) => match json {
        Value::Array(a) => {
          a.iter()
            .map(|v| NodeValue::JSON(v.clone()))
            .collect()
        }
        _ => vec![ self.clone() ]
      }
      NodeValue::LIST(list) => list.clone(),
      _ => vec![ self.clone() ]
    }
  }
}

impl From<String> for NodeValue {
  fn from(value: String) -> Self {
    NodeValue::STRING(value.clone())
  }
}

impl From<&str> for NodeValue {
  fn from(value: &str) -> Self {
    NodeValue::STRING(value.to_string())
  }
}

impl From<usize> for NodeValue {
  fn from(value: usize) -> Self {
    NodeValue::UINT(value as u64)
  }
}

impl From<u64> for NodeValue {
  fn from(value: u64) -> Self {
    NodeValue::UINT(value)
  }
}

impl From<Value> for NodeValue {
  fn from(value: Value) -> Self {
    NodeValue::JSON(value.clone())
  }
}

impl From<&Value> for NodeValue {
  fn from(value: &Value) -> Self {
    NodeValue::JSON(value.clone())
  }
}

impl From<HashMap<&str, Value>> for NodeValue {
  fn from(value: HashMap<&str, Value>) -> Self {
    let json = Object(value.iter().map(|(k, v)| (k.to_string(), v.clone())).collect());
    NodeValue::JSON(json)
  }
}

impl From<Vec<String>> for NodeValue {
  fn from(value: Vec<String>) -> Self {
    NodeValue::SLIST(value)
  }
}

impl From<Vec<&String>> for NodeValue {
  fn from(value: Vec<&String>) -> Self {
    NodeValue::SLIST(value.iter().map(|v| (*v).clone()).collect())
  }
}

#[cfg(feature = "xml")]
impl From<Element> for NodeValue {
  fn from(value: Element) -> Self {
    NodeValue::XML(XmlValue::Element(value.clone()))
  }
}

#[cfg(feature = "xml")]
impl From<&Element> for NodeValue {
  fn from(value: &Element) -> Self {
    NodeValue::XML(XmlValue::Element(value.clone()))
  }
}

impl Matches<NodeValue> for NodeValue {
  fn matches_with(&self, actual: NodeValue, matcher: &MatchingRule, cascaded: bool) -> anyhow::Result<()> {
    match self {
      NodeValue::NULL => Value::Null.matches_with(actual.as_json().unwrap_or_default(), matcher, cascaded),
      NodeValue::STRING(s) => if let Some(actual_str) = actual.as_string() {
        s.matches_with(actual_str, matcher, cascaded)
      } else if let Some(list) = actual.as_slist() {
        let result = list.iter()
          .map(|item| s.matches_with(item, matcher, cascaded))
          .filter_map(|r| match r {
            Ok(_) => None,
            Err(err) => Some(err.to_string())
          })
          .collect_vec();
        if result.is_empty() {
          Ok(())
        } else {
          Err(anyhow!(result.join(", ")))
        }
      } else {
        s.matches_with(actual.to_string(), matcher, cascaded)
      },
      NodeValue::BOOL(b) => b.matches_with(actual.as_bool().unwrap_or_default(), matcher, cascaded),
      NodeValue::UINT(u) => u.matches_with(actual.as_uint().unwrap_or_default(), matcher, cascaded),
      NodeValue::JSON(json) => json.matches_with(actual.as_json().unwrap_or_default(), matcher, cascaded),
      NodeValue::SLIST(list) => if let Some(actual_list) = actual.as_slist() {
        list.matches_with(&actual_list, matcher, cascaded)
      } else {
        let actual_str = if let Some(actual_str) = actual.as_string() {
          actual_str
        } else {
          actual.to_string()
        };
        list.matches_with(&vec![actual_str], matcher, cascaded)
      }
      #[cfg(feature = "xml")]
      NodeValue::XML(xml_value) => if let Some(actual) = actual.as_xml() {
        xml_value.matches_with(actual, matcher, cascaded)
      } else {
        Err(anyhow!("Was expecting an XML value but got {}", actual))
      },
      _ => Err(anyhow!("Matching rules can not be applied to {} values", self.str_form()))
    }
  }
}

impl Display for NodeValue {
  fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
    write!(f, "{}", self.str_form())
  }
}


/// Terminator for tree transversal
#[derive(Copy, Clone, Debug, PartialEq, PartialOrd)]
pub enum Terminator {
  /// No termination
  ALL,
  /// Terminate at containers
  CONTAINERS
}



fn setup_method_plan(
  expected: &HttpRequest,
  _context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlanNode> {
  let mut method_container = ExecutionPlanNode::container("method");

  let mut match_method = ExecutionPlanNode::action("match:equality");
  let expected_method = expected.method.as_str().to_uppercase();
  match_method
    .add(ExecutionPlanNode::value_node(expected_method.clone()))
    .add(ExecutionPlanNode::action("upper-case")
      .add(ExecutionPlanNode::resolve_value(DocPath::new("$.method")?)))
    .add(ExecutionPlanNode::value_node(NodeValue::NULL));

  method_container.add(ExecutionPlanNode::annotation(format!("method == {}", expected_method)));
  method_container.add(match_method);

  Ok(method_container)
}

fn setup_path_plan(
  expected: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlanNode> {
  let mut plan_node = ExecutionPlanNode::container("path");
  let expected_node = ExecutionPlanNode::value_node(expected.path.as_str());
  let doc_path = DocPath::new("$.path")?;
  let actual_node = ExecutionPlanNode::resolve_value(doc_path.clone());
  if context.matcher_is_defined(&doc_path) {
    let matchers = context.select_best_matcher(&doc_path);
    plan_node.add(ExecutionPlanNode::annotation(format!("path {}", matchers.generate_description(false))));
    plan_node.add(build_matching_rule_node(&expected_node, &actual_node, &matchers, false));
  } else {
    plan_node.add(ExecutionPlanNode::annotation(format!("path == '{}'", expected.path)));
    plan_node
      .add(
        ExecutionPlanNode::action("match:equality")
          .add(expected_node)
          .add(ExecutionPlanNode::resolve_value(doc_path))
          .add(ExecutionPlanNode::value_node(NodeValue::NULL))
      );
  }
  Ok(plan_node)
}

fn build_matching_rule_node(
  expected_node: &ExecutionPlanNode,
  actual_node: &ExecutionPlanNode,
  matchers: &RuleList,
  for_collection: bool
) -> ExecutionPlanNode {
  if matchers.rules.len() == 1 {
    let matcher = if for_collection {
      matchers.rules[0].clone()
    } else {
      matchers.rules[0].for_single_item()
    };
    let mut plan_node = ExecutionPlanNode::action(format!("match:{}", matcher.name()).as_str());
    plan_node
      .add(expected_node.clone())
      .add(actual_node.clone())
      .add(ExecutionPlanNode::value_node(matcher.values()));
    plan_node
  } else {
    let mut logic_node = match matchers.rule_logic {
      RuleLogic::And => ExecutionPlanNode::action("and"),
      RuleLogic::Or => ExecutionPlanNode::action("or")
    };
    for rule in &matchers.rules {
      let matcher = if for_collection {
        rule.clone()
      } else {
        rule.for_single_item()
      };
      logic_node
        .add(
          ExecutionPlanNode::action(format!("match:{}", matcher.name()).as_str())
            .add(expected_node.clone())
            .add(actual_node.clone())
            .add(ExecutionPlanNode::value_node(matcher.values()))
        );
    }
    logic_node
  }
}

fn setup_query_plan(
  expected: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlanNode> {
  let mut plan_node = ExecutionPlanNode::container("query parameters");
  let doc_path = DocPath::new("$.query")?;

  if let Some(query) = &expected.query {
    if query.is_empty() {
      plan_node
        .add(
          ExecutionPlanNode::action("expect:empty")
            .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
            .add(
              ExecutionPlanNode::action("join")
                .add(ExecutionPlanNode::value_node("Expected no query parameters but got "))
                .add(ExecutionPlanNode::resolve_value(doc_path))
            )
        );
    } else {
      let keys = query.keys().cloned().sorted().collect_vec();
      for key in &keys {
        let value = query.get(key).unwrap();
        let mut item_node = ExecutionPlanNode::container(key);

        let mut presence_check = ExecutionPlanNode::action("if");
        let item_value = if value.len() == 1 {
          NodeValue::STRING(value[0].clone().unwrap_or_default())
        } else {
          NodeValue::SLIST(value.iter()
            .map(|v| v.clone().unwrap_or_default()).collect())
        };
        presence_check
          .add(
            ExecutionPlanNode::action("check:exists")
              .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
          );

        let item_path = DocPath::root().join(key);
        let path = doc_path.join(key);
        if context.matcher_is_defined(&item_path) {
          let matchers = context.select_best_matcher(&item_path);
          item_node.add(ExecutionPlanNode::annotation(format!("{} {}", key, matchers.generate_description(true))));
          presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
                                                      &ExecutionPlanNode::resolve_value(&path), &matchers, true));
        } else {
          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
          let mut item_check = ExecutionPlanNode::action("match:equality");
          item_check
            .add(ExecutionPlanNode::value_node(item_value))
            .add(ExecutionPlanNode::resolve_value(&path))
            .add(ExecutionPlanNode::value_node(NodeValue::NULL));
          presence_check.add(item_check);
        }

        item_node.add(presence_check);
        plan_node.add(item_node);
      }

      plan_node.add(
        ExecutionPlanNode::action("expect:entries")
          .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone())))
          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
          .add(
            ExecutionPlanNode::action("join")
              .add(ExecutionPlanNode::value_node("The following expected query parameters were missing: "))
              .add(ExecutionPlanNode::action("join-with")
                .add(ExecutionPlanNode::value_node(", "))
                .add(
                  ExecutionPlanNode::splat()
                    .add(ExecutionPlanNode::action("apply"))
                )
              )
          )
      );

      plan_node.add(
        ExecutionPlanNode::action("expect:only-entries")
          .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone())))
          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
          .add(
            ExecutionPlanNode::action("join")
              .add(ExecutionPlanNode::value_node("The following query parameters were not expected: "))
              .add(ExecutionPlanNode::action("join-with")
                .add(ExecutionPlanNode::value_node(", "))
                .add(
                  ExecutionPlanNode::splat()
                    .add(ExecutionPlanNode::action("apply"))
                )
              )
          )
      );
    }
  } else {
    plan_node
      .add(
        ExecutionPlanNode::action("expect:empty")
          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
          .add(
            ExecutionPlanNode::action("join")
              .add(ExecutionPlanNode::value_node("Expected no query parameters but got "))
              .add(ExecutionPlanNode::resolve_value(doc_path))
          )
      );
  }

  Ok(plan_node)
}

fn setup_header_plan(
  expected: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlanNode> {
  let mut plan_node = ExecutionPlanNode::container("headers");
  let doc_path = DocPath::new("$.headers")?;

  if let Some(headers) = &expected.headers {
    if !headers.is_empty() {
      let keys = headers.keys().cloned().sorted().collect_vec();
      for key in &keys {
        let value = headers.get(key).unwrap();
        let mut item_node = ExecutionPlanNode::container(key);

        let mut presence_check = ExecutionPlanNode::action("if");
        let item_value = if value.len() == 1 {
          NodeValue::STRING(value[0].clone())
        } else {
          NodeValue::SLIST(value.clone())
        };
        presence_check
          .add(
            ExecutionPlanNode::action("check:exists")
              .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
          );

        let item_path = DocPath::root().join(key);
        let path = doc_path.join(key);
        if context.matcher_is_defined(&item_path) {
          let matchers = context.select_best_matcher(&item_path);
          item_node.add(ExecutionPlanNode::annotation(format!("{} {}", key, matchers.generate_description(true))));
          presence_check.add(build_matching_rule_node(&ExecutionPlanNode::value_node(item_value),
                                                      &ExecutionPlanNode::resolve_value(&path), &matchers, true));
        } else if PARAMETERISED_HEADERS.contains(&key.to_lowercase().as_str()) {
          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
          if value.len() == 1 {
            let apply_node = build_parameterised_header_plan(&path, value[0].as_str());
            presence_check.add(apply_node);
          } else {
            for (index, item_value) in value.iter().enumerate() {
              let item_path = doc_path.join(key).join_index(index);
              let mut item_node = ExecutionPlanNode::container(index.to_string());
              let apply_node = build_parameterised_header_plan(
                &item_path, item_value.as_str());
              item_node.add(apply_node);
              presence_check.add(item_node);
            }
          }
        } else {
          item_node.add(ExecutionPlanNode::annotation(format!("{}={}", key, item_value.to_string())));
          let mut item_check = ExecutionPlanNode::action("match:equality");
          item_check
            .add(ExecutionPlanNode::value_node(item_value))
            .add(ExecutionPlanNode::resolve_value(doc_path.join(key)))
            .add(ExecutionPlanNode::value_node(NodeValue::NULL));
          presence_check.add(item_check);
        }

        item_node.add(presence_check);
        plan_node.add(item_node);
      }

      plan_node.add(
        ExecutionPlanNode::action("expect:entries")
          .add(ExecutionPlanNode::action("lower-case")
            .add(ExecutionPlanNode::value_node(NodeValue::SLIST(keys.clone()))))
          .add(ExecutionPlanNode::resolve_value(doc_path.clone()))
          .add(
            ExecutionPlanNode::action("join")
              .add(ExecutionPlanNode::value_node("The following expected headers were missing: "))
              .add(ExecutionPlanNode::action("join-with")
                .add(ExecutionPlanNode::value_node(", "))
                .add(
                  ExecutionPlanNode::splat()
                    .add(ExecutionPlanNode::action("apply"))
                )
              )
          )
      );
    }
  }

  Ok(plan_node)
}

fn build_parameterised_header_plan(doc_path: &DocPath, val: &str) -> ExecutionPlanNode {
  let values: Vec<&str> = strip_whitespace(val, ";");
  let (header_value, header_params) = values.as_slice()
    .split_first()
    .unwrap_or((&"", &[]));
  let parameter_map = parse_charset_parameters(header_params);

  let mut apply_node = ExecutionPlanNode::action("tee");
  apply_node
    .add(ExecutionPlanNode::action("header:parse")
      .add(ExecutionPlanNode::resolve_value(doc_path)));
  apply_node.add(
    ExecutionPlanNode::action("match:equality")
      .add(ExecutionPlanNode::value_node(*header_value))
      .add(ExecutionPlanNode::action("to-string")
        .add(ExecutionPlanNode::resolve_current_value(&DocPath::new_unwrap("value"))))
      .add(ExecutionPlanNode::value_node(NodeValue::NULL))
  );

  if !parameter_map.is_empty() {
    let parameter_path = DocPath::new_unwrap("parameters");
    for (k, v) in &parameter_map {
      let mut parameter_node = ExecutionPlanNode::container(k.as_str());
      parameter_node.add(
        ExecutionPlanNode::action("if")
          .add(ExecutionPlanNode::action("check:exists")
            .add(ExecutionPlanNode::resolve_current_value(&parameter_path.join(k.as_str()))))
          .add(ExecutionPlanNode::action("match:equality")
            .add(ExecutionPlanNode::value_node(v.to_lowercase()))
            .add(ExecutionPlanNode::action("lower-case")
              .add(ExecutionPlanNode::resolve_current_value(&parameter_path.join(k.as_str()))))
            .add(ExecutionPlanNode::value_node(NodeValue::NULL)))
          .add(ExecutionPlanNode::action("error")
            .add(ExecutionPlanNode::value_node(
              format!("Expected a {} value of '{}' but it was missing", k, v)))
          )
      );
      apply_node.add(parameter_node);
    }
  }
  apply_node
}

fn setup_body_plan(
  expected: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlanNode> {
  // TODO: Look at the matching rules and generators here
  let mut plan_node = ExecutionPlanNode::container("body");

  match &expected.body {
    OptionalBody::Missing => {}
    OptionalBody::Empty | OptionalBody::Null => {
      plan_node.add(ExecutionPlanNode::action("expect:empty")
        .add(ExecutionPlanNode::resolve_value(DocPath::new("$.body")?)));
    }
    OptionalBody::Present(content, _, _) => {
      let content_type = expected.content_type().unwrap_or_else(|| TEXT.clone());
      let mut content_type_check_node = ExecutionPlanNode::action("if");
      content_type_check_node
        .add(
          ExecutionPlanNode::action("match:equality")
            .add(ExecutionPlanNode::value_node(content_type.to_string()))
            .add(ExecutionPlanNode::resolve_value(DocPath::new("$.content-type")?))
            .add(ExecutionPlanNode::value_node(NodeValue::NULL))
            .add(
              ExecutionPlanNode::action("error")
                .add(ExecutionPlanNode::value_node(NodeValue::STRING("Body type error - ".to_string())))
                .add(ExecutionPlanNode::action("apply"))
            )
        );
      if let Some(plan_builder) = get_body_plan_builder(&content_type) {
        content_type_check_node.add(plan_builder.build_plan(content, context)?);
      } else {
        let plan_builder = PlainTextBuilder::new();
        content_type_check_node.add(plan_builder.build_plan(content, context)?);
      }
      plan_node.add(content_type_check_node);
    }
  }

  Ok(plan_node)
}

/// Executes the request plan against the actual request.
pub fn execute_request_plan(
  plan: &ExecutionPlan,
  actual: &HttpRequest,
  context: &PlanMatchingContext
) -> anyhow::Result<ExecutionPlan> {
  let value_resolver = HttpRequestValueResolver {
    request: actual.clone()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(context);
  let path = vec![];
  let executed_tree = interpreter.walk_tree(&path, &plan.plan_root, &value_resolver)?;
  Ok(ExecutionPlan {
    plan_root: executed_tree
  })
}

