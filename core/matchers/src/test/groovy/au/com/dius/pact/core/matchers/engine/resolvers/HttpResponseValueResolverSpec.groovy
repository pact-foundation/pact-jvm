package au.com.dius.pact.core.matchers.engine.resolvers

import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Specification

class HttpResponseValueResolverSpec extends Specification {

  PlanMatchingContext context
  HttpResponse response

  def setup() {
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    context = new PlanMatchingContext(pact, interaction, config)
    response = new HttpResponse()
  }

  def 'resolve default values - #path'() {
    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath(path), context).unwrap() == result

    where:
    path          | result
    '$.status'    | new NodeValue.UINT(200)
    '$.headers'   | new NodeValue.MMAP([:])
  }

  def 'resolve status code'() {
    given:
    response = new HttpResponse(404)

    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath('$.status'), context).unwrap() ==
      new NodeValue.UINT(404)
  }

  def 'resolve headers'() {
    given:
    response = new HttpResponse(200, ['Content-Type': ['application/json']])

    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath('$.headers'), context).unwrap() ==
      new NodeValue.MMAP(['content-type': ['application/json']])
    new HttpResponseValueResolver(response).resolve(new DocPath('$.headers.content-type'), context).unwrap() ==
      new NodeValue.STRING('application/json')
  }

  def 'resolve content-type'() {
    given:
    response = new HttpResponse(200, ['Content-Type': ['application/json']])

    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath('$.content-type'), context).unwrap() ==
      new NodeValue.STRING('application/json')
  }

  def 'resolve body'() {
    given:
    def body = 'Hello!'
    response = new HttpResponse(200, [:], au.com.dius.pact.core.model.OptionalBody.body(body.bytes))

    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath('$.body'), context).unwrap() ==
      new NodeValue.BARRAY(body.bytes)
  }

  def 'resolve missing body returns NULL'() {
    expect:
    new HttpResponseValueResolver(response).resolve(new DocPath('$.body'), context).unwrap() ==
      NodeValue.NULL.INSTANCE
  }

  def 'http response resolve failures'() {
    given:
    def resolver = new HttpResponseValueResolver(response)

    expect:
    resolver.resolve(DocPath.root(), context).errorValue() == '$ is not valid for a HTTP response'
    resolver.resolve(new DocPath('$.blah'), context).errorValue() == '$.blah is not valid for a HTTP response'
  }
}
