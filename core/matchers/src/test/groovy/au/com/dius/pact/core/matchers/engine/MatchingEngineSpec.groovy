package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.BodyMatchResult
import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.MethodMismatch
import au.com.dius.pact.core.matchers.RequestMatchResult
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import com.github.difflib.DiffUtils
import spock.lang.Ignore
import spock.lang.Specification

import static com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff

@SuppressWarnings('MethodSize')
class MatchingEngineSpec extends Specification {

  def 'simple match request test'() {
    given:
    def request = new HttpRequest('put', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))
    def expectedRequest = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def matchingRules = new MatchingRuleCategory('test')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, new MatchingContext(matchingRules, false), config)

    def expected = '''(
      |  :request (
      |    :method (
      |      #{'method == POST'},
      |      %match:equality (
      |        'POST',
      |        %upper-case (
      |          $.method
      |        ),
      |        NULL
      |      )
      |    ),
      |    :path (
      |      #{"path == '\\/test'"},
      |      %match:equality (
      |        '/test',
      |        $.path,
      |        NULL
      |      )
      |    ),
      |    :"query parameters" (
      |      %expect:empty (
      |        $.query,
      |        %join (
      |          'Expected no query parameters but got ',
      |          $.query
      |        )
      |      )
      |    ),
      |    :body (
      |      %if (
      |        %match:equality (
      |          'text\\/plain; charset=ISO-8859-1',
      |          $.content-type,
      |          NULL,
      |          %error (
      |            'Body type error - ',
      |            %apply ()
      |          )
      |        ),
      |        %match:equality (
      |          'Some nice bit of text',
      |          %convert:UTF8 (
      |            $.body
      |          ),
      |          NULL
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

      def expectedExecutedPlan = '''(
      |  :request (
      |    :method (
      |      #{'method == POST'},
      |      %match:equality (
      |        'POST' => 'POST',
      |        %upper-case (
      |          $.method => 'put'
      |        ) => 'PUT',
      |        NULL => NULL
      |      ) => ERROR(Expected 'PUT' (String) to be equal to 'POST' (String))
      |    ) => BOOL(false),
      |    :path (
      |      #{"path == '\\/test'"},
      |      %match:equality (
      |        '/test' => '/test',
      |        $.path => '/test',
      |        NULL => NULL
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :"query parameters" (
      |      %expect:empty (
      |        $.query => {},
      |        %join (
      |          'Expected no query parameters but got ',
      |          $.query
      |        )
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :body (
      |      %if (
      |        %match:equality (
      |          'text\\/plain; charset=ISO-8859-1' => 'text\\/plain; charset=ISO-8859-1',
      |          $.content-type => 'text\\/plain; charset=ISO-8859-1',
      |          NULL => NULL,
      |          %error (
      |            'Body type error - ',
      |            %apply ()
      |          )
      |        ) => BOOL(true),
      |        %match:equality (
      |          'Some nice bit of text' => 'Some nice bit of text',
      |          %convert:UTF8 (
      |            $.body => BYTES(21, U29tZSBuaWNlIGJpdCBvZiB0ZXh0)
      |          ) => 'Some nice bit of text',
      |          NULL => NULL
      |        ) => BOOL(true)
      |      ) => BOOL(true)
      |    ) => BOOL(true)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    def expectedMatchResult = new RequestMatchResult(
      new MethodMismatch('', '', "Expected 'PUT' (String) to be equal to 'POST' (String)"),
      null,
      [],
      null,
      [],
      new BodyMatchResult(null, [])
    )

    when:
    def plan = V2MatchingEngine.INSTANCE.buildRequestPlan(expectedRequest, context)
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def summary = executedPlan.generateSummary(false)

    then:
    summary == '''request:
    |  method: method == POST - ERROR Expected 'PUT' (String) to be equal to 'POST' (String)
    |  path: path == '/test' - OK
    |  query parameters: - OK
    |  body: - OK
    |'''.stripMargin('|')

    when:
    def mismatches = executedPlan.intoRequestMatchResult()

    then:
    mismatches == expectedMatchResult
  }

  /*
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
   */

  @Ignore
  def 'simple json match request test'() {
    given:
    def request = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('{"b": "22"}', ContentType.JSON))
    def expectedRequest = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('{"a": 100,"b": 200.1}', ContentType.JSON))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def matchingRules = new MatchingRuleCategory('test')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, new MatchingContext(matchingRules, false), config)

    def expected = '''(
      |  :request (
      |    :method (
      |      #{'method == POST'},
      |      %match:equality (
      |        'POST',
      |        %upper-case (
      |          $.method
      |        ),
      |        NULL
      |      )
      |    ),
      |    :path (
      |      #{"path == '\\/test'"},
      |      %match:equality (
      |        '/test',
      |        $.path,
      |        NULL
      |      )
      |    ),
      |    :"query parameters" (
      |      %expect:empty (
      |        $.query,
      |        %join (
      |          'Expected no query parameters but got ',
      |          $.query
      |        )
      |      )
      |    ),
      |    :body (
      |      %if (
      |        %match:equality (
      |          'application/json',
      |          $.content-type,
      |          NULL,
      |          %error (
      |            'Body type error - ',
      |            %apply ()
      |          )
      |        ),
      |        %tee (
      |          %json:parse (
      |            $.body
      |          ),
      |          :$ (
      |            %json:expect:entries (
      |              'OBJECT',
      |              ['a', 'b'],
      |              ~>$
      |            ),
      |            %expect:only-entries (
      |              ['a', 'b'],
      |              ~>$
      |            ),
      |            :$.a (
      |              %match:equality (
      |                json:100,
      |                ~>$.a,
      |                NULL
      |              )
      |            ),
      |            :$.b (
      |              %match:equality (
      |                json:200.1,
      |                ~>$.b,
      |                NULL
      |              )
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :request (
      |    :method (
      |      #{'method == POST'},
      |      %match:equality (
      |        'POST' => 'POST',
      |        %upper-case (
      |          $.method => 'put'
      |        ) => 'PUT',
      |        NULL => NULL
      |      ) => ERROR(Expected 'PUT' (String) to be equal to 'POST' (String))
      |    ) => BOOL(false),
      |    :path (
      |      #{"path == '\\/test'"},
      |      %match:equality (
      |        '/test' => '/test',
      |        $.path => '/test',
      |        NULL => NULL
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :"query parameters" (
      |      %expect:empty (
      |        $.query => {},
      |        %join (
      |          'Expected no query parameters but got ',
      |          $.query
      |        )
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :body (
      |      %if (
      |        %match:equality (
      |          'text\\/plain; charset=ISO-8859-1' => 'text\\/plain; charset=ISO-8859-1',
      |          $.content-type => 'text\\/plain; charset=ISO-8859-1',
      |          NULL => NULL,
      |          %error (
      |            'Body type error - ',
      |            %apply ()
      |          )
      |        ) => BOOL(true),
      |        %match:equality (
      |          'Some nice bit of text' => 'Some nice bit of text',
      |          %convert:UTF8 (
      |            $.body => BYTES(21, U29tZSBuaWNlIGJpdCBvZiB0ZXh0)
      |          ) => 'Some nice bit of text',
      |          NULL => NULL
      |        ) => BOOL(true)
      |      ) => BOOL(true)
      |    ) => BOOL(true)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    def expectedMatchResult = new RequestMatchResult(
      new MethodMismatch('', '', "Expected 'PUT' (String) to be equal to 'POST' (String)"),
      null,
      [],
      null,
      [],
      new BodyMatchResult(null, [])
    )

    when:
    def plan = V2MatchingEngine.INSTANCE.buildRequestPlan(expectedRequest, context)
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def summary = executedPlan.generateSummary(false)

    then:
    summary == '''request:
    |  method: method == POST - ERROR Expected 'PUT' (String) to be equal to 'POST' (String)
    |  path: path == '/test' - OK
    |  query parameters: - OK
    |  body: - OK
    |'''.stripMargin('|')

    when:
    def mismatches = executedPlan.intoRequestMatchResult()

    then:
    mismatches == expectedMatchResult
  }
}
