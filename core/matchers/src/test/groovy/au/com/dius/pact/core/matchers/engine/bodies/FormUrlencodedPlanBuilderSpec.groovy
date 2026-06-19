package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import spock.lang.Specification

class FormUrlencodedPlanBuilderSpec extends Specification {
  PlanMatchingContext context
  V4Pact pact
  V4Interaction.SynchronousHttp interaction

  def setup() {
    pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    interaction = new V4Interaction.SynchronousHttp('test interaction')
    context = new PlanMatchingContext(pact, interaction, new MatchingConfiguration(false, false, true, false))
  }

  def 'supports form-urlencoded content type only'() {
    expect:
    FormUrlencodedPlanBuilder.INSTANCE.supportsType(contentType) == supported

    where:
    contentType                                           | supported
    new ContentType('application/x-www-form-urlencoded')  | true
    ContentType.JSON                                      | false
  }

  def 'build plan uses form parse equality and multi-value expected nodes'() {
    when:
    def node = FormUrlencodedPlanBuilder.INSTANCE.buildPlan('a=1&a=2&b=two+words'.bytes, context)
    def pretty = new StringBuilder()
    node.prettyForm(pretty, 0)

    then:
    pretty.toString().contains('%form:parse')
    pretty.toString().contains(':$.a')
    pretty.toString().contains("['1', '2']")
    pretty.toString().contains('%match:equality')
    pretty.toString().contains('The following expected form parameters were missing: ')
    pretty.toString().contains('The following form parameters were not expected: ')
  }

  def 'build plan uses matcher nodes when form matchers are defined'() {
    given:
    interaction.request.matchingRules
      .addCategory('body')
      .addRule('$.id', new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER))
      .addRule('$.tags', new EachValueMatcher(new MatchingRuleDefinition(
        '1',
        new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER),
        null,
        'matching(number, 1)'
      )))
    context = new PlanMatchingContext(pact, interaction, new MatchingConfiguration(false, false, true, false)).forBody()

    when:
    def node = FormUrlencodedPlanBuilder.INSTANCE.buildPlan('id=100&tags=1&tags=2'.bytes, context)
    def pretty = new StringBuilder()
    node.prettyForm(pretty, 0)

    then:
    pretty.toString().contains('%match:number')
    pretty.toString().contains('%match:each-value')
    pretty.toString().contains(':$.tags')
  }
}
