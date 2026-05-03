package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import com.github.difflib.DiffUtils
import spock.lang.Specification

import static com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff

@SuppressWarnings(['LineLength', 'ClassSize', 'MethodSize'])
class HeaderMatchingSpec extends Specification {
  def 'match headers where there are none'() {
    given:
    def request = new HttpRequest()
    def request2 = new HttpRequest()
    request2.headers = [
      'HEADER_X': ['A']
    ]
    def expectedRequest = new HttpRequest()

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))

    then:
    plan.prettyForm() == '''(
    |  :header-test ()
    |)
    |'''.stripMargin('|')

    when:
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)

    then:
    executedPlan.prettyForm() == '''(
    |  :header-test () => BOOL(true)
    |)
    |'''.stripMargin('|')

    when:
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request2, context)

    then:
    executedPlan.prettyForm() == '''(
    |  :header-test () => BOOL(true)
    |)
    |'''.stripMargin('|')
  }

  def 'match headers with expected headers'() {
    given:
    def request = new HttpRequest()
    request.headers = [
      'HEADER-X': ['b']
    ]
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.headers['HEADER-X'] => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-x': 'b'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with expected headers - missing header'() {
    given:
    def request = new HttpRequest()
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X']
      |          ),
      |          %match:equality (
      |            'b',
      |            $.headers['HEADER-X'],
      |            NULL
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => NULL
      |          ) => BOOL(false),
      |          %match:equality (
      |            'b',
      |            $.headers['HEADER-X'],
      |            NULL
      |          )
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {},
      |        %join (
      |          'The following expected headers were missing: ' => 'The following expected headers were missing: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'header-x'
      |            ) => OK
      |          ) => 'header-x'
      |        ) => 'The following expected headers were missing: header-x'
      |      ) => ERROR(The following expected headers were missing: header-x)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
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
  }

  def 'match headers with mismatch'() {
    given:
    def request = new HttpRequest()
    request.headers = [
      'HEADER-X': ['C']
    ]
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => 'C'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.headers['HEADER-X'] => 'C',
      |            NULL => NULL
      |          ) => ERROR(Expected 'C' \\(String\\) to be equal to 'b' \\(String\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-x': 'C'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with additional headers'() {
    given:
    def request = new HttpRequest()
    request.headers = [
      'HEADER-X': ['b'],
      'HEADER-Y': ['C']
    ]
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.headers['HEADER-X'] => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-x': 'b', 'header-y': 'C'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with additional headers that are not accepted'() {
    given:
    def request = new HttpRequest()
    request.headers = [
      'HEADER-X': ['b'],
      'HEADER-Y': ['C']
    ]
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.headers['HEADER-X'] => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-x': 'b', 'header-y': 'C'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-x': 'b', 'header-y': 'C'},
      |        %join (
      |          'The following headers were unexpected: ' => 'The following headers were unexpected: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'header-y'
      |            ) => OK
      |          ) => 'header-y'
      |        ) => 'The following headers were unexpected: header-y'
      |      ) => ERROR(The following headers were unexpected: header-y)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with additional headers that are not accepted and missing required header'() {
    given:
    def request = new HttpRequest()
    request.headers = [
      'HEADER-Y': ['C']
    ]
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'HEADER-X': ['b']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :HEADER-X (
      |        #{"HEADER-X='b'"},
      |        %if (
      |          %check:exists (
      |            $.headers['HEADER-X'] => NULL
      |          ) => BOOL(false),
      |          %match:equality (
      |            'b',
      |            $.headers['HEADER-X'],
      |            NULL
      |          )
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-y': 'C'},
      |        %join (
      |          'The following expected headers were missing: ' => 'The following expected headers were missing: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'header-x'
      |            ) => OK
      |          ) => 'header-x'
      |        ) => 'The following expected headers were missing: header-x'
      |      ) => ERROR(The following expected headers were missing: header-x),
      |      %expect:only-entries (
      |        %lower-case (
      |          ['HEADER-X'] => ['HEADER-X']
      |        ) => ['header-x'],
      |        $.headers => {'header-y': 'C'},
      |        %join (
      |          'The following headers were unexpected: ' => 'The following headers were unexpected: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'header-y'
      |            ) => OK
      |          ) => 'header-y'
      |        ) => 'The following headers were unexpected: header-y'
      |      ) => ERROR(The following headers were unexpected: header-y)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with matching rule'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new RegexMatcher('^[0-9]+$') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['9023470945622'],
      'REF-CODE': ['test']
    ]

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must match the regular expression /^[0-9]+$/'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '9023470945622'
      |          ) => BOOL(true),
      |          %match:regex (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => '9023470945622',
      |            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': '9023470945622'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with matching rule - mismatch'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new RegexMatcher('^[0-9]+$') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['9023470X945622'],
      'REF-CODE': ['test']
    ]

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must match the regular expression /^[0-9]+$/'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '9023470X945622'
      |          ) => BOOL(true),
      |          %match:regex (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => '9023470X945622',
      |            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
      |          ) => ERROR(Expected '9023470X945622' to match '^[0-9]+$')
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': '9023470X945622'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with values having different lengths'() {
    given:
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'REF-ID': ['1234'],
      'REF-CODE': ['test', 'test2']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE=['test', 'test2']"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE']
      |          ),
      |          %match:equality (
      |            ['test', 'test2'],
      |            $.headers['REF-CODE'],
      |            NULL
      |          )
      |        )
      |      ),
      |      :REF-ID (
      |        #{"REF-ID='1234'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID']
      |          ),
      |          %match:equality (
      |            '1234',
      |            $.headers['REF-ID'],
      |            NULL
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE=['test', 'test2']"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => ['test', 'test2']
      |          ) => BOOL(true),
      |          %match:equality (
      |            ['test', 'test2'] => ['test', 'test2'],
      |            $.headers['REF-CODE'] => ['test', 'test2'],
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{"REF-ID='1234'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '1234'
      |          ) => BOOL(true),
      |          %match:equality (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => '1234',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': ['test', 'test2'], 'ref-id': '1234'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1234'],
      'REF-CODE': ['test', 'test2']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with values having different lengths - mismatch'() {
    given:
    def expectedRequest = new HttpRequest()
    expectedRequest.headers = [
      'REF-ID': ['1234'],
      'REF-CODE': ['test', 'test2']
    ]

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(true, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE=['test', 'test2']"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            ['test', 'test2'] => ['test', 'test2'],
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => ERROR(Expected [test] \\(Array\\) to be equal to [test, test2] \\(Array\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      :REF-ID (
      |        #{"REF-ID='1234'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => ['1234', '1234', '4567']
      |          ) => BOOL(true),
      |          %match:equality (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => ['1234', '1234', '4567'],
      |            NULL => NULL
      |          ) => ERROR(Expected [1234, 1234, 4567] \\(Array\\) to be equal to [1234] \\(Array\\), Expected '4567' \\(String\\) to be equal to '1234' \\(String\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': ['1234', '1234', '4567']},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context.forHeaders()))
    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1234', '1234', '4567'],
      'REF-CODE': ['test']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with number type matching rule'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE']
      |          ),
      |          %match:equality (
      |            'test',
      |            $.headers['REF-CODE'],
      |            NULL
      |          )
      |        )
      |      ),
      |      :REF-ID (
      |        #{'REF-ID must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID']
      |          ),
      |          %match:integer (
      |            '1234',
      |            $.headers['REF-ID'],
      |            json:{}
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '9023470945622'
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => '9023470945622',
      |            json:{} => json:{}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': '9023470945622'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['9023470945622'],
      'REF-CODE': ['test']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1111', '2222'],
      'REF-CODE': ['test']
    ]
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()

    expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => ['1111', '2222']
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => ['1111', '2222'],
      |            json:{} => json:{}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': ['1111', '2222']},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with number type matching rule - mismatch'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '9023470X945622'
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => '9023470X945622',
      |            json:{} => json:{}
      |          ) => ERROR(Expected '9023470X945622' \\(String\\) to be an integer)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': '9023470X945622'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['9023470X945622'],
      'REF-CODE': ['test']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1111', 'two'],
      'REF-CODE': ['test']
    ]
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => ['1111', 'two']
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1234' => '1234',
      |            $.headers['REF-ID'] => ['1111', 'two'],
      |            json:{} => json:{}
      |          ) => ERROR(Expected 'two' \\(String\\) to be an integer)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': ['1111', 'two']},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with min type matching rules'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new MinTypeMatcher(2)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234', '4567'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE']
      |          ),
      |          %match:equality (
      |            'test',
      |            $.headers['REF-CODE'],
      |            NULL
      |          )
      |        )
      |      ),
      |      :REF-ID (
      |        #{'REF-ID must match by type and have at least 2 items'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID']
      |          ),
      |          %match:min-type (
      |            ['1234', '4567'],
      |            $.headers['REF-ID'],
      |            json:{"min":2}
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must match by type and have at least 2 items'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => ['1', '1', '1']
      |          ) => BOOL(true),
      |          %match:min-type (
      |            ['1234', '4567'] => ['1234', '4567'],
      |            $.headers['REF-ID'] => ['1', '1', '1'],
      |            json:{"min":2} => json:{"min":2}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': ['1', '1', '1']},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1', '1', '1'],
      'REF-CODE': ['test']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match headers with min type matching rules - mismatch'() {
    given:
    def matchingRulesForHeaders = new MatchingRuleCategory('header', [
      'REF-ID': new MatchingRuleGroup([new MinTypeMatcher(2)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForHeaders)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'REF-ID': ['1234', '4567'],
        'REF-CODE': ['test']
      ],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :REF-CODE (
      |        #{"REF-CODE='test'"},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-CODE'] => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.headers['REF-CODE'] => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :REF-ID (
      |        #{'REF-ID must match by type and have at least 2 items'},
      |        %if (
      |          %check:exists (
      |            $.headers['REF-ID'] => '1'
      |          ) => BOOL(true),
      |          %match:min-type (
      |            ['1234', '4567'] => ['1234', '4567'],
      |            $.headers['REF-ID'] => '1',
      |            json:{"min":2} => json:{"min":2}
      |          ) => ERROR(Expected [1] \\(size 1\\) to have minimum size of 2)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['REF-CODE', 'REF-ID'] => ['REF-CODE', 'REF-ID']
      |        ) => ['ref-code', 'ref-id'],
      |        $.headers => {'ref-code': 'test', 'ref-id': '1'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))

    def request = new HttpRequest()
    request.headers = [
      'REF-ID': ['1'],
      'REF-CODE': ['test']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match content type header'() {
    given:
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'Content-Type': ['application/hal+json']
      ]
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/hal+json'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type']
      |          ),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type']
      |            ),
      |            %match:equality (
      |              'application/hal+json',
      |              %to-string (
      |                ~>value
      |              ),
      |              NULL
      |            )
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/hal+json'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type'] => 'application/hal+json;charset=UTF-8'
      |          ) => BOOL(true),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type'] => 'application/hal+json;charset=UTF-8'
      |            ) => json:{"parameters":{"charset":"UTF-8"},"value":"application/hal+json"},
      |            %match:equality (
      |              'application/hal+json' => 'application/hal+json',
      |              %to-string (
      |                ~>value => json:"application/hal+json"
      |              ) => 'application/hal+json',
      |              NULL => NULL
      |            ) => BOOL(true)
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type'] => ['Content-Type']
      |        ) => ['content-type'],
      |        $.headers => {'content-type': 'application/hal+json;charset=UTF-8'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest()
    request.headers = [
      'content-type': ['application/hal+json;charset=UTF-8']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match content type header with charset parameter'() {
    given:
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [:],
      [
        'Content-Type': ['application/json;charset=UTF-8']
      ]
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forHeaders()

    def expected = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/json;charset=UTF-8'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type']
      |          ),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type']
      |            ),
      |            %match:equality (
      |              'application/json',
      |              %to-string (
      |                ~>value
      |              ),
      |              NULL
      |            ),
      |            :charset (
      |              %if (
      |                %check:exists (
      |                  ~>parameters.charset
      |                ),
      |                %match:equality (
      |                  'utf-8',
      |                  %lower-case (
      |                    ~>parameters.charset
      |                  ),
      |                  NULL
      |                ),
      |                %error (
      |                  'Expected a charset value of \\'UTF-8\\' but it was missing'
      |                )
      |              )
      |            )
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type']
      |        ),
      |        $.headers,
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      )
      |    )
      |  )
      |)
      |'''.stripMargin('|')

    def expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/json;charset=UTF-8'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type'] => 'application\\/json; charset=UTF-8'
      |          ) => BOOL(true),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type'] => 'application\\/json; charset=UTF-8'
      |            ) => json:{"parameters":{"charset":"UTF-8"},"value":"application/json"},
      |            %match:equality (
      |              'application/json' => 'application/json',
      |              %to-string (
      |                ~>value => json:"application/json"
      |              ) => 'application/json',
      |              NULL => NULL
      |            ) => BOOL(true),
      |            :charset (
      |              %if (
      |                %check:exists (
      |                  ~>parameters.charset => json:"UTF-8"
      |                ) => BOOL(true),
      |                %match:equality (
      |                  'utf-8' => 'utf-8',
      |                  %lower-case (
      |                    ~>parameters.charset => json:"UTF-8"
      |                  ) => 'utf-8',
      |                  NULL => NULL
      |                ) => BOOL(true),
      |                %error (
      |                  'Expected a charset value of \\'UTF-8\\' but it was missing'
      |                )
      |              ) => BOOL(true)
      |            ) => BOOL(true)
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type'] => ['Content-Type']
      |        ) => ['content-type'],
      |        $.headers => {'content-type': 'application\\/json; charset=UTF-8'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('header-test')
    plan.add(V2MatchingEngine.INSTANCE.setupHeaderPlan(expectedRequest, context))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expected, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest()
    request.headers = [
      'content-type': ['application/json; charset=UTF-8']
    ]
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    request = new HttpRequest()
    request.headers = [
      'content-type': ['application/json']
    ]
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/json;charset=UTF-8'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type'] => 'application/json'
      |          ) => BOOL(true),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type'] => 'application/json'
      |            ) => json:{"parameters":{},"value":"application/json"},
      |            %match:equality (
      |              'application/json' => 'application/json',
      |              %to-string (
      |                ~>value => json:"application/json"
      |              ) => 'application/json',
      |              NULL => NULL
      |            ) => BOOL(true),
      |            :charset (
      |              %if (
      |                %check:exists (
      |                  ~>parameters.charset => NULL
      |                ) => BOOL(false),
      |                %match:equality (
      |                  'utf-8',
      |                  %lower-case (
      |                    ~>parameters.charset
      |                  ),
      |                  NULL
      |                ),
      |                %error (
      |                  'Expected a charset value of \\'UTF-8\\' but it was missing' => 'Expected a charset value of \\'UTF-8\\' but it was missing'
      |                ) => ERROR(Expected a charset value of 'UTF-8' but it was missing)
      |              ) => ERROR(Expected a charset value of 'UTF-8' but it was missing)
      |            ) => BOOL(false)
      |          ) => BOOL(false)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type'] => ['Content-Type']
      |        ) => ['content-type'],
      |        $.headers => {'content-type': 'application/json'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    request = new HttpRequest()
    request.headers = [
      'content-type': ['application/json;charset=UTF-16;other=stuff']
    ]
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :header-test (
      |    :headers (
      |      :Content-Type (
      |        #{"Content-Type='application\\/json;charset=UTF-8'"},
      |        %if (
      |          %check:exists (
      |            $.headers['Content-Type'] => 'application/json;charset=UTF-16;other=stuff'
      |          ) => BOOL(true),
      |          %tee (
      |            %header:parse (
      |              $.headers['Content-Type'] => 'application/json;charset=UTF-16;other=stuff'
      |            ) => json:{"parameters":{"charset":"UTF-16","other":"stuff"},"value":"application/json"},
      |            %match:equality (
      |              'application/json' => 'application/json',
      |              %to-string (
      |                ~>value => json:"application/json"
      |              ) => 'application/json',
      |              NULL => NULL
      |            ) => BOOL(true),
      |            :charset (
      |              %if (
      |                %check:exists (
      |                  ~>parameters.charset => json:"UTF-16"
      |                ) => BOOL(true),
      |                %match:equality (
      |                  'utf-8' => 'utf-8',
      |                  %lower-case (
      |                    ~>parameters.charset => json:"UTF-16"
      |                  ) => 'utf-16',
      |                  NULL => NULL
      |                ) => ERROR(Expected 'utf-16' \\(String\\) to be equal to 'utf-8' \\(String\\)),
      |                %error (
      |                  'Expected a charset value of \\'UTF-8\\' but it was missing'
      |                )
      |              ) => BOOL(false)
      |            ) => BOOL(false)
      |          ) => BOOL(false)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        %lower-case (
      |          ['Content-Type'] => ['Content-Type']
      |        ) => ['content-type'],
      |        $.headers => {'content-type': 'application/json;charset=UTF-16;other=stuff'},
      |        %join (
      |          'The following expected headers were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }
}
