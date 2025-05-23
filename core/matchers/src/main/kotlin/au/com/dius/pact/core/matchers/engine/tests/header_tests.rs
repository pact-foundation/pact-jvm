use maplit::hashmap;

use pact_models::matchingrules;
use pact_models::matchingrules::MatchingRule;
use pact_models::prelude::v4::SynchronousHttp;
use pact_models::v4::http_parts::HttpRequest;
use pact_models::v4::interaction::V4Interaction;

use crate::engine::{execute_request_plan, ExecutionPlan, setup_header_plan};
use crate::engine::context::PlanMatchingContext;

#[test]
fn match_headers_where_there_are_none() {
  let expected = HttpRequest::default();
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("header-test");

  plan.add(setup_header_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :header-test ()
)
"#, plan.pretty_form());

  let request = HttpRequest::default();
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test () => BOOL(true)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    headers: Some(hashmap!{
      "HEADER_X".to_string() => vec!["A".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test () => BOOL(true)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_headers_with_expected_headers() {
  let expected = HttpRequest {
    headers: Some(hashmap!{
      "HEADER-X".to_string() => vec!["b".to_string()]
    }),
    .. HttpRequest::default()
  };
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("header-test");

  plan.add(setup_header_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X']
          ),
          %match:equality (
            'b',
            $.headers['HEADER-X'],
            NULL
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['HEADER-X']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X'] => NULL
          ) => BOOL(false),
          %match:equality (
            'b',
            $.headers['HEADER-X'],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['HEADER-X'] => ['HEADER-X']
        ) => ['header-x'],
        $.headers => {},
        %join (
          'The following expected headers were missing: ' => 'The following expected headers were missing: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'header-x'
            ) => OK
          ) => 'header-x'
        ) => 'The following expected headers were missing: header-x'
      ) => ERROR(The following expected headers were missing: header-x)
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());

  let request = HttpRequest {
    headers: Some(hashmap!{
      "HEADER-X".to_string() => vec!["b".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X'] => 'b'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.headers['HEADER-X'] => 'b',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['HEADER-X'] => ['HEADER-X']
        ) => ['header-x'],
        $.headers => {'header-x': 'b'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "HEADER-X".to_string() => vec!["C".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X'] => 'C'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.headers['HEADER-X'] => 'C',
            NULL => NULL
          ) => ERROR(Expected 'C' to be equal to 'b')
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['HEADER-X'] => ['HEADER-X']
        ) => ['header-x'],
        $.headers => {'header-x': 'C'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "HEADER-X".to_string() => vec!["b".to_string()],
      "HEADER-Y".to_string() => vec!["b".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X'] => 'b'
          ) => BOOL(true),
          %match:equality (
            'b' => 'b',
            $.headers['HEADER-X'] => 'b',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['HEADER-X'] => ['HEADER-X']
        ) => ['header-x'],
        $.headers => {'header-x': 'b', 'header-y': 'b'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "HEADER-Y".to_string() => vec!["b".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :HEADER-X (
        #{"HEADER-X='b'"},
        %if (
          %check:exists (
            $.headers['HEADER-X'] => NULL
          ) => BOOL(false),
          %match:equality (
            'b',
            $.headers['HEADER-X'],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['HEADER-X'] => ['HEADER-X']
        ) => ['header-x'],
        $.headers => {'header-y': 'b'},
        %join (
          'The following expected headers were missing: ' => 'The following expected headers were missing: ',
          %join-with (
            ', ' => ', ',
            ** (
              %apply () => 'header-x'
            ) => OK
          ) => 'header-x'
        ) => 'The following expected headers were missing: header-x'
      ) => ERROR(The following expected headers were missing: header-x)
    ) => BOOL(false)
  ) => BOOL(false)
)
"#, executed_plan.pretty_form());
}

#[test]
fn match_headers_with_matching_rule() {
  let matching_rules = matchingrules! {
    "header" => { "REF-ID" => [ MatchingRule::Regex("^[0-9]+$".to_string()) ] }
  };
  let expected_request = HttpRequest {
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
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

  let mut plan = ExecutionPlan::new("header-test");
  plan.add(setup_header_plan(&expected_request, &context.for_headers()).unwrap());

  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE']
          ),
          %match:equality (
            'test',
            $.headers['REF-CODE'],
            NULL
          )
        )
      ),
      :REF-ID (
        #{'REF-ID must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.headers['REF-ID']
          ),
          %match:regex (
            '1234',
            $.headers['REF-ID'],
            json:{"regex":"^[0-9]+$"}
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["9023470945622".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '9023470945622'
          ) => BOOL(true),
          %match:regex (
            '1234' => '1234',
            $.headers['REF-ID'] => '9023470945622',
            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': '9023470945622'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["9023470X945622".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must match the regular expression /^[0-9]+$/'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '9023470X945622'
          ) => BOOL(true),
          %match:regex (
            '1234' => '1234',
            $.headers['REF-ID'] => '9023470X945622',
            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
          ) => ERROR(Expected '9023470X945622' to match '^[0-9]+$')
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': '9023470X945622'},
        %join (
          'The following expected headers were missing: ',
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
fn match_headers_with_values_having_different_lengths() {
  let expected_request = HttpRequest {
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string(), "test2".to_string()]
    }),
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

  let mut plan = ExecutionPlan::new("header-test");
  plan.add(setup_header_plan(&expected_request, &context.for_headers()).unwrap());

  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE=['test', 'test2']"},
        %if (
          %check:exists (
            $.headers['REF-CODE']
          ),
          %match:equality (
            ['test', 'test2'],
            $.headers['REF-CODE'],
            NULL
          )
        )
      ),
      :REF-ID (
        #{"REF-ID='1234'"},
        %if (
          %check:exists (
            $.headers['REF-ID']
          ),
          %match:equality (
            '1234',
            $.headers['REF-ID'],
            NULL
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string(), "test2".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE=['test', 'test2']"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => ['test', 'test2']
          ) => BOOL(true),
          %match:equality (
            ['test', 'test2'] => ['test', 'test2'],
            $.headers['REF-CODE'] => ['test', 'test2'],
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{"REF-ID='1234'"},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '1234'
          ) => BOOL(true),
          %match:equality (
            '1234' => '1234',
            $.headers['REF-ID'] => '1234',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': ['test', 'test2'], 'ref-id': '1234'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string(), "1234".to_string(), "4567".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE=['test', 'test2']"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            ['test', 'test2'] => ['test', 'test2'],
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => ERROR(Expected ["test"] to be equal to ["test","test2"])
        ) => BOOL(false)
      ) => BOOL(false),
      :REF-ID (
        #{"REF-ID='1234'"},
        %if (
          %check:exists (
            $.headers['REF-ID'] => ['1234', '1234', '4567']
          ) => BOOL(true),
          %match:equality (
            '1234' => '1234',
            $.headers['REF-ID'] => ['1234', '1234', '4567'],
            NULL => NULL
          ) => ERROR(Expected '4567' to be equal to '1234')
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': ['1234', '1234', '4567']},
        %join (
          'The following expected headers were missing: ',
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
fn match_headers_with_number_type_matching_rule() {
  let matching_rules = matchingrules! {
    "header" => { "REF-ID" => [ MatchingRule::Integer ] }
  };
  let expected_request = HttpRequest {
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
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

  let mut plan = ExecutionPlan::new("header-test");
  plan.add(setup_header_plan(&expected_request, &context.for_headers()).unwrap());

  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE']
          ),
          %match:equality (
            'test',
            $.headers['REF-CODE'],
            NULL
          )
        )
      ),
      :REF-ID (
        #{'REF-ID must be an integer'},
        %if (
          %check:exists (
            $.headers['REF-ID']
          ),
          %match:integer (
            '1234',
            $.headers['REF-ID'],
            json:{}
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["9023470945622".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must be an integer'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '9023470945622'
          ) => BOOL(true),
          %match:integer (
            '1234' => '1234',
            $.headers['REF-ID'] => '9023470945622',
            json:{} => json:{}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': '9023470945622'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["9023470X945622".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must be an integer'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '9023470X945622'
          ) => BOOL(true),
          %match:integer (
            '1234' => '1234',
            $.headers['REF-ID'] => '9023470X945622',
            json:{} => json:{}
          ) => ERROR(Expected '9023470X945622' to match an integer number)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': '9023470X945622'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1111".to_string(), "2222".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must be an integer'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => ['1111', '2222']
          ) => BOOL(true),
          %match:integer (
            '1234' => '1234',
            $.headers['REF-ID'] => ['1111', '2222'],
            json:{} => json:{}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': ['1111', '2222']},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1111".to_string(), "two".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must be an integer'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => ['1111', 'two']
          ) => BOOL(true),
          %match:integer (
            '1234' => '1234',
            $.headers['REF-ID'] => ['1111', 'two'],
            json:{} => json:{}
          ) => ERROR(Expected 'two' to match an integer number)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': ['1111', 'two']},
        %join (
          'The following expected headers were missing: ',
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
fn match_headers_with_min_type_matching_rules() {
  let matching_rules = matchingrules! {
    "header" => { "REF-ID" => [ MatchingRule::MinType(2) ] }
  };
  let expected_request = HttpRequest {
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1234".to_string(), "4567".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
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

  let mut plan = ExecutionPlan::new("header-test");
  plan.add(setup_header_plan(&expected_request, &context.for_headers()).unwrap());

  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE']
          ),
          %match:equality (
            'test',
            $.headers['REF-CODE'],
            NULL
          )
        )
      ),
      :REF-ID (
        #{'REF-ID must match by type and have at least 2 items'},
        %if (
          %check:exists (
            $.headers['REF-ID']
          ),
          %match:min-type (
            ['1234', '4567'],
            $.headers['REF-ID'],
            json:{"min":2}
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1".to_string(), "1".to_string(), "1".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must match by type and have at least 2 items'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => ['1', '1', '1']
          ) => BOOL(true),
          %match:min-type (
            ['1234', '4567'] => ['1234', '4567'],
            $.headers['REF-ID'] => ['1', '1', '1'],
            json:{"min":2} => json:{"min":2}
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': ['1', '1', '1']},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "REF-ID".to_string() => vec!["1".to_string()],
      "REF-CODE".to_string() => vec!["test".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :REF-CODE (
        #{"REF-CODE='test'"},
        %if (
          %check:exists (
            $.headers['REF-CODE'] => 'test'
          ) => BOOL(true),
          %match:equality (
            'test' => 'test',
            $.headers['REF-CODE'] => 'test',
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :REF-ID (
        #{'REF-ID must match by type and have at least 2 items'},
        %if (
          %check:exists (
            $.headers['REF-ID'] => '1'
          ) => BOOL(true),
          %match:min-type (
            ['1234', '4567'] => ['1234', '4567'],
            $.headers['REF-ID'] => '1',
            json:{"min":2} => json:{"min":2}
          ) => ERROR(Expected [1] (size 1) to have minimum size of 2)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
        ) => ['ref-code', 'ref-id'],
        $.headers => {'ref-code': 'test', 'ref-id': '1'},
        %join (
          'The following expected headers were missing: ',
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
fn match_content_type_header() {
  let expected = HttpRequest {
    headers: Some(hashmap!{
      "Content-Type".to_string() => vec![
        "application/hal+json".to_string()
      ]
    }),
    .. HttpRequest::default()
  };
  let mut context = PlanMatchingContext::default();
  let mut plan = ExecutionPlan::new("header-test");

  plan.add(setup_header_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/hal+json'"},
        %if (
          %check:exists (
            $.headers['Content-Type']
          ),
          %tee (
            %header:parse (
              $.headers['Content-Type']
            ),
            %match:equality (
              'application/hal+json',
              %to-string (
                ~>value
              ),
              NULL
            )
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['Content-Type']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "content-type".to_string() => vec!["application/hal+json;charset=UTF-8".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/hal+json'"},
        %if (
          %check:exists (
            $.headers['Content-Type'] => 'application/hal+json;charset=UTF-8'
          ) => BOOL(true),
          %tee (
            %header:parse (
              $.headers['Content-Type'] => 'application/hal+json;charset=UTF-8'
            ) => json:{"parameters":{"charset":"UTF-8"},"value":"application/hal+json"},
            %match:equality (
              'application/hal+json' => 'application/hal+json',
              %to-string (
                ~>value => json:"application/hal+json"
              ) => 'application/hal+json',
              NULL => NULL
            ) => BOOL(true)
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['Content-Type'] => ['Content-Type']
        ) => ['content-type'],
        $.headers => {'content-type': 'application/hal+json;charset=UTF-8'},
        %join (
          'The following expected headers were missing: ',
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

  let expected = HttpRequest {
    headers: Some(hashmap!{
      "Content-Type".to_string() => vec![
        "application/json;charset=UTF-8".to_string()
      ]
    }),
    .. HttpRequest::default()
  };
  let mut plan = ExecutionPlan::new("header-test");
  plan.add(setup_header_plan(&expected, &context.for_query()).unwrap());
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/json;charset=UTF-8'"},
        %if (
          %check:exists (
            $.headers['Content-Type']
          ),
          %tee (
            %header:parse (
              $.headers['Content-Type']
            ),
            %match:equality (
              'application/json',
              %to-string (
                ~>value
              ),
              NULL
            ),
            :charset (
              %if (
                %check:exists (
                  ~>parameters.charset
                ),
                %match:equality (
                  'utf-8',
                  %lower-case (
                    ~>parameters.charset
                  ),
                  NULL
                ),
                %error (
                  "Expected a charset value of 'UTF-8' but it was missing"
                )
              )
            )
          )
        )
      ),
      %expect:entries (
        %lower-case (
          ['Content-Type']
        ),
        $.headers,
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "content-type".to_string() => vec!["application/json; charset=UTF-8".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/json;charset=UTF-8'"},
        %if (
          %check:exists (
            $.headers['Content-Type'] => 'application/json; charset=UTF-8'
          ) => BOOL(true),
          %tee (
            %header:parse (
              $.headers['Content-Type'] => 'application/json; charset=UTF-8'
            ) => json:{"parameters":{"charset":"UTF-8"},"value":"application/json"},
            %match:equality (
              'application/json' => 'application/json',
              %to-string (
                ~>value => json:"application/json"
              ) => 'application/json',
              NULL => NULL
            ) => BOOL(true),
            :charset (
              %if (
                %check:exists (
                  ~>parameters.charset => json:"UTF-8"
                ) => BOOL(true),
                %match:equality (
                  'utf-8' => 'utf-8',
                  %lower-case (
                    ~>parameters.charset => json:"UTF-8"
                  ) => 'utf-8',
                  NULL => NULL
                ) => BOOL(true),
                %error (
                  "Expected a charset value of 'UTF-8' but it was missing"
                )
              ) => BOOL(true)
            ) => BOOL(true)
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      %expect:entries (
        %lower-case (
          ['Content-Type'] => ['Content-Type']
        ) => ['content-type'],
        $.headers => {'content-type': 'application/json; charset=UTF-8'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "content-type".to_string() => vec!["application/json".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/json;charset=UTF-8'"},
        %if (
          %check:exists (
            $.headers['Content-Type'] => 'application/json'
          ) => BOOL(true),
          %tee (
            %header:parse (
              $.headers['Content-Type'] => 'application/json'
            ) => json:{"parameters":{},"value":"application/json"},
            %match:equality (
              'application/json' => 'application/json',
              %to-string (
                ~>value => json:"application/json"
              ) => 'application/json',
              NULL => NULL
            ) => BOOL(true),
            :charset (
              %if (
                %check:exists (
                  ~>parameters.charset => NULL
                ) => BOOL(false),
                %match:equality (
                  'utf-8',
                  %lower-case (
                    ~>parameters.charset
                  ),
                  NULL
                ),
                %error (
                  "Expected a charset value of 'UTF-8' but it was missing" => "Expected a charset value of 'UTF-8' but it was missing"
                ) => ERROR(Expected a charset value of 'UTF-8' but it was missing)
              ) => ERROR(Expected a charset value of 'UTF-8' but it was missing)
            ) => BOOL(false)
          ) => BOOL(false)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['Content-Type'] => ['Content-Type']
        ) => ['content-type'],
        $.headers => {'content-type': 'application/json'},
        %join (
          'The following expected headers were missing: ',
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
    headers: Some(hashmap!{
      "content-type".to_string() => vec!["application/json;charset=UTF-16;other=stuff".to_string()]
    }),
    .. HttpRequest::default()
  };
  let executed_plan = execute_request_plan(&plan, &request, &mut context).unwrap();
  pretty_assertions::assert_eq!(r#"(
  :header-test (
    :headers (
      :Content-Type (
        #{"Content-Type='application/json;charset=UTF-8'"},
        %if (
          %check:exists (
            $.headers['Content-Type'] => 'application/json;charset=UTF-16;other=stuff'
          ) => BOOL(true),
          %tee (
            %header:parse (
              $.headers['Content-Type'] => 'application/json;charset=UTF-16;other=stuff'
            ) => json:{"parameters":{"charset":"UTF-16","other":"stuff"},"value":"application/json"},
            %match:equality (
              'application/json' => 'application/json',
              %to-string (
                ~>value => json:"application/json"
              ) => 'application/json',
              NULL => NULL
            ) => BOOL(true),
            :charset (
              %if (
                %check:exists (
                  ~>parameters.charset => json:"UTF-16"
                ) => BOOL(true),
                %match:equality (
                  'utf-8' => 'utf-8',
                  %lower-case (
                    ~>parameters.charset => json:"UTF-16"
                  ) => 'utf-16',
                  NULL => NULL
                ) => ERROR(Expected 'utf-16' to be equal to 'utf-8'),
                %error (
                  "Expected a charset value of 'UTF-8' but it was missing"
                )
              ) => BOOL(false)
            ) => BOOL(false)
          ) => BOOL(false)
        ) => BOOL(false)
      ) => BOOL(false),
      %expect:entries (
        %lower-case (
          ['Content-Type'] => ['Content-Type']
        ) => ['content-type'],
        $.headers => {'content-type': 'application/json;charset=UTF-16;other=stuff'},
        %join (
          'The following expected headers were missing: ',
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
