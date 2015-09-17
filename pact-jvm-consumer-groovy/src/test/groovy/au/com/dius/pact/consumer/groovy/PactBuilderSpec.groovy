package au.com.dius.pact.consumer.groovy

@SuppressWarnings('UnusedImport')
import scala.None$
import spock.lang.Specification

class PactBuilderSpec extends Specification {

  private final aliceService = new PactBuilder()

  void setup() {
    aliceService {
      serviceConsumer 'Consumer'
      hasPactWith 'Alice Service'
      port 1234
    }
  }

  def 'should not define providerState when no given()'() {
    given:
    aliceService {
      uponReceiving('a retrieve Mallory request')
      withAttributes(method: 'get', path: '/mallory')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }

    when:
    aliceService.buildInteractions()

    then:
    aliceService.interactions.size() == 1
    aliceService.interactions[0].providerState == None$.empty()
  }

  def 'allows matching on paths'() {
    given:
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: ~'/mallory/[0-9]+')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }

    when:
    aliceService.buildInteractions()

    then:
    aliceService.interactions.size() == 1
    aliceService.interactions[0].request.path =~ '/mallory/[0-9]+'
    aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  def 'allows using the defined matcher on paths'() {
    given:
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: regexp(~'/mallory/[0-9]+', '/mallory/1234567890'))
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }

    when:
    aliceService.buildInteractions()

    then:
    aliceService.interactions.size() == 1
    aliceService.interactions[0].request.path == '/mallory/1234567890'
    aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  def 'allows matching on headers'() {
    given:
    aliceService {
      uponReceiving('a request to match a header')
      withAttributes(method: 'get', path: '/headers', headers: [MALLORY: ~'mallory:[0-9]+'])
      willRespondWith(
        status: 200,
        headers: ['Content-Type': regexp('text/.*', 'text/html')],
        body: '"That is some good Mallory."'
      )
    }

    when:
    aliceService.buildInteractions()
    def firstInteraction = aliceService.interactions[0]

    then:
    aliceService.interactions.size() == 1

    firstInteraction.request.headers.get().apply('MALLORY') =~ 'mallory:[0-9]+'
    firstInteraction.request.matchingRules.get().apply('$.headers.MALLORY').apply('regex') == 'mallory:[0-9]+'
    firstInteraction.response.headers.get().apply('Content-Type') == 'text/html'
    firstInteraction.response.matchingRules.get().apply('$.headers.Content-Type').apply('regex') == 'text/.*'
  }

  def 'allow arrays as the root of the body'() {
    given:
    aliceService {
      uponReceiving('a request to get a array response')
      withAttributes(method: 'get', path: '/array')
      willRespondWith(status: 200)
      withBody([
        1, 2, 3
      ])
    }

    when:
    aliceService.buildInteractions()
    def firstInteraction = aliceService.interactions[0]

    then:
    aliceService.interactions.size() == 1

    firstInteraction.response.body.get() == '[\n' +
      '    1,\n' +
      '    2,\n' +
      '    3\n' +
      ']'
  }

  def 'allow arrays of objects as the root of the body'() {
    given:
    aliceService {
      uponReceiving('a request to get a array of objects response')
      withAttributes(method: 'get', path: '/array')
      willRespondWith(status: 200)
      withBody([
        {
          id identifier(1)
          name 'item1'
        }, {
          id identifier(2)
          name 'item2'
        }
      ])
    }

    when:
    aliceService.buildInteractions()
    def firstInteraction = aliceService.interactions[0]

    then:
    aliceService.interactions.size() == 1

    firstInteraction.response.body.get() == '[\n' +
      '    {\n' +
      '        "id": 1,\n' +
      '        "name": "item1"\n' +
      '    },\n' +
      '    {\n' +
      '        "id": 2,\n' +
      '        "name": "item2"\n' +
      '    }\n' +
      ']'
    firstInteraction.response.matchingRules.get().keySet().toString() == 'Set($.body[0].id, $.body[1].id)'
  }

  def 'allow like matcher as the root of the body'() {
    given:
    aliceService {
      uponReceiving('a request to get a like array of objects response')
      withAttributes(method: 'get', path: '/array')
      willRespondWith(status: 200)
      withBody eachLike {
        id identifier(1)
        name 'item1'
      }
    }

    when:
    aliceService.buildInteractions()
    def firstInteraction = aliceService.interactions[0]

    then:
    aliceService.interactions.size() == 1

    firstInteraction.response.body.get() == '[\n' +
      '    {\n' +
      '        "id": 1,\n' +
      '        "name": "item1"\n' +
      '    }\n' +
      ']'
    firstInteraction.response.matchingRules.get().keySet().toString() == 'Set($.body, $.body[*].id)'
  }

  def 'pretty prints bodies by default'() {
    given:
    aliceService {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/', body: [
        name: 'harry',
        surname: 'larry',
        position: 'staff',
        happy: true
      ])
      willRespondWith(status: 200, body: [name: 'harry'])
    }

    when:
    aliceService.buildInteractions()
    def request = aliceService.interactions.first().request
    def response = aliceService.interactions.first().response

    then:
    request.body.get() == '''|{
                             |    "name": "harry",
                             |    "surname": "larry",
                             |    "position": "staff",
                             |    "happy": true
                             |}'''.stripMargin()
    response.body.get() == '''|{
                              |    "name": "harry"
                              |}'''.stripMargin()
  }

  def 'pretty prints bodies if pretty print is set to true'() {
    given:
    aliceService {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/', body: [
        name: 'harry',
        surname: 'larry',
        position: 'staff',
        happy: true
      ], prettyPrint: true)
      willRespondWith(status: 200, body: [name: 'harry'], prettyPrint: true)
    }

    when:
    aliceService.buildInteractions()
    def request = aliceService.interactions.first().request
    def response = aliceService.interactions.first().response

    then:
    request.body.get() == '''|{
                             |    "name": "harry",
                             |    "surname": "larry",
                             |    "position": "staff",
                             |    "happy": true
                             |}'''.stripMargin()
    response.body.get() == '''|{
                              |    "name": "harry"
                              |}'''.stripMargin()
  }

  def 'does not pretty print bodies if pretty print is set to false'() {
    given:
    aliceService {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/', body: [
        name: 'harry',
        surname: 'larry',
        position: 'staff',
        happy: true
      ], prettyPrint: false)
      willRespondWith(status: 200, body: [name: 'harry'], prettyPrint: false)
    }

    when:
    aliceService.buildInteractions()
    def request = aliceService.interactions.first().request
    def response = aliceService.interactions.first().response

    then:
    request.body.get() == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.get() == '{"name":"harry"}'
  }

  def 'does not pretty print bodies if the mimetype corresponds to one that requires compact bodies'() {
    given:
    aliceService {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/', body: [
        name: 'harry',
        surname: 'larry',
        position: 'staff',
        happy: true
      ], headers: ['Content-Type': 'application/x-thrift+json'])
      willRespondWith(status: 200, body: [name: 'harry'], headers: ['Content-Type': 'application/x-thrift+json'])
    }

    when:
    aliceService.buildInteractions()
    def request = aliceService.interactions.first().request
    def response = aliceService.interactions.first().response

    then:
    request.body.get() == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.get() == '{"name":"harry"}'
  }
}
