package au.com.dius.pact.model

import spock.lang.Specification

class RequestResponsePactSpec extends Specification {

  def 'when writing V2 spec, query parameters must be encoded appropriately'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [
      new RequestResponseInteraction(request: new Request(method: 'GET', query: [a: ['b=c&d']]),
        response: new Response())
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V2)

    then:
    result.interactions.first().request.query == 'a=b%3Dc%26d'
  }

  def 'should handle body types other than JSON'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [
      new RequestResponseInteraction(request: new Request(method: 'PUT',
        body: OptionalBody.body('<?xml version="1.0"><root/>'),
        headers: ['Content-Type': 'application/xml']),
        response: new Response(body: OptionalBody.body('Ok, no prob'), headers: ['Content-Type': 'text/plain']))
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V3)

    then:
    result.interactions.first().request.body == '<?xml version="1.0"><root/>'
    result.interactions.first().response.body == 'Ok, no prob'
  }

  def 'does not lose the scale for decimal numbers'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [
      new RequestResponseInteraction(request: new Request(method: 'GET'),
        response: new Response(body: OptionalBody.body('{"value": 1234.0}'),
          headers: ['Content-Type': 'application/json']))
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V3)

    then:
    result.interactions.first().response.body.toString() == '{value=1234.0}'
  }

}
