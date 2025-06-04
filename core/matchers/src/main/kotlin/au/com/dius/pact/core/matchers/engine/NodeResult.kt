package au.com.dius.pact.core.matchers.engine

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
  /** Default value to make a node as successfully executed */
  data object OK: NodeResult()

  /** Marks a node as successfully executed with a result */
  data class VALUE(val value: NodeValue): NodeResult()

  /** Marks a node as unsuccessfully executed with an error */
  data class ERROR(val message: String): NodeResult()

  /** If this value represents a truthy value (not NULL, false ot empty) */
  fun isTruthy(): Boolean {
    return when (this) {
      is OK -> true
      is VALUE -> when (this.value) {
        is NodeValue.BARRAY -> this.value.bytes.isNotEmpty()
        is NodeValue.BOOL -> this.value.bool
        is NodeValue.LIST -> this.value.items.isNotEmpty()
        is NodeValue.MMAP -> this.value.entries.isNotEmpty()
        is NodeValue.SLIST -> this.value.items.isNotEmpty()
        is NodeValue.STRING -> this.value.string.isNotEmpty()
        is NodeValue.UINT -> this.value.uint.toInt() != 0
        else -> false
      }
      is ERROR -> false
    }
  }

  /** Converts this node result into a truthy value */
  fun truthy() = VALUE(NodeValue.BOOL(isTruthy()))

  /** Return the AND of this result with the given one */
  fun and(result: NodeResult?): NodeResult {
    return if (result != null) {
      when (this) {
        is ERROR -> this
        OK -> when (result) {
          is ERROR -> result
          OK -> OK
          is VALUE -> result
        }
        is VALUE -> when (result) {
          is ERROR -> result
          OK -> this
          is VALUE -> VALUE(this.value.and(result.value))
        }
      }
    } else {
      this
    }
  }
}
