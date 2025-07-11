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
