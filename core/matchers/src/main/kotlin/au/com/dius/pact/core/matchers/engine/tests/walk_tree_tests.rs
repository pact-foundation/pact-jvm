use bytes::Bytes;
use serde_json::{json, Value};
use tracing::trace;
use pact_models::path_exp::DocPath;
use pretty_assertions::assert_eq;

use crate::engine::bodies::{JsonPlanBuilder, PlanBodyBuilder, XMLPlanBuilder};
use crate::engine::context::PlanMatchingContext;
use crate::engine::NodeValue;
use crate::engine::value_resolvers::ValueResolver;
use crate::engine::interpreter::ExecutionPlanInterpreter;

struct TestValueResolver {
  pub bytes: Vec<u8>
}

impl ValueResolver for TestValueResolver {
  fn resolve(&self, path: &DocPath, _context: &PlanMatchingContext) -> anyhow::Result<NodeValue> {
    trace!(%path, "resolve called");
    Ok(NodeValue::BARRAY(self.bytes.clone()))
  }
}

#[test_log::test]
fn json_with_null() {
  let path = vec!["$".to_string()];
  let builder = JsonPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice(Value::Null.to_string().as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(4, bnVsbA==)
    ) => json:null,
    :$ (
      %match:equality (
        json:null => json:null,
        ~>$ => json:null,
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)", buffer);

  let content = Bytes::copy_from_slice(Value::Bool(true).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(4, dHJ1ZQ==)
    ) => json:true,
    :$ (
      %match:equality (
        json:null => json:null,
        ~>$ => json:true,
        NULL => NULL
      ) => ERROR(Expected true (Boolean) to be equal to null (Null))
    ) => BOOL(false)
  ) => BOOL(false)", buffer);

  let content = Bytes::copy_from_slice("{".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(1, ew==)
    ) => ERROR(json parse error - EOF while parsing an object at line 1 column 1),
    :$ (
      %match:equality (
        json:null,
        ~>$,
        NULL
      )
    )
  ) => ERROR(json parse error - EOF while parsing an object at line 1 column 1)", buffer);
}

#[test_log::test]
fn json_with_boolean() {
  let path = vec!["$".to_string()];
  let builder = JsonPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice(Value::Bool(true).to_string().as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(4, dHJ1ZQ==)
    ) => json:true,
    :$ (
      %match:equality (
        json:true => json:true,
        ~>$ => json:true,
        NULL => NULL
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)", buffer);

  let content = Bytes::copy_from_slice(Value::Bool(false).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(5, ZmFsc2U=)
    ) => json:false,
    :$ (
      %match:equality (
        json:true => json:true,
        ~>$ => json:false,
        NULL => NULL
      ) => ERROR(Expected false (Boolean) to be equal to true (Boolean))
    ) => BOOL(false)
  ) => BOOL(false)", buffer);
}

