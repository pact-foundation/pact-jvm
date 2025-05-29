package au.com.dius.pact.core.matchers.engine

//#[derive(Clone, Debug, Default, PartialEq)]
//pub enum NodeResult {
//  /// Default value to make a node as successfully executed
//  #[default]
//  OK,
//  /// Marks a node as successfully executed with a result
//  VALUE(NodeValue),
//  /// Marks a node as unsuccessfully executed with an error
//  ERROR(String)
//}
//
//impl NodeResult {
//  /// Return the AND of this result with the given one
//  pub fn and(&self, option: &Option<NodeResult>) -> NodeResult {
//    if let Some(result) = option {
//      match self {
//        NodeResult::OK => match result {
//          NodeResult::OK => NodeResult::OK,
//          NodeResult::VALUE(_) => result.clone(),
//          NodeResult::ERROR(_) => result.clone()
//        }
//        NodeResult::VALUE(v1) => match result {
//          NodeResult::OK => self.clone(),
//          NodeResult::VALUE(v2) => NodeResult::VALUE(v1.and(v2)),
//          NodeResult::ERROR(_) => result.clone()
//        }
//        NodeResult::ERROR(_) => self.clone()
//      }
//    } else {
//      self.clone()
//    }
//  }
//
//  /// Converts the result value to a string
//  pub fn as_string(&self) -> Option<String> {
//    match self {
//      NodeResult::OK => None,
//      NodeResult::VALUE(val) => match val {
//        NodeValue::NULL => Some("".to_string()),
//        NodeValue::STRING(s) => Some(s.clone()),
//        NodeValue::BOOL(b) => Some(b.to_string()),
//        NodeValue::MMAP(m) => Some(format!("{:?}", m)),
//        NodeValue::SLIST(list) => Some(format!("{:?}", list)),
//        NodeValue::BARRAY(bytes) => Some(BASE64.encode(bytes)),
//        NodeValue::NAMESPACED(name, value) => Some(format!("{}:{}", name, value)),
//        NodeValue::UINT(ui) => Some(ui.to_string()),
//        NodeValue::JSON(json) => Some(json.to_string()),
//        NodeValue::ENTRY(k, v) => Some(format!("{} -> {}", k, v)),
//        NodeValue::LIST(list) => Some(format!("{:?}", list)),
//        #[cfg(feature = "xml")]
//        NodeValue::XML(node) => Some(node.to_string())
//      }
//      NodeResult::ERROR(_) => None
//    }
//  }
//
//  /// If the result is a number, returns it
//  pub fn as_number(&self) -> Option<u64> {
//    match self {
//      NodeResult::OK => None,
//      NodeResult::VALUE(val) => match val {
//        NodeValue::UINT(ui) => Some(*ui),
//        _ => None
//      }
//      NodeResult::ERROR(_) => None
//    }
//  }
//
//  /// Returns the associated value if there is one
//  pub fn as_value(&self) -> Option<NodeValue> {
//    match self {
//      NodeResult::OK => None,
//      NodeResult::VALUE(val) => Some(val.clone()),
//      NodeResult::ERROR(_) => None
//    }
//  }
//
//  /// If the result is a list of Strings, returns it
//  pub fn as_slist(&self) -> Option<Vec<String>> {
//    match self {
//      NodeResult::OK => None,
//      NodeResult::VALUE(val) => match val {
//        NodeValue::SLIST(list) => Some(list.clone()),
//        _ => None
//      }
//      NodeResult::ERROR(_) => None
//    }
//  }
//
//  /// If this value represents a truthy value (not NULL, false ot empty)
//  pub fn is_truthy(&self) -> bool {
//    match self {
//      NodeResult::OK => true,
//      NodeResult::VALUE(v) => match v {
//        NodeValue::STRING(s) => !s.is_empty(),
//        NodeValue::BOOL(b) => *b,
//        NodeValue::MMAP(m) => !m.is_empty(),
//        NodeValue::SLIST(l) => !l.is_empty(),
//        NodeValue::BARRAY(b) => !b.is_empty(),
//        NodeValue::UINT(ui) => *ui != 0,
//        NodeValue::LIST(l) => !l.is_empty(),
//        _ => false
//      }
//      NodeResult::ERROR(_) => false
//    }
//  }
//
//  /// Converts this node result into a truthy value
//  pub fn truthy(&self) -> NodeResult {
//    NodeResult::VALUE(NodeValue::BOOL(self.is_truthy()))
//  }
//
//  /// Unwraps the result into a value, or returns the error results as an error
//  pub fn value_or_error(&self) -> anyhow::Result<NodeValue> {
//    match self {
//      NodeResult::OK => Ok(NodeValue::BOOL(true)),
//      NodeResult::VALUE(v) => Ok(v.clone()),
//      NodeResult::ERROR(err) => Err(anyhow!(err.clone()))
//    }
//  }
//
//  /// If the result is an error result
//  pub fn is_err(&self) -> bool {
//    match self {
//      NodeResult::ERROR(_) => true,
//      _ => false
//    }
//  }
//
//  /// If the result is an ok result
//  pub fn is_ok(&self) -> bool {
//    match self {
//      NodeResult::OK => true,
//      _ => false
//    }
//  }
//}
//
//impl Display for NodeResult {
//  fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
//    match self {
//      NodeResult::OK => write!(f, "OK"),
//      NodeResult::VALUE(val) => write!(f, "{}", val.str_form()),
//      NodeResult::ERROR(err) => write!(f, "ERROR({})", err),
//    }
//  }
//}

/** Enum to store the result of executing a node */
sealed class NodeResult {
}
