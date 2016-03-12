package au.com.dius.pact.model

import spock.lang.Specification

class PactWriterSpec extends Specification {

  def 'when writing V2 spec, query parameters must be encoded appropriately'() {
    given:
    def pact = new RequestResponsePact(interactions: [
      new RequestResponseInteraction(request: new Request(method: 'GET', query: [a: ['b=c&d']]),
        response: new Response())
    ])

    when:
    def result = PactWriter.toMap(pact, PactSpecVersion.V2)

    then:
    result.interactions.first().request.query == 'a=b%3Dc%26d'
  }

}
