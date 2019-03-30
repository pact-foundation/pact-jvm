package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

class GeneratedResponseSpec extends Specification {
  private Generators generators
  private Response response

  def setup() {
    generators = new Generators()
    generators.addGenerator(Category.STATUS, new RandomIntGenerator(400, 499))
    generators.addGenerator(Category.HEADER, 'A', UuidGenerator.INSTANCE)
    generators.addGenerator(Category.BODY, '$.a', new RandomStringGenerator())
    response = new Response(generators: generators)
  }

  def 'applies status generator for status to the copy of the response'() {
    given:
    response.status = 200

    when:
    def generated = response.generatedResponse()

    then:
    generated.status >= 400 && generated.status < 500
  }

  def 'applies header generator for headers to the copy of the response'() {
    given:
    response.headers = [A: 'a', B: 'b']

    when:
    def generated = response.generatedResponse()

    then:
    generated.headers.A != 'a'
    generated.headers.B == 'b'
  }

  def 'applies body generators for body values to the copy of the response'() {
    given:
    def body = [a: 'A', b: 'B']
    response.body = OptionalBody.body(JsonOutput.toJson(body).bytes)

    when:
    def generated = response.generatedResponse()
    def generatedBody = new JsonSlurper().parseText(generated.body.valueAsString())

    then:
    generatedBody.a != 'A'
    generatedBody.b == 'B'
  }

}