#[test_log::test]
fn json_with_empty_array() {
  let path = vec!["$".to_string()];
  let builder = JsonPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice(Value::Array(vec![]).to_string().as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(2, W10=)
    ) => json:[],
    :$ (
      %json:expect:empty (
        'ARRAY' => 'ARRAY',
        ~>$ => json:[]
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)", buffer);

  let content = Bytes::copy_from_slice(Value::Bool(false).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(5, ZmFsc2U=)
    ) => json:false,
    :$ (
      %json:expect:empty (
        'ARRAY' => 'ARRAY',
        ~>$ => json:false
      ) => ERROR(Was expecting a JSON Array but got a Boolean)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);

  let content = Bytes::copy_from_slice(Value::Array(vec![Value::Bool(true)]).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(6, W3RydWVd)
    ) => json:[true],
    :$ (
      %json:expect:empty (
        'ARRAY' => 'ARRAY',
        ~>$ => json:[true]
      ) => ERROR(Expected JSON Array ([true]) to be empty)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);
}

#[test_log::test]
fn json_with_array() {
  let path = vec!["$".to_string()];
  let builder = JsonPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice(json!([1, 2, 3]).to_string().as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(7, WzEsMiwzXQ==)
    ) => json:[1,2,3],
    :$ (
      %json:match:length (
        'ARRAY' => 'ARRAY',
        UINT(3) => UINT(3),
        ~>$ => json:[1,2,3]
      ) => BOOL(true),
      :$[0] (
        %if (
          %check:exists (
            ~>$[0] => json:1
          ) => BOOL(true),
          %match:equality (
            json:1 => json:1,
            ~>$[0] => json:1,
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :$[1] (
        %if (
          %check:exists (
            ~>$[1] => json:2
          ) => BOOL(true),
          %match:equality (
            json:2 => json:2,
            ~>$[1] => json:2,
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :$[2] (
        %if (
          %check:exists (
            ~>$[2] => json:3
          ) => BOOL(true),
          %match:equality (
            json:3 => json:3,
            ~>$[2] => json:3,
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)", buffer);

  let content = Bytes::copy_from_slice(Value::Bool(false).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(5, ZmFsc2U=)
    ) => json:false,
    :$ (
      %json:match:length (
        'ARRAY' => 'ARRAY',
        UINT(3) => UINT(3),
        ~>$ => json:false
      ) => ERROR(Was expecting a JSON Array but got a Boolean),
      :$[0] (
        %if (
          %check:exists (
            ~>$[0] => NULL
          ) => BOOL(false),
          %match:equality (
            json:1,
            ~>$[0],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      :$[1] (
        %if (
          %check:exists (
            ~>$[1] => NULL
          ) => BOOL(false),
          %match:equality (
            json:2,
            ~>$[1],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      :$[2] (
        %if (
          %check:exists (
            ~>$[2] => NULL
          ) => BOOL(false),
          %match:equality (
            json:3,
            ~>$[2],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);

  let content = Bytes::copy_from_slice(Value::Array(vec![Value::Bool(true)]).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(6, W3RydWVd)
    ) => json:[true],
    :$ (
      %json:match:length (
        'ARRAY' => 'ARRAY',
        UINT(3) => UINT(3),
        ~>$ => json:[true]
      ) => ERROR(Was expecting a length of 3, but actual length is 1),
      :$[0] (
        %if (
          %check:exists (
            ~>$[0] => json:true
          ) => BOOL(true),
          %match:equality (
            json:1 => json:1,
            ~>$[0] => json:true,
            NULL => NULL
          ) => ERROR(Expected true (Boolean) to be equal to 1 (Integer))
        ) => BOOL(false)
      ) => BOOL(false),
      :$[1] (
        %if (
          %check:exists (
            ~>$[1] => NULL
          ) => BOOL(false),
          %match:equality (
            json:2,
            ~>$[1],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false),
      :$[2] (
        %if (
          %check:exists (
            ~>$[2] => NULL
          ) => BOOL(false),
          %match:equality (
            json:3,
            ~>$[2],
            NULL
          )
        ) => BOOL(false)
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);

  let content = Bytes::copy_from_slice(json!([1, 3, 3]).to_string().as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %json:parse (
      $.body => BYTES(7, WzEsMywzXQ==)
    ) => json:[1,3,3],
    :$ (
      %json:match:length (
        'ARRAY' => 'ARRAY',
        UINT(3) => UINT(3),
        ~>$ => json:[1,3,3]
      ) => BOOL(true),
      :$[0] (
        %if (
          %check:exists (
            ~>$[0] => json:1
          ) => BOOL(true),
          %match:equality (
            json:1 => json:1,
            ~>$[0] => json:1,
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true),
      :$[1] (
        %if (
          %check:exists (
            ~>$[1] => json:3
          ) => BOOL(true),
          %match:equality (
            json:2 => json:2,
            ~>$[1] => json:3,
            NULL => NULL
          ) => ERROR(Expected 3 (Integer) to be equal to 2 (Integer))
        ) => BOOL(false)
      ) => BOOL(false),
      :$[2] (
        %if (
          %check:exists (
            ~>$[2] => json:3
          ) => BOOL(true),
          %match:equality (
            json:3 => json:3,
            ~>$[2] => json:3,
            NULL => NULL
          ) => BOOL(true)
        ) => BOOL(true)
      ) => BOOL(true)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);
}

#[test_log::test]
fn very_simple_xml() {
  let path = vec!["$".to_string()];
  let builder = XMLPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice("<foo>test</foo>".as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %xml:parse (
      $.body => BYTES(15, PGZvbz50ZXN0PC9mb28+)
    ) => xml:'<foo>test</foo>',
    :$ (
      %if (
        %check:exists (
          ~>$.foo => xml:'<foo>test</foo>'
        ) => BOOL(true),
        :$.foo (
          :#text (
            %match:equality (
              'test' => 'test',
              %to-string (
                ~>$.foo['#text'] => xml:text:test
              ) => 'test',
              NULL => NULL
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:empty (
            ~>$.foo => xml:'<foo>test</foo>'
          ) => BOOL(true)
        ) => BOOL(true),
        %error (
          'Was expecting an XML element /foo but it was missing'
        )
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)", buffer);

  let content = Bytes::copy_from_slice("<bar></bar>".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %xml:parse (
      $.body => BYTES(11, PGJhcj48L2Jhcj4=)
    ) => xml:'<bar/>',
    :$ (
      %if (
        %check:exists (
          ~>$.foo => NULL
        ) => BOOL(false),
        :$.foo (
          :#text (
            %match:equality (
              'test',
              %to-string (
                ~>$.foo['#text']
              ),
              NULL
            )
          ),
          %expect:empty (
            ~>$.foo
          )
        ),
        %error (
          'Was expecting an XML element /foo but it was missing' => 'Was expecting an XML element /foo but it was missing'
        ) => ERROR(Was expecting an XML element /foo but it was missing)
      ) => ERROR(Was expecting an XML element /foo but it was missing)
    ) => BOOL(false)
  ) => BOOL(false)", buffer);

  let content = Bytes::copy_from_slice("<foo>test".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!("  %tee (
    %xml:parse (
      $.body => BYTES(9, PGZvbz50ZXN0)
    ) => ERROR(XML parse error - ParsingError: root element not closed),
    :$ (
      %if (
        %check:exists (
          ~>$.foo
        ),
        :$.foo (
          :#text (
            %match:equality (
              'test',
              %to-string (
                ~>$.foo['#text']
              ),
              NULL
            )
          ),
          %expect:empty (
            ~>$.foo
          )
        ),
        %error (
          'Was expecting an XML element /foo but it was missing'
        )
      )
    )
  ) => ERROR(XML parse error - ParsingError: root element not closed)", buffer);
}

#[test_log::test]
fn simple_xml() {
  let path = vec!["$".to_string()];
  let context = PlanMatchingContext::default();
  let builder = XMLPlanBuilder::new();
  let content = Bytes::copy_from_slice(r#"<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name>My Settings</name>
        <sound>
          <property name="volume" value="11" />
          <property name="mixer" value="standard" />
        </sound>
      </config>
  "#.as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(239, PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KICAgICAgPGNvbmZpZz4KICAgICAgICA8bmFtZT5NeSBTZXR0aW5nczwvbmFtZT4KICAgICAgICA8c291bmQ+CiAgICAgICAgICA8cHJvcGVydHkgbmFtZT0idm9sdW1lIiB2YWx1ZT0iMTEiIC8+CiAgICAgICAgICA8cHJvcGVydHkgbmFtZT0ibWl4ZXIiIHZhbHVlPSJzdGFuZGFyZCIgLz4KICAgICAgICA8L3NvdW5kPgogICAgICA8L2NvbmZpZz4KICA=)
    ) => xml:"<config>\n  <name>My Settings</name>\n  <sound>\n    <property name=\"volume\" value=\"11\"/>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>",
    :$ (
      %if (
        %check:exists (
          ~>$.config => xml:"<config>\n  <name>My Settings</name>\n  <sound>\n    <property name=\"volume\" value=\"11\"/>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>"
        ) => BOOL(true),
        :$.config (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.config['#text'] => NULL
              ) => ''
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:only-entries (
            ['name', 'sound'] => ['name', 'sound'],
            ~>$.config => xml:"<config>\n  <name>My Settings</name>\n  <sound>\n    <property name=\"volume\" value=\"11\"/>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>"
          ) => OK,
          %expect:count (
            UINT(1) => UINT(1),
            ~>$.config.name => xml:'<name>My Settings</name>',
            %join (
              'Expected 1 <name> child element but there were ',
              %length (
                ~>$.config.name
              )
            )
          ) => OK,
          %if (
            %check:exists (
              ~>$.config.name[0] => xml:'<name>My Settings</name>'
            ) => BOOL(true),
            :$.config.name[0] (
              :#text (
                %match:equality (
                  'My Settings' => 'My Settings',
                  %to-string (
                    ~>$.config.name[0]['#text'] => xml:text:'My Settings'
                  ) => 'My Settings',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.config.name[0] => xml:'<name>My Settings</name>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /config/name/0 but it was missing'
            )
          ) => BOOL(true),
          %expect:count (
            UINT(1) => UINT(1),
            ~>$.config.sound => xml:"<sound>\n  <property name=\"volume\" value=\"11\"/>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>",
            %join (
              'Expected 1 <sound> child element but there were ',
              %length (
                ~>$.config.sound
              )
            )
          ) => OK,
          %if (
            %check:exists (
              ~>$.config.sound[0] => xml:"<sound>\n  <property name=\"volume\" value=\"11\"/>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>"
            ) => BOOL(true),
            :$.config.sound[0] (
              :#text (
                %expect:empty (
                  %to-string (
                    ~>$.config.sound[0]['#text'] => NULL
                  ) => ''
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:only-entries (
                ['property'] => ['property'],
                ~>$.config.sound[0] => xml:"<sound>\n  <property name=\"volume\" value=\"11\"/>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>"
              ) => OK,
              %expect:count (
                UINT(2) => UINT(2),
                ~>$.config.sound[0].property => [xml:'<property name="volume" value="11"/>', xml:'<property name="mixer" value="standard"/>'],
                %join (
                  'Expected 2 <property> child elements but there were ',
                  %length (
                    ~>$.config.sound[0].property
                  )
                )
              ) => OK,
              %if (
                %check:exists (
                  ~>$.config.sound[0].property[0] => xml:'<property name="volume" value="11"/>'
                ) => BOOL(true),
                :$.config.sound[0].property[0] (
                  :attributes (
                    :$.config.sound[0].property[0]['@name'] (
                      #{"@name='volume'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[0]['@name'] => xml:attribute:name=volume
                        ) => BOOL(true),
                        %match:equality (
                          'volume' => 'volume',
                          %xml:value (
                            ~>$.config.sound[0].property[0]['@name'] => xml:attribute:name=volume
                          ) => 'volume',
                          NULL => NULL
                        ) => BOOL(true)
                      ) => BOOL(true)
                    ) => BOOL(true),
                    :$.config.sound[0].property[0]['@value'] (
                      #{"@value='11'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[0]['@value'] => xml:attribute:value=11
                        ) => BOOL(true),
                        %match:equality (
                          '11' => '11',
                          %xml:value (
                            ~>$.config.sound[0].property[0]['@value'] => xml:attribute:value=11
                          ) => '11',
                          NULL => NULL
                        ) => BOOL(true)
                      ) => BOOL(true)
                    ) => BOOL(true),
                    %expect:entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[0] => xml:'<property name="volume" value="11"/>'
                      ) => {'name': 'volume', 'value': '11'},
                      %join (
                        'The following expected attributes were missing: ',
                        %join-with (
                          ', ',
                          ** (
                            %apply ()
                          )
                        )
                      )
                    ) => OK,
                    %expect:only-entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[0] => xml:'<property name="volume" value="11"/>'
                      ) => {'name': 'volume', 'value': '11'}
                    ) => OK
                  ) => BOOL(true),
                  :#text (
                    %expect:empty (
                      %to-string (
                        ~>$.config.sound[0].property[0]['#text'] => NULL
                      ) => ''
                    ) => BOOL(true)
                  ) => BOOL(true),
                  %expect:empty (
                    ~>$.config.sound[0].property[0] => xml:'<property name="volume" value="11"/>'
                  ) => BOOL(true)
                ) => BOOL(true),
                %error (
                  'Was expecting an XML element /config/sound/0/property/0 but it was missing'
                )
              ) => BOOL(true),
              %if (
                %check:exists (
                  ~>$.config.sound[0].property[1] => xml:'<property name="mixer" value="standard"/>'
                ) => BOOL(true),
                :$.config.sound[0].property[1] (
                  :attributes (
                    :$.config.sound[0].property[1]['@name'] (
                      #{"@name='mixer'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[1]['@name'] => xml:attribute:name=mixer
                        ) => BOOL(true),
                        %match:equality (
                          'mixer' => 'mixer',
                          %xml:value (
                            ~>$.config.sound[0].property[1]['@name'] => xml:attribute:name=mixer
                          ) => 'mixer',
                          NULL => NULL
                        ) => BOOL(true)
                      ) => BOOL(true)
                    ) => BOOL(true),
                    :$.config.sound[0].property[1]['@value'] (
                      #{"@value='standard'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[1]['@value'] => xml:attribute:value=standard
                        ) => BOOL(true),
                        %match:equality (
                          'standard' => 'standard',
                          %xml:value (
                            ~>$.config.sound[0].property[1]['@value'] => xml:attribute:value=standard
                          ) => 'standard',
                          NULL => NULL
                        ) => BOOL(true)
                      ) => BOOL(true)
                    ) => BOOL(true),
                    %expect:entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[1] => xml:'<property name="mixer" value="standard"/>'
                      ) => {'name': 'mixer', 'value': 'standard'},
                      %join (
                        'The following expected attributes were missing: ',
                        %join-with (
                          ', ',
                          ** (
                            %apply ()
                          )
                        )
                      )
                    ) => OK,
                    %expect:only-entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[1] => xml:'<property name="mixer" value="standard"/>'
                      ) => {'name': 'mixer', 'value': 'standard'}
                    ) => OK
                  ) => BOOL(true),
                  :#text (
                    %expect:empty (
                      %to-string (
                        ~>$.config.sound[0].property[1]['#text'] => NULL
                      ) => ''
                    ) => BOOL(true)
                  ) => BOOL(true),
                  %expect:empty (
                    ~>$.config.sound[0].property[1] => xml:'<property name="mixer" value="standard"/>'
                  ) => BOOL(true)
                ) => BOOL(true),
                %error (
                  'Was expecting an XML element /config/sound/0/property/1 but it was missing'
                )
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /config/sound/0 but it was missing'
            )
          ) => BOOL(true)
        ) => BOOL(true),
        %error (
          'Was expecting an XML element /config but it was missing'
        )
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)"#, buffer);

  let content = Bytes::copy_from_slice(r#"<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name/>
        <sound>
          <property name="mixer" value="standard" />
        </sound>
      </config>
  "#.as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(174, PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KICAgICAgPGNvbmZpZz4KICAgICAgICA8bmFtZS8+CiAgICAgICAgPHNvdW5kPgogICAgICAgICAgPHByb3BlcnR5IG5hbWU9Im1peGVyIiB2YWx1ZT0ic3RhbmRhcmQiIC8+CiAgICAgICAgPC9zb3VuZD4KICAgICAgPC9jb25maWc+CiAg)
    ) => xml:"<config>\n  <name/>\n  <sound>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>",
    :$ (
      %if (
        %check:exists (
          ~>$.config => xml:"<config>\n  <name/>\n  <sound>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>"
        ) => BOOL(true),
        :$.config (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.config['#text'] => NULL
              ) => ''
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:only-entries (
            ['name', 'sound'] => ['name', 'sound'],
            ~>$.config => xml:"<config>\n  <name/>\n  <sound>\n    <property name=\"mixer\" value=\"standard\"/>\n  </sound>\n</config>"
          ) => OK,
          %expect:count (
            UINT(1) => UINT(1),
            ~>$.config.name => xml:'<name/>',
            %join (
              'Expected 1 <name> child element but there were ',
              %length (
                ~>$.config.name
              )
            )
          ) => OK,
          %if (
            %check:exists (
              ~>$.config.name[0] => xml:'<name/>'
            ) => BOOL(true),
            :$.config.name[0] (
              :#text (
                %match:equality (
                  'My Settings' => 'My Settings',
                  %to-string (
                    ~>$.config.name[0]['#text'] => NULL
                  ) => '',
                  NULL => NULL
                ) => ERROR(Expected '' to be equal to 'My Settings')
              ) => BOOL(false),
              %expect:empty (
                ~>$.config.name[0] => xml:'<name/>'
              ) => BOOL(true)
            ) => BOOL(false),
            %error (
              'Was expecting an XML element /config/name/0 but it was missing'
            )
          ) => BOOL(false),
          %expect:count (
            UINT(1) => UINT(1),
            ~>$.config.sound => xml:"<sound>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>",
            %join (
              'Expected 1 <sound> child element but there were ',
              %length (
                ~>$.config.sound
              )
            )
          ) => OK,
          %if (
            %check:exists (
              ~>$.config.sound[0] => xml:"<sound>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>"
            ) => BOOL(true),
            :$.config.sound[0] (
              :#text (
                %expect:empty (
                  %to-string (
                    ~>$.config.sound[0]['#text'] => NULL
                  ) => ''
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:only-entries (
                ['property'] => ['property'],
                ~>$.config.sound[0] => xml:"<sound>\n  <property name=\"mixer\" value=\"standard\"/>\n</sound>"
              ) => OK,
              %expect:count (
                UINT(2) => UINT(2),
                ~>$.config.sound[0].property => xml:'<property name="mixer" value="standard"/>',
                %join (
                  'Expected 2 <property> child elements but there were ' => 'Expected 2 <property> child elements but there were ',
                  %length (
                    ~>$.config.sound[0].property => xml:'<property name="mixer" value="standard"/>'
                  ) => UINT(1)
                ) => 'Expected 2 <property> child elements but there were 1'
              ) => ERROR(Expected 2 <property> child elements but there were 1),
              %if (
                %check:exists (
                  ~>$.config.sound[0].property[0] => xml:'<property name="mixer" value="standard"/>'
                ) => BOOL(true),
                :$.config.sound[0].property[0] (
                  :attributes (
                    :$.config.sound[0].property[0]['@name'] (
                      #{"@name='volume'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[0]['@name'] => xml:attribute:name=mixer
                        ) => BOOL(true),
                        %match:equality (
                          'volume' => 'volume',
                          %xml:value (
                            ~>$.config.sound[0].property[0]['@name'] => xml:attribute:name=mixer
                          ) => 'mixer',
                          NULL => NULL
                        ) => ERROR(Expected 'mixer' to be equal to 'volume')
                      ) => BOOL(false)
                    ) => BOOL(false),
                    :$.config.sound[0].property[0]['@value'] (
                      #{"@value='11'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[0]['@value'] => xml:attribute:value=standard
                        ) => BOOL(true),
                        %match:equality (
                          '11' => '11',
                          %xml:value (
                            ~>$.config.sound[0].property[0]['@value'] => xml:attribute:value=standard
                          ) => 'standard',
                          NULL => NULL
                        ) => ERROR(Expected 'standard' to be equal to '11')
                      ) => BOOL(false)
                    ) => BOOL(false),
                    %expect:entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[0] => xml:'<property name="mixer" value="standard"/>'
                      ) => {'name': 'mixer', 'value': 'standard'},
                      %join (
                        'The following expected attributes were missing: ',
                        %join-with (
                          ', ',
                          ** (
                            %apply ()
                          )
                        )
                      )
                    ) => OK,
                    %expect:only-entries (
                      ['name', 'value'] => ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[0] => xml:'<property name="mixer" value="standard"/>'
                      ) => {'name': 'mixer', 'value': 'standard'}
                    ) => OK
                  ) => BOOL(false),
                  :#text (
                    %expect:empty (
                      %to-string (
                        ~>$.config.sound[0].property[0]['#text'] => NULL
                      ) => ''
                    ) => BOOL(true)
                  ) => BOOL(true),
                  %expect:empty (
                    ~>$.config.sound[0].property[0] => xml:'<property name="mixer" value="standard"/>'
                  ) => BOOL(true)
                ) => BOOL(false),
                %error (
                  'Was expecting an XML element /config/sound/0/property/0 but it was missing'
                )
              ) => BOOL(false),
              %if (
                %check:exists (
                  ~>$.config.sound[0].property[1] => NULL
                ) => BOOL(false),
                :$.config.sound[0].property[1] (
                  :attributes (
                    :$.config.sound[0].property[1]['@name'] (
                      #{"@name='mixer'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[1]['@name']
                        ),
                        %match:equality (
                          'mixer',
                          %xml:value (
                            ~>$.config.sound[0].property[1]['@name']
                          ),
                          NULL
                        )
                      )
                    ),
                    :$.config.sound[0].property[1]['@value'] (
                      #{"@value='standard'"},
                      %if (
                        %check:exists (
                          ~>$.config.sound[0].property[1]['@value']
                        ),
                        %match:equality (
                          'standard',
                          %xml:value (
                            ~>$.config.sound[0].property[1]['@value']
                          ),
                          NULL
                        )
                      )
                    ),
                    %expect:entries (
                      ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[1]
                      ),
                      %join (
                        'The following expected attributes were missing: ',
                        %join-with (
                          ', ',
                          ** (
                            %apply ()
                          )
                        )
                      )
                    ),
                    %expect:only-entries (
                      ['name', 'value'],
                      %xml:attributes (
                        ~>$.config.sound[0].property[1]
                      )
                    )
                  ),
                  :#text (
                    %expect:empty (
                      %to-string (
                        ~>$.config.sound[0].property[1]['#text']
                      )
                    )
                  ),
                  %expect:empty (
                    ~>$.config.sound[0].property[1]
                  )
                ),
                %error (
                  'Was expecting an XML element /config/sound/0/property/1 but it was missing' => 'Was expecting an XML element /config/sound/0/property/1 but it was missing'
                ) => ERROR(Was expecting an XML element /config/sound/0/property/1 but it was missing)
              ) => ERROR(Was expecting an XML element /config/sound/0/property/1 but it was missing)
            ) => BOOL(false),
            %error (
              'Was expecting an XML element /config/sound/0 but it was missing'
            )
          ) => BOOL(false)
        ) => BOOL(false),
        %error (
          'Was expecting an XML element /config but it was missing'
        )
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)"#, buffer);
}

