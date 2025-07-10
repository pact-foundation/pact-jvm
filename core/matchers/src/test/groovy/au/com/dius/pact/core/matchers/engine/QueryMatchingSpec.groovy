package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
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

@SuppressWarnings(['LineLength', 'AbcMetric', 'ClassSize', 'MethodSize'])
class QueryMatchingSpec extends Specification {
  def 'match query where none are expected'() {
    given:
    def expectedRequest = new HttpRequest()

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    then:
    plan.prettyForm() == '''(
    |  :query-test (
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

    when:
    def request = new HttpRequest()
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)

    then:
    executedPlan.prettyForm() == '''(
    |  :query-test (
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

    when:
    request = new HttpRequest('get', '/', [a: ['b']])
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)

    then:
    executedPlan.prettyForm() == '''(
    |  :query-test (
    |    :"query parameters" (
    |      %expect:empty (
    |        $.query => {'a': 'b'},
    |        %join (
    |          'Expected no query parameters but got ' => 'Expected no query parameters but got ',
    |          $.query => {'a': 'b'}
    |        ) => 'Expected no query parameters but got {\\'a\\': \\'b\\'}'
    |      ) => ERROR(Expected no query parameters but got {'a': 'b'})
    |    ) => BOOL(false)
    |  ) => BOOL(false)
    |)
    |'''.stripMargin('|')
  }

  def 'match query with expected query string'() {
    given:
    def expectedRequest = new HttpRequest('get', '/', [
      a: ['b']
    ])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a
      |          ),
      |          %match:equality (
      |            'b',
      |            $.query.a,
      |            NULL
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        ['a'],
      |        $.query,
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ),
      |      %expect:only-entries (
      |        ['a'],
      |        $.query,
      |        %join (
      |          'The following query parameters were not expected: ',
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
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.query.a => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'b'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'b'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest('get', '/', [
      a: ['b']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with expected query string - missing value'() {
    given:
    def expectedRequest = new HttpRequest('get', '/', [
      a: ['b']
    ])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => NULL
      |          ) => BOOL(false),
      |          %match:equality (
      |            'b',
      |            $.query.a,
      |            NULL
      |          )
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['a'] => ['a'],
      |        $.query => {},
      |        %join (
      |          'The following expected query parameters were missing: ' => 'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'a'
      |            ) => OK
      |          ) => 'a'
      |        ) => 'The following expected query parameters were missing: a'
      |      ) => ERROR(The following expected query parameters were missing: a),
      |      %expect:only-entries (
      |        ['a'] => ['a'],
      |        $.query => {},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def request = new HttpRequest('get', '/', [:])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with expected query string - incorrect value'() {
    given:
    def expectedRequest = new HttpRequest('get', '/', [
      a: ['b']
    ])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => 'c'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.query.a => 'c',
      |            NULL => NULL
      |          ) => ERROR(Expected 'c' \\(String\\) to be equal to 'b' \\(String\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'c'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'c'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def request = new HttpRequest('get', '/', [a: ['c']])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with expected query string - unexpected value'() {
    given:
    def expectedRequest = new HttpRequest('get', '/', [
      a: ['b']
    ])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.query.a => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'b', 'b': 'c'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['a'] => ['a'],
      |        $.query => {'a': 'b', 'b': 'c'},
      |        %join (
      |          'The following query parameters were not expected: ' => 'The following query parameters were not expected: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'b'
      |            ) => OK
      |          ) => 'b'
      |        ) => 'The following query parameters were not expected: b'
      |      ) => ERROR(The following query parameters were not expected: b)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def request = new HttpRequest('get', '/', [
      a: ['b'],
      b: ['c']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with expected query string - missing and unexpected value'() {
    given:
    def expectedRequest = new HttpRequest('get', '/', [
      a: ['b']
    ])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => NULL
      |          ) => BOOL(false),
      |          %match:equality (
      |            'b',
      |            $.query.a,
      |            NULL
      |          )
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['a'] => ['a'],
      |        $.query => {'b': 'c'},
      |        %join (
      |          'The following expected query parameters were missing: ' => 'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'a'
      |            ) => OK
      |          ) => 'a'
      |        ) => 'The following expected query parameters were missing: a'
      |      ) => ERROR(The following expected query parameters were missing: a),
      |      %expect:only-entries (
      |        ['a'] => ['a'],
      |        $.query => {'b': 'c'},
      |        %join (
      |          'The following query parameters were not expected: ' => 'The following query parameters were not expected: ',
      |          %join-with (
      |            ', ' => ', ',
      |            ** (
      |              %apply () => 'b'
      |            ) => OK
      |          ) => 'b'
      |        ) => 'The following query parameters were not expected: b'
      |      ) => ERROR(The following query parameters were not expected: b)
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |)
      |'''.stripMargin('|')

    when:
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def request = new HttpRequest('get', '/', [
      b: ['c']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with matching rule'() {
    given:
    def matchingRulesForQuery = new MatchingRuleCategory('query', [
      'user_id': new MatchingRuleGroup([new RegexMatcher('^[0-9]+$') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForQuery)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [
        'user_id': ['1'],
        'field': ['test']
      ],
      [:],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :field (
      |        #{"field='test'"},
      |        %if (
      |          %check:exists (
      |            $.query.field
      |          ),
      |          %match:equality (
      |            'test',
      |            $.query.field,
      |            NULL
      |          )
      |        )
      |      ),
      |      :user_id (
      |        #{'user_id must match the regular expression /^[0-9]+$/'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id
      |          ),
      |          %match:regex (
      |            '1',
      |            $.query.user_id,
      |            json:{"regex":"^[0-9]+$"}
      |          )
      |        )
      |      ),
      |      %expect:entries (
      |        ['field', 'user_id'],
      |        $.query,
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ),
      |      %expect:only-entries (
      |        ['field', 'user_id'],
      |        $.query,
      |        %join (
      |          'The following query parameters were not expected: ',
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
      |  :query-test (
      |    :"query parameters" (
      |      :field (
      |        #{"field='test'"},
      |        %if (
      |          %check:exists (
      |            $.query.field => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.query.field => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :user_id (
      |        #{'user_id must match the regular expression /^[0-9]+$/'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id => '2455324356421'
      |          ) => BOOL(true),
      |          %match:regex (
      |            '1' => '1',
      |            $.query.user_id => '2455324356421',
      |            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['field', 'user_id'] => ['field', 'user_id'],
      |        $.query => {'field': 'test', 'user_id': '2455324356421'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['field', 'user_id'] => ['field', 'user_id'],
      |        $.query => {'field': 'test', 'user_id': '2455324356421'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))
    def pretty = plan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    def request = new HttpRequest('get', '/', [
      field: ['test'],
      user_id: ['2455324356421']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with matching rule - mismatch'() {
    given:
    def matchingRulesForQuery = new MatchingRuleCategory('query', [
      'user_id': new MatchingRuleGroup([new RegexMatcher('^[0-9]+$') ])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForQuery)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [
        'user_id': ['1'],
        'field': ['test']
      ],
      [:],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :field (
      |        #{"field='test'"},
      |        %if (
      |          %check:exists (
      |            $.query.field => 'test'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'test' => 'test',
      |            $.query.field => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :user_id (
      |        #{'user_id must match the regular expression /^[0-9]+$/'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id => '100Kb'
      |          ) => BOOL(true),
      |          %match:regex (
      |            '1' => '1',
      |            $.query.user_id => '100Kb',
      |            json:{"regex":"^[0-9]+$"} => json:{"regex":"^[0-9]+$"}
      |          ) => ERROR(Expected '100Kb' to match '^[0-9]+$')
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['field', 'user_id'] => ['field', 'user_id'],
      |        $.query => {'field': 'test', 'user_id': '100Kb'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['field', 'user_id'] => ['field', 'user_id'],
      |        $.query => {'field': 'test', 'user_id': '100Kb'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    def request = new HttpRequest('get', '/', [
      field: ['test'],
      user_id: ['100Kb']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }

  def 'match query with query values having different lengths'() {
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [
        a: ['b'],
        c: ['d', 'e']
      ]
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => 'b'
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.query.a => 'b',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      :c (
      |        #{"c=['d', 'e']"},
      |        %if (
      |          %check:exists (
      |            $.query.c => ['d', 'e']
      |          ) => BOOL(true),
      |          %match:equality (
      |            ['d', 'e'] => ['d', 'e'],
      |            $.query.c => ['d', 'e'],
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['a', 'c'] => ['a', 'c'],
      |        $.query => {'a': 'b', 'c': ['d', 'e']},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['a', 'c'] => ['a', 'c'],
      |        $.query => {'a': 'b', 'c': ['d', 'e']},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    def request = new HttpRequest('get', '/', [
      a: ['b'],
      c: ['d', 'e']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    request = new HttpRequest('get', '/', [
      a: ['b', 'e'],
      c: ['d']
    ])
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :a (
      |        #{"a='b'"},
      |        %if (
      |          %check:exists (
      |            $.query.a => ['b', 'e']
      |          ) => BOOL(true),
      |          %match:equality (
      |            'b' => 'b',
      |            $.query.a => ['b', 'e'],
      |            NULL => NULL
      |          ) => ERROR(Expected [b, e] \\(Array\\) to be equal to [b] \\(Array\\), Expected 'e' \\(String\\) to be equal to 'b' \\(String\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      :c (
      |        #{"c=['d', 'e']"},
      |        %if (
      |          %check:exists (
      |            $.query.c => 'd'
      |          ) => BOOL(true),
      |          %match:equality (
      |            ['d', 'e'] => ['d', 'e'],
      |            $.query.c => 'd',
      |            NULL => NULL
      |          ) => ERROR(Expected [d] \\(Array\\) to be equal to [d, e] \\(Array\\))
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['a', 'c'] => ['a', 'c'],
      |        $.query => {'a': ['b', 'e'], 'c': 'd'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['a', 'c'] => ['a', 'c'],
      |        $.query => {'a': ['b', 'e'], 'c': 'd'},
      |        %join (
      |          'The following query parameters were not expected: ',
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

  def 'match query with number type matching rule'() {
    given:
    def matchingRulesForQuery = new MatchingRuleCategory('query', [
      'user_id': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForQuery)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [
        'user_id': ['1000']
      ],
      [:],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forQuery()

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :user_id (
      |        #{'user_id must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id => '123456789'
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1000' => '1000',
      |            $.query.user_id => '123456789',
      |            json:{} => json:{}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': '123456789'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': '123456789'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    def request = new HttpRequest('get', '/', [
      user_id: ['123456789']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    request = new HttpRequest('get', '/', [
      user_id: ['100', '200']
    ])
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :user_id (
      |        #{'user_id must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id => ['100', '200']
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1000' => '1000',
      |            $.query.user_id => ['100', '200'],
      |            json:{} => json:{}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': ['100', '200']},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': ['100', '200']},
      |        %join (
      |          'The following query parameters were not expected: ',
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

    when:
    plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    request = new HttpRequest('get', '/', [
      user_id: ['100x', '200']
    ])
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :user_id (
      |        #{'user_id must be an integer'},
      |        %if (
      |          %check:exists (
      |            $.query.user_id => ['100x', '200']
      |          ) => BOOL(true),
      |          %match:integer (
      |            '1000' => '1000',
      |            $.query.user_id => ['100x', '200'],
      |            json:{} => json:{}
      |          ) => ERROR(Expected '100x' \\(String\\) to be an integer)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': ['100x', '200']},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['user_id'] => ['user_id'],
      |        $.query => {'user_id': ['100x', '200']},
      |        %join (
      |          'The following query parameters were not expected: ',
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

  def 'match query with min type matching rules'() {
    given:
    def matchingRulesForQuery = new MatchingRuleCategory('query', [
      'id': new MatchingRuleGroup([new MinTypeMatcher(2)])
    ])
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(matchingRulesForQuery)
    def expectedRequest = new HttpRequest(
      'get',
      '/',
      [
        'id': ['1', '2']
      ],
      [:],
      OptionalBody.missing(),
      matchingRules,
      new Generators()
    )

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config).forQuery()

    def expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :id (
      |        #{'id must match by type and have at least 2 items'},
      |        %if (
      |          %check:exists (
      |            $.query.id => ['1', '2', '3', '4']
      |          ) => BOOL(true),
      |          %match:min-type (
      |            ['1', '2'] => ['1', '2'],
      |            $.query.id => ['1', '2', '3', '4'],
      |            json:{"min":2} => json:{"min":2}
      |          ) => BOOL(true)
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %expect:entries (
      |        ['id'] => ['id'],
      |        $.query => {'id': ['1', '2', '3', '4']},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['id'] => ['id'],
      |        $.query => {'id': ['1', '2', '3', '4']},
      |        %join (
      |          'The following query parameters were not expected: ',
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
    def plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    def request = new HttpRequest('get', '/', [
      id: ['1', '2', '3', '4']
    ])
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    def pretty = executedPlan.prettyForm()
    def patch = DiffUtils.diff(pretty, expectedExecutedPlan, null)
    def diff = generateUnifiedDiff('', '', pretty.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''

    when:
    plan = new ExecutionPlan('query-test')
    plan.add(V2MatchingEngine.INSTANCE.setupQueryPlan(expectedRequest, context.forQuery()))

    request = new HttpRequest('get', '/', [
      id: ['100']
    ])
    executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, request, context)
    pretty = executedPlan.prettyForm()
    expectedExecutedPlan = '''(
      |  :query-test (
      |    :"query parameters" (
      |      :id (
      |        #{'id must match by type and have at least 2 items'},
      |        %if (
      |          %check:exists (
      |            $.query.id => '100'
      |          ) => BOOL(true),
      |          %match:min-type (
      |            ['1', '2'] => ['1', '2'],
      |            $.query.id => '100',
      |            json:{"min":2} => json:{"min":2}
      |          ) => ERROR(Expected [100] \\(size 1\\) to have minimum size of 2)
      |        ) => BOOL(false)
      |      ) => BOOL(false),
      |      %expect:entries (
      |        ['id'] => ['id'],
      |        $.query => {'id': '100'},
      |        %join (
      |          'The following expected query parameters were missing: ',
      |          %join-with (
      |            ', ',
      |            ** (
      |              %apply ()
      |            )
      |          )
      |        )
      |      ) => OK,
      |      %expect:only-entries (
      |        ['id'] => ['id'],
      |        $.query => {'id': '100'},
      |        %join (
      |          'The following query parameters were not expected: ',
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
