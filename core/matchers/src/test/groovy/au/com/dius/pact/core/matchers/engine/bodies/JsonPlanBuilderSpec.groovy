package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class JsonPlanBuilderSpec extends Specification {
  PlanMatchingContext context
  V4Pact pact
  V4Interaction.SynchronousHttp interaction
  MatchingConfiguration config

  def setup() {
    pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    interaction = new V4Interaction.SynchronousHttp('test interaction')
    config = new MatchingConfiguration(false, false, true, false)
    context = new PlanMatchingContext(pact, interaction, config)
  }

  def 'json plan builder with null'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.Null.INSTANCE.serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %match:equality (
    |      json:null,
    |      ~>$,
    |      NULL
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with boolean'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.True.INSTANCE.serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %match:equality (
    |      json:true,
    |      ~>$,
    |      NULL
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with string'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.StringValue('I am a string!').serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %match:equality (
    |      json:"I am a string!",
    |      ~>$,
    |      NULL
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with int'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Integer(1000).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %match:equality (
    |      json:1000,
    |      ~>$,
    |      NULL
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with float'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Decimal(1000.3).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %match:equality (
    |      json:1000.3,
    |      ~>$,
    |      NULL
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with empty array'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Array([]).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:expect:empty (
    |      'ARRAY',
    |      ~>$
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with array'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Array([
      new JsonValue.Integer(100),
      new JsonValue.Integer(200),
      new JsonValue.Integer(300)
    ]).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:match:length (
    |      'ARRAY',
    |      UINT(3),
    |      ~>$
    |    ),
    |    :$[0] (
    |      %if (
    |        %check:exists (
    |          ~>$[0]
    |        ),
    |        %match:equality (
    |          json:100,
    |          ~>$[0],
    |          NULL
    |        )
    |      )
    |    ),
    |    :$[1] (
    |      %if (
    |        %check:exists (
    |          ~>$[1]
    |        ),
    |        %match:equality (
    |          json:200,
    |          ~>$[1],
    |          NULL
    |        )
    |      )
    |    ),
    |    :$[2] (
    |      %if (
    |        %check:exists (
    |          ~>$[2]
    |        ),
    |        %match:equality (
    |          json:300,
    |          ~>$[2],
    |          NULL
    |        )
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with empty object'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Object([:]).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:expect:empty (
    |      'OBJECT',
    |      ~>$
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with object'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Object([
      a: new JsonValue.Integer(100),
      b: new JsonValue.Integer(200),
      c: new JsonValue.Integer(300)
    ]).serialise()

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:expect:entries (
    |      'OBJECT',
    |      ['a', 'b', 'c'],
    |      ~>$
    |    ),
    |    %expect:only-entries (
    |      ['a', 'b', 'c'],
    |      ~>$
    |    ),
    |    :$.a (
    |      %match:equality (
    |        json:100,
    |        ~>$.a,
    |        NULL
    |      )
    |    ),
    |    :$.b (
    |      %match:equality (
    |        json:200,
    |        ~>$.b,
    |        NULL
    |      )
    |    ),
    |    :$.c (
    |      %match:equality (
    |        json:300,
    |        ~>$.c,
    |        NULL
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with object with matching rule'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = new JsonValue.Object([
      a: new JsonValue.Integer(100),
      b: new JsonValue.Integer(200),
      c: new JsonValue.Integer(300)
    ]).serialise()
    def matchingRules = new MatchingRuleCategory('body', [
      '$.a': new MatchingRuleGroup([ new RegexMatcher('^[0-9]+$') ])
    ])
    def context = new PlanMatchingContext(pact, interaction, config, new MatchingContext(matchingRules, false))

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:expect:entries (
    |      'OBJECT',
    |      ['a', 'b', 'c'],
    |      ~>$
    |    ),
    |    %expect:only-entries (
    |      ['a', 'b', 'c'],
    |      ~>$
    |    ),
    |    :$.a (
    |      #{'a must match the regular expression /^[0-9]+$/'},
    |      %match:regex (
    |        json:100,
    |        ~>$.a,
    |        json:{"regex":"^[0-9]+$"}
    |      )
    |    ),
    |    :$.b (
    |      %match:equality (
    |        json:200,
    |        ~>$.b,
    |        NULL
    |      )
    |    ),
    |    :$.c (
    |      %match:equality (
    |        json:300,
    |        ~>$.c,
    |        NULL
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'json plan builder with array and type matcher'() {
    given:
    def builder = JsonPlanBuilder.INSTANCE
    def content = '{"item": [{"a": 100}, {"a": 200}, {"a": 300}]}'
    def matchingRules = new MatchingRuleCategory('body', [
      '$.item': new MatchingRuleGroup([ new MinTypeMatcher(2) ])
    ])
    def context = new PlanMatchingContext(pact, interaction, config, new MatchingContext(matchingRules, false))

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %json:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %json:expect:entries (
    |      'OBJECT',
    |      ['item'],
    |      ~>$
    |    ),
    |    %expect:only-entries (
    |      ['item'],
    |      ~>$
    |    ),
    |    :$.item (
    |      #{'item must match by type and have at least 2 items'},
    |      %match:min-type (
    |        json:[{"a":100},{"a":200},{"a":300}],
    |        ~>$.item,
    |        json:{"min":2}
    |      ),
    |      %for-each (
    |        ~>$.item,
    |        :$.item[*] (
    |          %json:expect:entries (
    |            'OBJECT',
    |            ['a'],
    |            ~>$.item[*]
    |          ),
    |          %expect:only-entries (
    |            ['a'],
    |            ~>$.item[*]
    |          ),
    |          :$.item[*].a (
    |            #{'a must match by type'},
    |            %match:type (
    |              json:100,
    |              ~>$.item[*].a,
    |              json:{}
    |            )
    |          )
    |        )
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }
}
