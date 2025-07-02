package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.matchers.BodyItemMatchResult
import au.com.dius.pact.core.matchers.BodyMatchResult
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.MethodMismatch
import au.com.dius.pact.core.matchers.PathMismatch
import au.com.dius.pact.core.matchers.RequestMatchResult
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import com.github.difflib.DiffUtils
import spock.lang.Specification

import static com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff

@SuppressWarnings(['MethodSize', 'AbcMetric'])
class MatchingEngineSpec extends Specification {

  def 'simple match request test'() {
    given:
    def request = new HttpRequest('put', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))
    def expectedRequest = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('Some nice bit of text', ContentType.TEXT_PLAIN))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

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

  def 'simple json match request test'() {
    given:
    def request = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('{"b": "22"}', ContentType.JSON))
    def expectedRequest = new HttpRequest('POST', '/test', [:], [:],
      OptionalBody.body('{"a": 100,"b": 200.1}', ContentType.JSON))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

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
      |          $.method => 'POST'
      |        ) => 'POST',
      |        NULL => NULL
      |      ) => BOOL(true)
      |    ) => BOOL(true),
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
      |          'application/json' => 'application/json',
      |          $.content-type => 'application/json',
      |          NULL => NULL,
      |          %error (
      |            'Body type error - ',
      |            %apply ()
      |          )
      |        ) => BOOL(true),
      |        %tee (
      |          %json:parse (
      |            $.body => BYTES(11, eyJiIjogIjIyIn0=)
      |          ) => json:{"b":"22"},
      |          :$ (
      |            %json:expect:entries (
      |              'OBJECT' => 'OBJECT',
      |              ['a', 'b'] => ['a', 'b'],
      |              ~>$ => json:{"b":"22"}
      |            ) => ERROR(The following expected entries were missing from the actual object: a),
      |            %expect:only-entries (
      |              ['a', 'b'] => ['a', 'b'],
      |              ~>$ => json:{"b":"22"}
      |            ) => OK,
      |            :$.a (
      |              %match:equality (
      |                json:100 => json:100,
      |                ~>$.a => NULL,
      |                NULL => NULL
      |              ) => ERROR(Expected null (Null) to be equal to 100 (Integer))
      |            ) => BOOL(false),
      |            :$.b (
      |              %match:equality (
      |                json:200.1 => json:200.1,
      |                ~>$.b => json:"22",
      |                NULL => NULL
      |              ) => ERROR(Expected '22' (String) to be equal to 200.1 (Decimal))
      |            ) => BOOL(false)
      |          ) => BOOL(false)
      |        ) => BOOL(false)
      |      ) => BOOL(false)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    def expectedMatchResult = [
      new BodyItemMatchResult('$', [
        new BodyMismatch(null, null, 'The following expected entries were missing from the actual Object: a', '$')
      ]),
      new BodyItemMatchResult('$.a', [
        new BodyMismatch(null, null, 'Expected null (Null) to be equal to 100 (Integer)', '$.a')
      ]),
      new BodyItemMatchResult('$.b', [
        new BodyMismatch(null, null, "Expected '22' (String) to be equal to 200.1 (Decimal)", '$.b')
      ])
    ]

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
    |  method: method == POST - OK
    |  path: path == '/test' - OK
    |  query parameters: - OK
    |  body:
    |    $: - ERROR The following expected entries were missing from the actual object: a
    |      $.a: - ERROR Expected null (Null) to be equal to 100 (Integer)
    |      $.b: - ERROR Expected '22' (String) to be equal to 200.1 (Decimal)
    |'''.stripMargin('|')

    when:
    def mismatches = executedPlan.intoRequestMatchResult()

    then:
    mismatches.method == null
    mismatches.path == null
    mismatches.query == []
    mismatches.cookie == null
    mismatches.headers == []
    mismatches.body.typeMismatch == null
    mismatches.body.bodyResults.size() == 3
    mismatches.body.bodyResults[0].key == '$'
    mismatches.body.bodyResults[0].result.size() == 1
    mismatches.body.bodyResults[0].result[0].mismatch ==
      'The following expected entries were missing from the actual object: a'
    mismatches.body.bodyResults[0].result[0].path == '$'
    mismatches.body.bodyResults[1] == expectedMatchResult[1]
    mismatches.body.bodyResults[2] == expectedMatchResult[2]
  }

  def 'match path with matching rule'() {
    given:
    def request = new HttpRequest('get', '/test12345', [:], [:], OptionalBody.missing())

    def matchingRulesForPath = new MatchingRuleCategory('path', [
      '': new MatchingRuleGroup([new RegexMatcher('\\/test[0-9]+') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForPath)
    def expectedRequest = new HttpRequest('get', '/test', [:], [:], OptionalBody.missing(),
      matchingRules, new Generators())

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expected = '''(
      |  :request (
      |    :method (
      |      #{'method == GET'},
      |      %match:equality (
      |        'GET',
      |        %upper-case (
      |          $.method
      |        ),
      |        NULL
      |      )
      |    ),
      |    :path (
      |      #{'path must match the regular expression /\\/test[0-9]+/'},
      |      %match:regex (
      |        '/test',
      |        $.path,
      |        json:{"regex":"\\\\/test[0-9]+"}
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
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :request (
      |    :method (
      |      #{'method == GET'},
      |      %match:equality (
      |        'GET' => 'GET',
      |        %upper-case (
      |          $.method => 'get'
      |        ) => 'GET',
      |        NULL => NULL
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :path (
      |      #{'path must match the regular expression /\\/test[0-9]+/'},
      |      %match:regex (
      |        '/test' => '/test',
      |        $.path => '/test12345',
      |        json:{"regex":"\\\\/test[0-9]+"} => json:{"regex":"\\\\/test[0-9]+"}
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
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    def expectedMatchResult = new RequestMatchResult(
      null,
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
    |  method: method == GET - OK
    |  path: path must match the regular expression /\\/test[0-9]+/ - OK
    |  query parameters: - OK
    |'''.stripMargin('|')

    when:
    def mismatches = executedPlan.intoRequestMatchResult()

    then:
    mismatches == expectedMatchResult
  }

  def 'match path with matching rule - mismatch'() {
    given:
    def request = new HttpRequest('get', '/test12345X', [:], [:], OptionalBody.missing())

    def matchingRulesForPath = new MatchingRuleCategory('path', [
      '': new MatchingRuleGroup([new RegexMatcher('\\/test[0-9]+') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForPath)
    def expectedRequest = new HttpRequest('get', '/test', [:], [:], OptionalBody.missing(),
      matchingRules, new Generators())

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :request (
      |    :method (
      |      #{'method == GET'},
      |      %match:equality (
      |        'GET' => 'GET',
      |        %upper-case (
      |          $.method => 'get'
      |        ) => 'GET',
      |        NULL => NULL
      |      ) => BOOL(true)
      |    ) => BOOL(true),
      |    :path (
      |      #{'path must match the regular expression /\\/test[0-9]+/'},
      |      %match:regex (
      |        '/test' => '/test',
      |        $.path => '/test12345X',
      |        json:{"regex":"\\\\/test[0-9]+"} => json:{"regex":"\\\\/test[0-9]+"}
      |      ) => ERROR(Expected '/test12345X' to match '\\/test[0-9]+')
      |    ) => BOOL(false),
      |    :"query parameters" (
      |      %expect:empty (
      |        $.query => {},
      |        %join (
      |          'Expected no query parameters but got ',
      |          $.query
      |        )
      |      ) => BOOL(true)
      |    ) => BOOL(true)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    def expectedMatchResult = new RequestMatchResult(
      null,
      new PathMismatch('', '', "Expected '/test12345X' to match '\\/test[0-9]+'"),
      [],
      null,
      [],
      new BodyMatchResult(null, [])
    )

    when:
    def plan = V2MatchingEngine.INSTANCE.buildRequestPlan(expectedRequest, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def summary = executedPlan.generateSummary(false)

    then:
    summary == '''request:
    |  method: method == GET - OK
    |  path: path must match the regular expression /\\/test[0-9]+/ - ERROR Expected '/test12345X' to match '\\/test[0-9]+\'
    |  query parameters: - OK
    |'''.stripMargin('|')

    when:
    def mismatches = executedPlan.intoRequestMatchResult()

    then:
    mismatches == expectedMatchResult
  }
}