#[test_log::test]
fn missing_xml_value() {
  let path = vec!["$".to_string()];
  let builder = XMLPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice("<values><value>A</value><value>B</value></values>".as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(49, PHZhbHVlcz48dmFsdWU+QTwvdmFsdWU+PHZhbHVlPkI8L3ZhbHVlPjwvdmFsdWVzPg==)
    ) => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n</values>",
    :$ (
      %if (
        %check:exists (
          ~>$.values => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n</values>"
        ) => BOOL(true),
        :$.values (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.values['#text'] => NULL
              ) => ''
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:only-entries (
            ['value'] => ['value'],
            ~>$.values => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n</values>"
          ) => OK,
          %expect:count (
            UINT(2) => UINT(2),
            ~>$.values.value => [xml:'<value>A</value>', xml:'<value>B</value>'],
            %join (
              'Expected 2 <value> child elements but there were ',
              %length (
                ~>$.values.value
              )
            )
          ) => OK,
          %if (
            %check:exists (
              ~>$.values.value[0] => xml:'<value>A</value>'
            ) => BOOL(true),
            :$.values.value[0] (
              :#text (
                %match:equality (
                  'A' => 'A',
                  %to-string (
                    ~>$.values.value[0]['#text'] => xml:text:A
                  ) => 'A',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.values.value[0] => xml:'<value>A</value>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /values/value/0 but it was missing'
            )
          ) => BOOL(true),
          %if (
            %check:exists (
              ~>$.values.value[1] => xml:'<value>B</value>'
            ) => BOOL(true),
            :$.values.value[1] (
              :#text (
                %match:equality (
                  'B' => 'B',
                  %to-string (
                    ~>$.values.value[1]['#text'] => xml:text:B
                  ) => 'B',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.values.value[1] => xml:'<value>B</value>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /values/value/1 but it was missing'
            )
          ) => BOOL(true)
        ) => BOOL(true),
        %error (
          'Was expecting an XML element /values but it was missing'
        )
      ) => BOOL(true)
    ) => BOOL(true)
  ) => BOOL(true)"#, buffer);

  let content = Bytes::copy_from_slice("<bar></bar>".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(11, PGJhcj48L2Jhcj4=)
    ) => xml:'<bar/>',
    :$ (
      %if (
        %check:exists (
          ~>$.values => NULL
        ) => BOOL(false),
        :$.values (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.values['#text']
              )
            )
          ),
          %expect:only-entries (
            ['value'],
            ~>$.values
          ),
          %expect:count (
            UINT(2),
            ~>$.values.value,
            %join (
              'Expected 2 <value> child elements but there were ',
              %length (
                ~>$.values.value
              )
            )
          ),
          %if (
            %check:exists (
              ~>$.values.value[0]
            ),
            :$.values.value[0] (
              :#text (
                %match:equality (
                  'A',
                  %to-string (
                    ~>$.values.value[0]['#text']
                  ),
                  NULL
                )
              ),
              %expect:empty (
                ~>$.values.value[0]
              )
            ),
            %error (
              'Was expecting an XML element /values/value/0 but it was missing'
            )
          ),
          %if (
            %check:exists (
              ~>$.values.value[1]
            ),
            :$.values.value[1] (
              :#text (
                %match:equality (
                  'B',
                  %to-string (
                    ~>$.values.value[1]['#text']
                  ),
                  NULL
                )
              ),
              %expect:empty (
                ~>$.values.value[1]
              )
            ),
            %error (
              'Was expecting an XML element /values/value/1 but it was missing'
            )
          )
        ),
        %error (
          'Was expecting an XML element /values but it was missing' => 'Was expecting an XML element /values but it was missing'
        ) => ERROR(Was expecting an XML element /values but it was missing)
      ) => ERROR(Was expecting an XML element /values but it was missing)
    ) => BOOL(false)
  ) => BOOL(false)"#, buffer);

  let content = Bytes::copy_from_slice("<values><value>A</value></values>".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(33, PHZhbHVlcz48dmFsdWU+QTwvdmFsdWU+PC92YWx1ZXM+)
    ) => xml:"<values>\n  <value>A</value>\n</values>",
    :$ (
      %if (
        %check:exists (
          ~>$.values => xml:"<values>\n  <value>A</value>\n</values>"
        ) => BOOL(true),
        :$.values (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.values['#text'] => NULL
              ) => ''
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:only-entries (
            ['value'] => ['value'],
            ~>$.values => xml:"<values>\n  <value>A</value>\n</values>"
          ) => OK,
          %expect:count (
            UINT(2) => UINT(2),
            ~>$.values.value => xml:'<value>A</value>',
            %join (
              'Expected 2 <value> child elements but there were ' => 'Expected 2 <value> child elements but there were ',
              %length (
                ~>$.values.value => xml:'<value>A</value>'
              ) => UINT(1)
            ) => 'Expected 2 <value> child elements but there were 1'
          ) => ERROR(Expected 2 <value> child elements but there were 1),
          %if (
            %check:exists (
              ~>$.values.value[0] => xml:'<value>A</value>'
            ) => BOOL(true),
            :$.values.value[0] (
              :#text (
                %match:equality (
                  'A' => 'A',
                  %to-string (
                    ~>$.values.value[0]['#text'] => xml:text:A
                  ) => 'A',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.values.value[0] => xml:'<value>A</value>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /values/value/0 but it was missing'
            )
          ) => BOOL(true),
          %if (
            %check:exists (
              ~>$.values.value[1] => NULL
            ) => BOOL(false),
            :$.values.value[1] (
              :#text (
                %match:equality (
                  'B',
                  %to-string (
                    ~>$.values.value[1]['#text']
                  ),
                  NULL
                )
              ),
              %expect:empty (
                ~>$.values.value[1]
              )
            ),
            %error (
              'Was expecting an XML element /values/value/1 but it was missing' => 'Was expecting an XML element /values/value/1 but it was missing'
            ) => ERROR(Was expecting an XML element /values/value/1 but it was missing)
          ) => ERROR(Was expecting an XML element /values/value/1 but it was missing)
        ) => BOOL(false),
        %error (
          'Was expecting an XML element /values but it was missing'
        )
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)"#, buffer);
}

