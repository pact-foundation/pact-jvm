package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import spock.lang.Specification

class MatchingEngineSpec extends Specification {

  def 'simple match request test'() {
    given:
    def request = new HttpRequest('put', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))
    def expectedRequest = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def matchingRules = new MatchingRulesImpl()
    def config = new MatchingConfiguration()
    def context = new PlanMatchingContext(pact, interaction, matchingRules, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildRequestPlan(expectedRequest, context).unwrap()

    then:
    //  assert_eq!(r#"(
    //  :request (
    //    :method (
    //      #{'method == POST'},
    //      %match:equality (
    //        'POST',
    //        %upper-case (
    //          $.method
    //        ),
    //        NULL
    //      )
    //    ),
    //    :path (
    //      #{"path == '/test'"},
    //      %match:equality (
    //        '/test',
    //        $.path,
    //        NULL
    //      )
    //    ),
    //    :"query parameters" (
    //      %expect:empty (
    //        $.query,
    //        %join (
    //          'Expected no query parameters but got ',
    //          $.query
    //        )
    //      )
    //    ),
    //    :body (
    //      %if (
    //        %match:equality (
    //          'text/plain',
    //          $.content-type,
    //          NULL,
    //          %error (
    //            'Body type error - ',
    //            %apply ()
    //          )
    //        ),
    //        %match:equality (
    //          'Some nice bit of text',
    //          %convert:UTF8 (
    //            $.body
    //          ),
    //          NULL
    //        )
    //      )
    //    )
    //  )
    //)
    //"#, plan.pretty_form());
    //
    //  let executed_plan = execute_request_plan(&plan, &request, &mut context)?;
    //  assert_eq!(r#"(
    //  :request (
    //    :method (
    //      #{'method == POST'},
    //      %match:equality (
    //        'POST' => 'POST',
    //        %upper-case (
    //          $.method => 'put'
    //        ) => 'PUT',
    //        NULL => NULL
    //      ) => ERROR(Expected 'PUT' to be equal to 'POST')
    //    ) => BOOL(false),
    //    :path (
    //      #{"path == '/test'"},
    //      %match:equality (
    //        '/test' => '/test',
    //        $.path => '/test',
    //        NULL => NULL
    //      ) => BOOL(true)
    //    ) => BOOL(true),
    //    :"query parameters" (
    //      %expect:empty (
    //        $.query => {},
    //        %join (
    //          'Expected no query parameters but got ',
    //          $.query
    //        )
    //      ) => BOOL(true)
    //    ) => BOOL(true),
    //    :body (
    //      %if (
    //        %match:equality (
    //          'text/plain' => 'text/plain',
    //          $.content-type => 'text/plain',
    //          NULL => NULL,
    //          %error (
    //            'Body type error - ',
    //            %apply ()
    //          )
    //        ) => BOOL(true),
    //        %match:equality (
    //          'Some nice bit of text' => 'Some nice bit of text',
    //          %convert:UTF8 (
    //            $.body => BYTES(21, U29tZSBuaWNlIGJpdCBvZiB0ZXh0)
    //          ) => 'Some nice bit of text',
    //          NULL => NULL
    //        ) => BOOL(true)
    //      ) => BOOL(true)
    //    ) => BOOL(true)
    //  ) => BOOL(false)
    //)
    //"#, executed_plan.pretty_form());
    //
    //  assert_eq!(r#"request:
    //  method: method == POST - ERROR Expected 'PUT' to be equal to 'POST'
    //  path: path == '/test' - OK
    //  query parameters: - OK
    //  body: - OK
    //"#, executed_plan.generate_summary(false));
    //
    //  let mismatches: RequestMatchResult = executed_plan.into();
    //  assert_eq!(RequestMatchResult {
    //    method: Some(MethodMismatch {
    //      expected: "".to_string(),
    //      actual: "".to_string(),
    //      mismatch: "Expected 'PUT' to be equal to 'POST'".to_string()
    //    }),
    //    path: None,
    //    headers: hashmap!{},
    //    query: hashmap!{},
    //    body: BodyMatchResult::Ok,
    //  }, mismatches);
    false
  }
}
