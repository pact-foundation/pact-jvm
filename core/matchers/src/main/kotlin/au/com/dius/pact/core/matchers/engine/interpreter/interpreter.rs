
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