#[test_log::test]
fn invalid_xml() {
  let path = vec!["$".to_string()];
  let builder = XMLPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice("<values><value>A</value><value>B</value></values>".as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let content = Bytes::copy_from_slice("<foo>test".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(9, PGZvbz50ZXN0)
    ) => ERROR(XML parse error - ParsingError: root element not closed),
    :$ (
      %if (
        %check:exists (
          ~>$.values
        ),
        :$.values (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.values['#text']
              )
            )
          ),
          %expect:only-entries (
            ['value'],
            ~>$.values
          ),
          %expect:count (
            UINT(2),
            ~>$.values.value,
            %join (
              'Expected 2 <value> child elements but there were ',
              %length (
                ~>$.values.value
              )
            )
          ),
          %if (
            %check:exists (
              ~>$.values.value[0]
            ),
            :$.values.value[0] (
              :#text (
                %match:equality (
                  'A',
                  %to-string (
                    ~>$.values.value[0]['#text']
                  ),
                  NULL
                )
              ),
              %expect:empty (
                ~>$.values.value[0]
              )
            ),
            %error (
              'Was expecting an XML element /values/value/0 but it was missing'
            )
          ),
          %if (
            %check:exists (
              ~>$.values.value[1]
            ),
            :$.values.value[1] (
              :#text (
                %match:equality (
                  'B',
                  %to-string (
                    ~>$.values.value[1]['#text']
                  ),
                  NULL
                )
              ),
              %expect:empty (
                ~>$.values.value[1]
              )
            ),
            %error (
              'Was expecting an XML element /values/value/1 but it was missing'
            )
          )
        ),
        %error (
          'Was expecting an XML element /values but it was missing'
        )
      )
    )
  ) => ERROR(XML parse error - ParsingError: root element not closed)"#, buffer);
}

