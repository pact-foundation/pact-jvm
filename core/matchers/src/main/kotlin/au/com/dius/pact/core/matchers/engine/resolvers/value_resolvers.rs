
/// Value resolver for an HTTP request
#[derive(Clone, Debug, Default)]
pub struct HttpRequestValueResolver {
  /// Request to resolve values against
  pub request: HttpRequest
}

impl ValueResolver for HttpRequestValueResolver {
  fn resolve(&self, path: &DocPath, _context: &PlanMatchingContext) -> anyhow::Result<NodeValue> {
    if let Some(field) = path.first_field() {
      match field {
        "method" => Ok(NodeValue::STRING(self.request.method.clone())),
        "path" => Ok(NodeValue::STRING(self.request.path.clone())),
        "query" => if path.len() == 2 || (path.len() == 3 && path.is_wildcard()) {
          let qp = self.request.query
            .clone()
            .unwrap_or_default()
            .iter()
            .map(|(k, v)| {
              (k.clone(), v.iter().map(|val| val.clone().unwrap_or_default()).collect())
            })
            .collect();
          Ok(NodeValue::MMAP(qp))
        } else if path.len() == 3 {
          let param_name = path.last_field().unwrap_or_default();
          let qp = self.request.query
            .clone()
            .unwrap_or_default();
          if let Some(val) = qp.get(param_name) {
            let values = val.iter()
              .map(|v| v.clone().unwrap_or_default())
              .collect_vec();
            if values.len() == 1 {
              Ok(NodeValue::STRING(values[0].clone()))
            } else {
              Ok(NodeValue::SLIST(values))
            }
          } else {
            Ok(NodeValue::NULL)
          }
        } else {
          Err(anyhow!("{} is not valid for a HTTP request query parameters", path))
        },
        "headers" => {
          let headers = self.request.headers
            .clone()
            .unwrap_or_default()
            .iter()
            .map(|(k, v)| (k.to_lowercase(), v.clone()))
            .collect();
          if path.len() == 2 || (path.len() == 3 && path.is_wildcard()) {
            Ok(NodeValue::MMAP(headers))
          } else if path.len() == 3 {
            let param_name = path.last_field().unwrap_or_default().to_lowercase();
            if let Some(val) = headers.get(&param_name) {
              if val.len() == 1 {
                Ok(NodeValue::STRING(val[0].clone()))
              } else {
                Ok(NodeValue::SLIST(val.clone()))
              }
            } else {
              Ok(NodeValue::NULL)
            }
          } else if path.len() == 4 && path.last().unwrap_or_default().is_index() {
            let param_name = path.last_field().unwrap_or_default().to_lowercase();
            if let Some(val) = headers.get(&param_name) {
              if let Some(PathToken::Index(index)) = path.last() {
                Ok(NodeValue::STRING(val[index].clone()))
              } else {
                Ok(NodeValue::NULL)
              }
            } else {
              Ok(NodeValue::NULL)
            }
          } else {
            Err(anyhow!("{} is not valid for HTTP request headers", path))
          }
        },
        "content-type" => {
          Ok(self.request.content_type()
            .map(|ct| NodeValue::STRING(ct.to_string()))
            .unwrap_or(NodeValue::NULL))
        },
        "body" if path.len() == 2 => match &self.request.body {
          OptionalBody::Present(bytes, _, _) => Ok(NodeValue::BARRAY(bytes.to_vec())),
          _ => Ok(NodeValue::NULL)
        }
        _ => Err(anyhow!("{} is not valid for a HTTP request", path))
      }
    } else {
      Err(anyhow!("{} is not valid for a HTTP request", path))
    }
  }
}

#[cfg(test)]
mod tests {
  use expectest::prelude::*;
  use googletest::prelude::*;
  use maplit::hashmap;
  use rstest::rstest;

  use pact_models::path_exp::DocPath;

  use crate::engine::{NodeValue, PlanMatchingContext};
  use crate::engine::value_resolvers::{HttpRequestValueResolver, ValueResolver};

  #[rstest(
    case("$.method", NodeValue::STRING("GET".to_string())),
    case("$.path", NodeValue::STRING("/".to_string())),
    case("$.query", NodeValue::MMAP(hashmap!{})),
    case("$.headers", NodeValue::MMAP(hashmap!{}))
  )]
  fn http_request_resolve_values(#[case] path: &str, #[case] expected: NodeValue) {
    let path = DocPath::new(path).unwrap();
    let resolver = HttpRequestValueResolver::default();
    let context = PlanMatchingContext::default();
    expect!(resolver.resolve(&path, &context).unwrap()).to(be_equal_to(expected));
  }

  #[googletest::test]
  fn http_request_resolve_failures() {
    let resolver = HttpRequestValueResolver::default();
    let context = PlanMatchingContext::default();

    let path = DocPath::root();
    expect_that!(resolver.resolve(&path, &context), err(displays_as(eq("$ is not valid for a HTTP request"))));

    let path = DocPath::new_unwrap("$.blah");
    expect_that!(resolver.resolve(&path, &context), err(displays_as(eq("$.blah is not valid for a HTTP request"))));
  }
}
