package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Specification

@SuppressWarnings(['MethodSize', 'AbcMetric', 'LineLength'])
class ResponseMatchingSpec extends Specification {

  def 'simple response match - status ok'() {
    given:
    def expectedResponse = new HttpResponse(200)
    def actualResponse = new HttpResponse(200)

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    result.matchedOk()
    result.status == null
  }

  def 'simple response match - status mismatch'() {
    given:
    def expectedResponse = new HttpResponse(200)
    def actualResponse = new HttpResponse(404)

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)

    then:
    plan.planRoot.nodeType.label == 'response'
    plan.fetchNode([':response', ':status']) != null

    when:
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    !result.matchedOk()
    result.status != null
    result.status.description().contains('200') && result.status.description().contains('404')
  }

  def 'response match with header'() {
    given:
    def expectedResponse = new HttpResponse(200, ['Content-Type': ['application/json']])
    def actualResponse = new HttpResponse(200, ['Content-Type': ['application/json']])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    result.matchedOk()
    result.headers.every { it.result.empty }
  }

  def 'response match with header mismatch'() {
    given:
    def expectedResponse = new HttpResponse(200, ['Content-Type': ['application/json']])
    def actualResponse = new HttpResponse(200, ['Content-Type': ['text/plain']])

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    !result.matchedOk()
    result.headers.any { !it.result.empty }
  }

  def 'response match with JSON body'() {
    given:
    def expectedResponse = new HttpResponse(200, [:],
      OptionalBody.body('{"a": 1}', ContentType.JSON))
    def actualResponse = new HttpResponse(200, [:],
      OptionalBody.body('{"a": 1}', ContentType.JSON))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    result.matchedOk()
  }

  def 'response match with JSON body mismatch'() {
    given:
    def expectedResponse = new HttpResponse(200, [:],
      OptionalBody.body('{"a": 1}', ContentType.JSON))
    def actualResponse = new HttpResponse(200, [:],
      OptionalBody.body('{"a": 2}', ContentType.JSON))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context)
    def result = executedPlan.intoResponseMatchResult()

    then:
    !result.matchedOk()
    result.body.bodyResults.any { !it.result.empty }
  }

  def 'response plan structure'() {
    given:
    def expectedResponse = new HttpResponse(200, ['X-Custom': ['value']],
      OptionalBody.body('test', ContentType.TEXT_PLAIN))

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [],
      new au.com.dius.pact.core.model.HttpRequest(), expectedResponse)
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)

    when:
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)

    then:
    plan.fetchNode([':response', ':status']) != null
    plan.fetchNode([':response', ':headers']) != null
    plan.fetchNode([':response', ':body']) != null
    plan.fetchNode([':response', ':headers', ':X-Custom']) != null
  }
}
