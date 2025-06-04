package au.com.dius.pact.core.matchers.engine.resolvers

import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import spock.lang.Specification

class HttpRequestValueResolverSpec extends Specification {
  def 'resolve default values - #path'() {
    given:
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def matchingRules = new MatchingRuleCategory('test')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, new MatchingContext(matchingRules, false), config)

    expect:
    new HttpRequestValueResolver(new HttpRequest()).resolve(new DocPath(path), context).unwrap() == result

    where:

    path        | result
    '$.method'  | new NodeValue.STRING('GET')
    '$.path'    | new NodeValue.STRING('/')
    '$.query'   | new NodeValue.MMAP([:])
    '$.headers' | new NodeValue.MMAP([:])
  }

  def 'http request resolve failures'() {
    given:
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def matchingRules = new MatchingRuleCategory('test')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, new MatchingContext(matchingRules, false), config)
    def request = new HttpRequest()
    def resolver = new HttpRequestValueResolver(request)

    expect:
    resolver.resolve(DocPath.root(), context).errorValue() == '$ is not valid for a HTTP request'
    resolver.resolve(new DocPath('$.blah'), context).errorValue() == '$.blah is not valid for a HTTP request'
  }
}
