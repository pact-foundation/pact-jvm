use expectest::prelude::*;
use maplit::hashmap;
use pretty_assertions::assert_eq;
use rstest::rstest;
use serde_json::json;

use pact_models::bodies::OptionalBody;
use pact_models::content_types::TEXT;
use pact_models::matchingrules;
use pact_models::v4::http_parts::HttpRequest;
use pact_models::v4::interaction::V4Interaction;
use pact_models::v4::synch_http::SynchronousHttp;
use crate::engine::{
  build_request_plan,
  execute_request_plan,
  NodeResult,
  NodeValue,
  PlanMatchingContext
};
use crate::{BodyMatchResult, MatchingRule, RequestMatchResult};
use crate::Mismatch::{ MethodMismatch, BodyMismatch };

mod walk_tree_tests;
mod query_tests;
mod header_tests;

#[rstest(
  case("", "''"),
  case("simple", "'simple'"),
  case("simple sentence", "'simple sentence'"),
  case("\"quoted sentence\"", "'\"quoted sentence\"'"),
  case("'quoted sentence'", "\"'quoted sentence'\""),
  case("new\nline", "\"new\\nline\""),
)]
fn node_value_str_form_escapes_strings(#[case] input: &str, #[case] expected: &str) {
  let node = NodeValue::STRING(input.to_string());
  expect!(node.str_form()).to(be_equal_to(expected));
}

#[rstest(
  case(NodeResult::OK, None, NodeResult::OK),
  case(NodeResult::VALUE(NodeValue::NULL), None, NodeResult::VALUE(NodeValue::NULL)),
  case(NodeResult::ERROR("".to_string()), None, NodeResult::ERROR("".to_string())),
  case(NodeResult::OK, Some(NodeResult::OK), NodeResult::OK),
  case(NodeResult::OK, Some(NodeResult::VALUE(NodeValue::NULL)), NodeResult::VALUE(NodeValue::NULL)),
  case(NodeResult::OK, Some(NodeResult::ERROR("error".to_string())), NodeResult::ERROR("error".to_string())),
  case(NodeResult::VALUE(NodeValue::NULL), Some(NodeResult::OK), NodeResult::VALUE(NodeValue::NULL)),
  case(NodeResult::VALUE(NodeValue::NULL), Some(NodeResult::VALUE(NodeValue::NULL)), NodeResult::VALUE(NodeValue::NULL)),
  case(NodeResult::VALUE(NodeValue::NULL), Some(NodeResult::VALUE(NodeValue::UINT(100))), NodeResult::VALUE(NodeValue::UINT(100))),
  case(NodeResult::VALUE(NodeValue::BOOL(false)), Some(NodeResult::VALUE(NodeValue::UINT(100))), NodeResult::VALUE(NodeValue::BOOL(false))),
  case(NodeResult::VALUE(NodeValue::BOOL(true)), Some(NodeResult::VALUE(NodeValue::NULL)), NodeResult::VALUE(NodeValue::BOOL(false))),
  case(NodeResult::VALUE(NodeValue::BOOL(true)), Some(NodeResult::VALUE(NodeValue::BOOL(false))), NodeResult::VALUE(NodeValue::BOOL(false))),
  case(NodeResult::VALUE(NodeValue::NULL), Some(NodeResult::ERROR("error".to_string())), NodeResult::ERROR("error".to_string())),
  case(NodeResult::ERROR("error".to_string()), Some(NodeResult::OK), NodeResult::ERROR("error".to_string())),
  case(NodeResult::ERROR("error".to_string()), Some(NodeResult::VALUE(NodeValue::NULL)), NodeResult::ERROR("error".to_string())),
  case(NodeResult::ERROR("error".to_string()), Some(NodeResult::ERROR("error2".to_string())), NodeResult::ERROR("error".to_string())),
)]
fn node_result_and(#[case] a: NodeResult, #[case] b: Option<NodeResult>, #[case] result: NodeResult) {
  expect!(a.and(&b)).to(be_equal_to(result));
}



