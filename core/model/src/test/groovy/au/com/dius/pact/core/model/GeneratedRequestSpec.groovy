package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import spock.lang.Specification

class GeneratedRequestSpec extends Specification {
  private Generators generators
  private Request request

  def setup() {
    generators = new Generators()
    generators.addGenerator(Category.PATH, new RandomIntGenerator(400, 499))
    generators.addGenerator(Category.HEADER, 'A', UuidGenerator.INSTANCE)
    generators.addGenerator(Category.QUERY, 'A', UuidGenerator.INSTANCE)
    generators.addGenerator(Category.BODY, '$.a', new RandomStringGenerator())
    request = new Request(generators: generators)
  }

  def 'applies path generator for path to the copy of the request'() {
    given:
    request.path = '/path'

    when:
    def generated = request.generatedRequest()

    then:
    generated.path != request.path
  }

  def 'applies header generator for headers to the copy of the request'() {
    given:
    request.headers = [A: 'a', B: 'b']

    when:
    def generated = request.generatedRequest()

    then:
    generated.headers.A != 'a'
    generated.headers.B == 'b'
  }

  def 'applies query generator for query parameters to the copy of the request'() {
    given:
    request.query = [A: ['a', 'b'], B: ['b']]

    when:
    def generated = request.generatedRequest()

    then:
    generated.query.A != ['a', 'b']
    generated.query.A.size() == 2
    generated.query.B == ['b']
  }

  def 'applies body generators for body values to the copy of the request'() {
    given:
    def body = [a: 'A', b: 'B']
    request.body = OptionalBody.body(Json.INSTANCE.prettyPrint(body).bytes)

    when:
    def generated = request.generatedRequest()
    def generatedBody = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(generated.body.valueAsString()))

    then:
    generatedBody.a != 'A'
    generatedBody.b == 'B'
  }

}
