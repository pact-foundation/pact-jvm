package au.com.dius.pact.model

import au.com.dius.pact.model.generators.Category
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.generators.RandomIntGenerator
import au.com.dius.pact.model.generators.UuidGenerator
import spock.lang.Specification

class GeneratedResponseSpec extends Specification {
  private Generators generators
  private Response response

  def setup() {
    generators = new Generators()
    generators.addGenerator(Category.STATUS, new RandomIntGenerator(400, 499))
    generators.addGenerator(Category.HEADER, 'A', new UuidGenerator())
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

}