#[test_log::test]
fn simple_json_match_request_test() -> anyhow::Result<()> {
  let request = HttpRequest {
    method: "POST".to_string(),
    path: "/test".to_string(),
    query: None,
    headers: None,
    body: OptionalBody::from(&json!({
        "b": "22"
      })),
    matching_rules: Default::default(),
    generators: Default::default(),
  };
  let expected_request = HttpRequest {
    method: "POST".to_string(),
    path: "/test".to_string(),
    query: None,
    headers: None,
    body: OptionalBody::from(&json!({
        "a": 100,
        "b": 200.1
      })),
    matching_rules: Default::default(),
    generators: Default::default(),
  };
  let mut context = PlanMatchingContext::default();
  let plan = build_request_plan(&expected_request, &context)?;

  assert_eq!(r#"(
  :request (
    :method (
      #{'method == POST'},
      %match:equality (
        'POST',
        %upper-case (
          $.method
        ),
        NULL
      )
    ),
    :path (
      #{"path == '/test'"},
      %match:equality (
        '/test',
        $.path,
        NULL
      )
    ),
    :"query parameters" (
      %expect:empty (
        $.query,
        %join (
          'Expected no query parameters but got ',
          $.query
        )
      )
    ),
    :body (
      %if (
        %match:equality (
          'application/json;charset=utf-8',
          $.content-type,
          NULL,
          %error (
            'Body type error - ',
            %apply ()
          )
        ),
        %tee (
          %json:parse (
            $.body
          ),
          :$ (
            %json:expect:entries (
              'OBJECT',
              ['a', 'b'],
              ~>$
            ),
            %expect:only-entries (
              ['a', 'b'],
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
                json:200.1,
                ~>$.b,
                NULL
              )
            )
          )
        )
      )
    )
  )
)
"#, plan.pretty_form());

  let executed_plan = execute_request_plan(&plan, &request, &mut context)?;
  assert_eq!(r#"(
  :request (
    :method (
      #{'method == POST'},
      %match:equality (
        'POST' => 'POST',
        %upper-case (
          $.method => 'POST'
        ) => 'POST',
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true),
    :path (
      #{"path == '/test'"},
      %match:equality (
        '/test' => '/test',
        $.path => '/test',
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true),
    :"query parameters" (
      %expect:empty (
        $.query => {},
        %join (
          'Expected no query parameters but got ',
          $.query
        )
      ) => BOOL(true)
    ) => BOOL(true),
    :body (
      %if (
        %match:equality (
          'application/json;charset=utf-8' => 'application/json;charset=utf-8',
          $.content-type => 'application/json;charset=utf-8',
          NULL => NULL,
          %error (
            'Body type error - ',
            %apply ()
          )
        ) => BOOL(true),
        %tee (
          %json:parse (
            $.body => BYTES(10, eyJiIjoiMjIifQ==)
          ) => json:{"b":"22"},
          :$ (
            %json:expect:entries (
              'OBJECT' => 'OBJECT',
              ['a', 'b'] => ['a', 'b'],
              ~>$ => json:{"b":"22"}
            ) => ERROR(The following expected entries were missing from the actual Object: a),
            %expect:only-entries (
              ['a', 'b'] => ['a', 'b'],
              ~>$ => json:{"b":"22"}
            ) => OK,
            :$.a (
              %match:equality (
                json:100 => json:100,
                ~>$.a => NULL,
                NULL => NULL
              ) => ERROR(Expected null (Null) to be equal to 100 (Integer))
            ) => BOOL(false),
            :$.b (
              %match:equality (
                json:200.1 => json:200.1,
                ~>$.b => json:"22",
                NULL => NULL
              ) => ERROR(Expected '22' (String) to be equal to 200.1 (Decimal))
            ) => BOOL(false)
          ) => BOOL(false)
        ) => BOOL(false)
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  assert_eq!(r#"request:
  method: method == POST - OK
  path: path == '/test' - OK
  query parameters: - OK
  body:
    $: - ERROR The following expected entries were missing from the actual Object: a
      $.a: - ERROR Expected null (Null) to be equal to 100 (Integer)
      $.b: - ERROR Expected '22' (String) to be equal to 200.1 (Decimal)
"#, executed_plan.generate_summary(false));

  let mismatches: RequestMatchResult = executed_plan.into();
  assert_eq!(RequestMatchResult {
    method: None,
    path: None,
    headers: hashmap!{},
    query: hashmap!{},
    body: BodyMatchResult::BodyMismatches(hashmap!{
      "$.a".to_string() => vec![
        BodyMismatch {
          path: "$.a".to_string(),
          expected: None,
          actual: None,
          mismatch: "Expected null (Null) to be equal to 100 (Integer)".to_string()
        }
      ],
      "$.b".to_string() => vec![
        BodyMismatch {
          path: "$.b".to_string(),
          expected: None,
          actual: None,
          mismatch: "Expected '22' (String) to be equal to 200.1 (Decimal)".to_string()
        }
      ],
      "$".to_string() => vec![
        BodyMismatch {
          path: "$".to_string(),
          expected: None,
          actual: None,
          mismatch: "The following expected entries were missing from the actual Object: a".to_string()
        }
      ]
    })
  }, mismatches);

  Ok(())
}

#[test_log::test]
fn match_path_with_matching_rule() -> anyhow::Result<()> {
  let request = HttpRequest {
    method: "get".to_string(),
    path: "/test12345".to_string(),
    .. Default::default()
  };
  let matching_rules = matchingrules! {
    "path" => { "" => [ MatchingRule::Regex("\\/test[0-9]+".to_string()) ] }
  };
  let expected_request = HttpRequest {
    method: "get".to_string(),
    path: "/test".to_string(),
    matching_rules: matching_rules.clone(),
    .. Default::default()
  };
  let expected_interaction = SynchronousHttp {
    request: expected_request.clone(),
    .. SynchronousHttp::default()
  };
  let mut context = PlanMatchingContext {
    interaction: expected_interaction.boxed_v4(),
    .. PlanMatchingContext::default()
  };
  let plan = build_request_plan(&expected_request, &context)?;

  assert_eq!(
r#"(
  :request (
    :method (
      #{'method == GET'},
      %match:equality (
        'GET',
        %upper-case (
          $.method
        ),
        NULL
      )
    ),
    :path (
      #{"path must match the regular expression /\\/test[0-9]+/"},
      %match:regex (
        '/test',
        $.path,
        json:{"regex":"\\/test[0-9]+"}
      )
    ),
    :"query parameters" (
      %expect:empty (
        $.query,
        %join (
          'Expected no query parameters but got ',
          $.query
        )
      )
    )
  )
)
"#, plan.pretty_form());

  let executed_plan = execute_request_plan(&plan, &request, &mut context)?;
  assert_eq!(r#"(
  :request (
    :method (
      #{'method == GET'},
      %match:equality (
        'GET' => 'GET',
        %upper-case (
          $.method => 'get'
        ) => 'GET',
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true),
    :path (
      #{"path must match the regular expression /\\/test[0-9]+/"},
      %match:regex (
        '/test' => '/test',
        $.path => '/test12345',
        json:{"regex":"\\/test[0-9]+"} => json:{"regex":"\\/test[0-9]+"}
      ) => BOOL(true)
    ) => BOOL(true),
    :"query parameters" (
      %expect:empty (
        $.query => {},
        %join (
          'Expected no query parameters but got ',
          $.query
        )
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    method: "get".to_string(),
    path: "/test12345X".to_string(),
    .. Default::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context)?;
  assert_eq!(r#"(
  :request (
    :method (
      #{'method == GET'},
      %match:equality (
        'GET' => 'GET',
        %upper-case (
          $.method => 'get'
        ) => 'GET',
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true),
    :path (
      #{"path must match the regular expression /\\/test[0-9]+/"},
      %match:regex (
        '/test' => '/test',
        $.path => '/test12345X',
        json:{"regex":"\\/test[0-9]+"} => json:{"regex":"\\/test[0-9]+"}
      ) => ERROR(Expected '/test12345X' to match '\/test[0-9]+')
    ) => BOOL(false),
    :"query parameters" (
      %expect:empty (
        $.query => {},
        %join (
          'Expected no query parameters but got ',
          $.query
        )
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  Ok(())
}
