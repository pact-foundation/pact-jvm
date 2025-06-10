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
}