#[test_log::test]
fn unexpected_xml_value() {
  let path = vec!["$".to_string()];
  let builder = XMLPlanBuilder::new();
  let context = PlanMatchingContext::default();
  let content = Bytes::copy_from_slice("<values><value>A</value><value>B</value></values>".as_bytes());
  let node = builder.build_plan(&content, &context).unwrap();

  let content = Bytes::copy_from_slice("<values><value>A</value><value>B</value><value>C</value></values>".as_bytes());
  let resolver = TestValueResolver {
    bytes: content.to_vec()
  };
  let mut interpreter = ExecutionPlanInterpreter::new_with_context(&context);
  let result = interpreter.walk_tree(&path, &node, &resolver).unwrap();
  let mut buffer = String::new();
  result.pretty_form(&mut buffer, 2);
  assert_eq!(r#"  %tee (
    %xml:parse (
      $.body => BYTES(65, PHZhbHVlcz48dmFsdWU+QTwvdmFsdWU+PHZhbHVlPkI8L3ZhbHVlPjx2YWx1ZT5DPC92YWx1ZT48L3ZhbHVlcz4=)
    ) => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n  <value>C</value>\n</values>",
    :$ (
      %if (
        %check:exists (
          ~>$.values => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n  <value>C</value>\n</values>"
        ) => BOOL(true),
        :$.values (
          :#text (
            %expect:empty (
              %to-string (
                ~>$.values['#text'] => NULL
              ) => ''
            ) => BOOL(true)
          ) => BOOL(true),
          %expect:only-entries (
            ['value'] => ['value'],
            ~>$.values => xml:"<values>\n  <value>A</value>\n  <value>B</value>\n  <value>C</value>\n</values>"
          ) => OK,
          %expect:count (
            UINT(2) => UINT(2),
            ~>$.values.value => [xml:'<value>A</value>', xml:'<value>B</value>', xml:'<value>C</value>'],
            %join (
              'Expected 2 <value> child elements but there were ' => 'Expected 2 <value> child elements but there were ',
              %length (
                ~>$.values.value => [xml:'<value>A</value>', xml:'<value>B</value>', xml:'<value>C</value>']
              ) => UINT(3)
            ) => 'Expected 2 <value> child elements but there were 3'
          ) => ERROR(Expected 2 <value> child elements but there were 3),
          %if (
            %check:exists (
              ~>$.values.value[0] => xml:'<value>A</value>'
            ) => BOOL(true),
            :$.values.value[0] (
              :#text (
                %match:equality (
                  'A' => 'A',
                  %to-string (
                    ~>$.values.value[0]['#text'] => xml:text:A
                  ) => 'A',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.values.value[0] => xml:'<value>A</value>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /values/value/0 but it was missing'
            )
          ) => BOOL(true),
          %if (
            %check:exists (
              ~>$.values.value[1] => xml:'<value>B</value>'
            ) => BOOL(true),
            :$.values.value[1] (
              :#text (
                %match:equality (
                  'B' => 'B',
                  %to-string (
                    ~>$.values.value[1]['#text'] => xml:text:B
                  ) => 'B',
                  NULL => NULL
                ) => BOOL(true)
              ) => BOOL(true),
              %expect:empty (
                ~>$.values.value[1] => xml:'<value>B</value>'
              ) => BOOL(true)
            ) => BOOL(true),
            %error (
              'Was expecting an XML element /values/value/1 but it was missing'
            )
          ) => BOOL(true)
        ) => BOOL(false),
        %error (
          'Was expecting an XML element /values but it was missing'
        )
      ) => BOOL(false)
    ) => BOOL(false)
  ) => BOOL(false)"#, buffer);
}
