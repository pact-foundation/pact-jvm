package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.v4.MessageContents
import spock.lang.Specification

@SuppressWarnings(['MethodSize', 'AbcMetric', 'LineLength'])
class MessageMatchingSpec extends Specification {

  def pact
  def config

  def setup() {
    pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    config = new MatchingConfiguration(false, false, true, false)
  }

  PlanMatchingContext contextFor(MessageContents expectedContents) {
    def interaction = new V4Interaction.AsynchronousMessage(null, 'test interaction', expectedContents)
    new PlanMatchingContext(pact, interaction, config)
  }

  def 'message match - plain text body ok'() {
    given:
    def expected = new MessageContents(OptionalBody.body('Hello!', ContentType.TEXT_PLAIN))
    def actual = new MessageContents(OptionalBody.body('Hello!', ContentType.TEXT_PLAIN))
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    result.matchedOk()
    result.contents.matchedOk()
    result.metadata.empty
  }

  def 'message match - plain text body mismatch'() {
    given:
    def expected = new MessageContents(OptionalBody.body('Hello!', ContentType.TEXT_PLAIN))
    def actual = new MessageContents(OptionalBody.body('World!', ContentType.TEXT_PLAIN))
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    !result.matchedOk()
    !result.contents.matchedOk()
  }

  def 'message match - JSON body ok'() {
    given:
    def expected = new MessageContents(OptionalBody.body('{"key": "value"}', ContentType.JSON))
    def actual = new MessageContents(OptionalBody.body('{"key": "value"}', ContentType.JSON))
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    result.matchedOk()
  }

  def 'message match - JSON body mismatch'() {
    given:
    def expected = new MessageContents(OptionalBody.body('{"key": "value"}', ContentType.JSON))
    def actual = new MessageContents(OptionalBody.body('{"key": "other"}', ContentType.JSON))
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    !result.matchedOk()
    result.contents.bodyResults.any { !it.result.empty }
  }

  def 'message match - metadata ok'() {
    given:
    def expected = new MessageContents(OptionalBody.missing(), ['contentType': 'application/json'])
    def actual = new MessageContents(OptionalBody.missing(), ['contentType': 'application/json'])
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    result.matchedOk()
    result.metadata.every { it.result.empty }
  }

  def 'message match - metadata mismatch'() {
    given:
    def expected = new MessageContents(OptionalBody.missing(), ['contentType': 'application/json'])
    def actual = new MessageContents(OptionalBody.missing(), ['contentType': 'text/plain'])
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    !result.matchedOk()
    result.metadata.any { !it.result.empty }
  }

  def 'message match - metadata missing key'() {
    given:
    def expected = new MessageContents(OptionalBody.missing(), ['contentType': 'application/json'])
    def actual = new MessageContents(OptionalBody.missing(), [:])
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeMessagePlan(plan, actual, context)
    def result = executedPlan.intoMessageMatchResult()

    then:
    !result.matchedOk()
  }

  def 'message plan structure'() {
    given:
    def expected = new MessageContents(
      OptionalBody.body('test', ContentType.TEXT_PLAIN),
      ['contentType': 'text/plain']
    )
    def context = contextFor(expected)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildMessagePlan(expected, context)

    then:
    plan.fetchNode([':message', ':body']) != null
    plan.fetchNode([':message', ':metadata']) != null
    plan.fetchNode([':message', ':metadata', ':contentType']) != null
  }
}
