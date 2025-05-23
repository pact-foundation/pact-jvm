use maplit::hashmap;

use pact_models::matchingrules;
use pact_models::matchingrules::MatchingRule;
use pact_models::prelude::v4::SynchronousHttp;
use pact_models::v4::http_parts::HttpRequest;
use pact_models::v4::interaction::V4Interaction;

use crate::engine::{execute_request_plan, ExecutionPlan, setup_query_plan};
use crate::engine::context::PlanMatchingContext;

#[test]
fn match_query_with_no_query_strings() {
  let expected = HttpRequest::default();
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("query-test");

  plan.add(setup_query_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :query-test (
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

  let request = HttpRequest::default();
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
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
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      %expect:empty (
        $.query => {'a': 'b'},
        %join (
          'Expected no query parameters but got ' => 'Expected no query parameters but got ',
          $.query => {'a': 'b'}
        ) => "Expected no query parameters but got {'a': 'b'}"
      ) => ERROR(Expected no query parameters but got {'a': 'b'})
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_query_with_expected_query_string() {
  let expected = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())]
    }),
    .. HttpRequest::default()
  };
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("query-test");

  plan.add(setup_query_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a
          ),
          %match:equality (
            'b',
            $.query.a,
            NULL
          )
        )
      ),
      %expect:entries (
        ['a'],
        $.query,
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ),
      %expect:only-entries (
        ['a'],
        $.query,
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      )
    )
  )
)
"#, plan.pretty_form());

  let request = HttpRequest::default();
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => NULL
          ) => BOOL(false),
          %match:equality (
            'b',
            $.query.a,
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['a'] => ['a'],
        $.query => {},
        %join (
          'The following expected query parameters were missing: ' => 'The following expected query parameters were missing: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'a'
            ) => OK
          ) => 'a'
        ) => 'The following expected query parameters were missing: a'
      ) => ERROR(The following expected query parameters were missing: a),
      %expect:only-entries (
        ['a'] => ['a'],
        $.query => {},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => 'b'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.query.a => 'b',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['a'] => ['a'],
        $.query => {'a': 'b'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['a'] => ['a'],
        $.query => {'a': 'b'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("c".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => 'c'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.query.a => 'c',
            NULL => NULL
          ) => ERROR(Expected 'c' to be equal to 'b')
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['a'] => ['a'],
        $.query => {'a': 'c'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['a'] => ['a'],
        $.query => {'a': 'c'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())],
      "b".to_string() => vec![Some("c".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => 'b'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.query.a => 'b',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['a'] => ['a'],
        $.query => {'a': 'b', 'b': 'c'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['a'] => ['a'],
        $.query => {'a': 'b', 'b': 'c'},
        %join (
          'The following query parameters were not expected: ' => 'The following query parameters were not expected: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'b'
            ) => OK
          ) => 'b'
        ) => 'The following query parameters were not expected: b'
      ) => ERROR(The following query parameters were not expected: b)
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "b".to_string() => vec![Some("c".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => NULL
          ) => BOOL(false),
          %match:equality (
            'b',
            $.query.a,
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['a'] => ['a'],
        $.query => {'b': 'c'},
        %join (
          'The following expected query parameters were missing: ' => 'The following expected query parameters were missing: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'a'
            ) => OK
          ) => 'a'
        ) => 'The following expected query parameters were missing: a'
      ) => ERROR(The following expected query parameters were missing: a),
      %expect:only-entries (
        ['a'] => ['a'],
        $.query => {'b': 'c'},
        %join (
          'The following query parameters were not expected: ' => 'The following query parameters were not expected: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'b'
            ) => OK
          ) => 'b'
        ) => 'The following query parameters were not expected: b'
      ) => ERROR(The following query parameters were not expected: b)
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_query_with_matching_rule() {
  let matching_rules = matchingrules! {
    "query" => { "user_id" => [ MatchingRule::Regex("^[0-9]+$".to_string()) ] }
  };
  let expected_request = HttpRequest {
    query: Some(hashmap!{
      "user_id".to_string() => vec![Some("1".to_string())],
      "field".to_string() => vec![Some("test".to_string())]
    }),
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

  let mut plan = ExecutionPlan::new("query-test");
  plan.add(setup_query_plan(&expected_request, &context.for_query()).unwrap());

  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :field (
        #{"field='test'"},
        %if (
          %check:exists (
            $.query.field
          ),
          %match:equality (
            'test',
            $.query.field,
            NULL
          )
        )
      ),
      :user_id (
        #{'user_id must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.query.user_id
          ),
          %match:regex (
            '1',
            $.query.user_id,
            json:{"regex":"^[0-9]+$"}
          )
        )
      ),
      %expect:entries (
        ['field', 'user_id'],
        $.query,
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ),
      %expect:only-entries (
        ['field', 'user_id'],
        $.query,
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      )
    )
  )
)
"#, plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "field".to_string() => vec![Some("test".to_string())],
      "user_id".to_string() => vec![Some("2455324356421".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :field (
        #{"field='test'"},
        %if (
          %check:exists (
            $.query.field => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.query.field => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :user_id (
        #{'user_id must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.query.user_id => '2455324356421'
          ) => BOOL(true),
          %match:regex (
            '1' => '1',
            $.query.user_id => '2455324356421',
            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['field', 'user_id'] => ['field', 'user_id'],
        $.query => {'field': 'test', 'user_id': '2455324356421'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['field', 'user_id'] => ['field', 'user_id'],
        $.query => {'field': 'test', 'user_id': '2455324356421'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "field".to_string() => vec![Some("test".to_string())],
      "user_id".to_string() => vec![Some("100Kb".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :field (
        #{"field='test'"},
        %if (
          %check:exists (
            $.query.field => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.query.field => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :user_id (
        #{'user_id must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.query.user_id => '100Kb'
          ) => BOOL(true),
          %match:regex (
            '1' => '1',
            $.query.user_id => '100Kb',
            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
          ) => ERROR(Expected '100Kb' to match '^[0-9]+$')
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['field', 'user_id'] => ['field', 'user_id'],
        $.query => {'field': 'test', 'user_id': '100Kb'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['field', 'user_id'] => ['field', 'user_id'],
        $.query => {'field': 'test', 'user_id': '100Kb'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_query_with_query_values_having_different_lengths() {
  let expected = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())],
      "c".to_string() => vec![Some("d".to_string()), Some("e".to_string())]
    }),
    .. HttpRequest::default()
  };
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("query-test");
  plan.add(setup_query_plan(&expected, &context.for_query()).unwrap());

  let request = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string())],
      "c".to_string() => vec![Some("d".to_string()), Some("e".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => 'b'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.query.a => 'b',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :c (
        #{"c=['d', 'e']"},
        %if (
          %check:exists (
            $.query.c => ['d', 'e']
          ) => BOOL(true),
          %match:equality (
            ['d', 'e'] => ['d', 'e'],
            $.query.c => ['d', 'e'],
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['a', 'c'] => ['a', 'c'],
        $.query => {'a': 'b', 'c': ['d', 'e']},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['a', 'c'] => ['a', 'c'],
        $.query => {'a': 'b', 'c': ['d', 'e']},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "a".to_string() => vec![Some("b".to_string()), Some("e".to_string())],
      "c".to_string() => vec![Some("d".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :a (
        #{"a='b'"},
        %if (
          %check:exists (
            $.query.a => ['b', 'e']
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.query.a => ['b', 'e'],
            NULL => NULL
          ) => ERROR(Expected 'e' to be equal to 'b')
        ) => BOOL(false)
      ) => BOOL(false),
      :c (
        #{"c=['d', 'e']"},
        %if (
          %check:exists (
            $.query.c => 'd'
          ) => BOOL(true),
          %match:equality (
            ['d', 'e'] => ['d', 'e'],
            $.query.c => 'd',
            NULL => NULL
          ) => ERROR(Expected ["d"] to be equal to ["d","e"])
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['a', 'c'] => ['a', 'c'],
        $.query => {'a': ['b', 'e'], 'c': 'd'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['a', 'c'] => ['a', 'c'],
        $.query => {'a': ['b', 'e'], 'c': 'd'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_query_with_number_type_matching_rule() {
  let matching_rules = matchingrules! {
    "query" => { "user_id" => [ MatchingRule::Integer ] }
  };
  let expected_request = HttpRequest {
    query: Some(hashmap!{
      "user_id".to_string() => vec![Some("1".to_string())]
    }),
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

  let mut plan = ExecutionPlan::new("query-test");
  plan.add(setup_query_plan(&expected_request, &context.for_query()).unwrap());

  let request = HttpRequest {
    query: Some(hashmap!{
      "user_id".to_string() => vec![Some("2455324356421".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :user_id (
        #{'user_id must be an integer'},
        %if (
          %check:exists (
            $.query.user_id => '2455324356421'
          ) => BOOL(true),
          %match:integer (
            '1' => '1',
            $.query.user_id => '2455324356421',
            json:{} => json:{}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': '2455324356421'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': '2455324356421'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "user_id".to_string() => vec![Some("100".to_string()), Some("200".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :user_id (
        #{'user_id must be an integer'},
        %if (
          %check:exists (
            $.query.user_id => ['100', '200']
          ) => BOOL(true),
          %match:integer (
            '1' => '1',
            $.query.user_id => ['100', '200'],
            json:{} => json:{}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': ['100', '200']},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': ['100', '200']},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "user_id".to_string() => vec![Some("100x".to_string()), Some("200".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :user_id (
        #{'user_id must be an integer'},
        %if (
          %check:exists (
            $.query.user_id => ['100x', '200']
          ) => BOOL(true),
          %match:integer (
            '1' => '1',
            $.query.user_id => ['100x', '200'],
            json:{} => json:{}
          ) => ERROR(Expected '100x' to match an integer number)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': ['100x', '200']},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['user_id'] => ['user_id'],
        $.query => {'user_id': ['100x', '200']},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_query_with_min_type_matching_rules() {
  let matching_rules = matchingrules! {
    "query" => { "id" => [ MatchingRule::MinType(2) ] }
  };
  let expected_request = HttpRequest {
    query: Some(hashmap!{
      "id".to_string() => vec![Some("1".to_string()), Some("2".to_string())]
    }),
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

  let mut plan = ExecutionPlan::new("query-test");
  plan.add(setup_query_plan(&expected_request, &context.for_query()).unwrap());

  let request = HttpRequest {
    query: Some(hashmap!{
      "id".to_string() => vec![
        Some("1".to_string()),
        Some("2".to_string()),
        Some("3".to_string()),
        Some("4".to_string())
      ]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :id (
        #{'id must match by type and have at least 2 items'},
        %if (
          %check:exists (
            $.query.id => ['1', '2', '3', '4']
          ) => BOOL(true),
          %match:min-type (
            ['1', '2'] => ['1', '2'],
            $.query.id => ['1', '2', '3', '4'],
            json:{"min":2} => json:{"min":2}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        ['id'] => ['id'],
        $.query => {'id': ['1', '2', '3', '4']},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['id'] => ['id'],
        $.query => {'id': ['1', '2', '3', '4']},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(true)
  ) => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    query: Some(hashmap!{
      "id".to_string() => vec![Some("100".to_string())]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :query-test (
    :"query parameters" (
      :id (
        #{'id must match by type and have at least 2 items'},
        %if (
          %check:exists (
            $.query.id => '100'
          ) => BOOL(true),
          %match:min-type (
            ['1', '2'] => ['1', '2'],
            $.query.id => '100',
            json:{"min":2} => json:{"min":2}
          ) => ERROR(Expected [100] (size 1) to have minimum size of 2)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        ['id'] => ['id'],
        $.query => {'id': '100'},
        %join (
          'The following expected query parameters were missing: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK,
      %expect:only-entries (
        ['id'] => ['id'],
        $.query => {'id': '100'},
        %join (
          'The following query parameters were not expected: ',
          %join-with (
            ', ',
            ** (
              %apply ()
            )
          )
        )
      ) => OK
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}
