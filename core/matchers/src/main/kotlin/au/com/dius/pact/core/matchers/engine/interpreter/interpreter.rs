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
