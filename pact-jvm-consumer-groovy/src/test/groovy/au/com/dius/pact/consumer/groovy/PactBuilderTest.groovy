package au.com.dius.pact.consumer.groovy

import org.junit.Before
import org.junit.Test
@SuppressWarnings('UnusedImport')
import scala.None$

class PactBuilderTest {

  private final aliceService = new PactBuilder()

  @Before
  void setup() {
    aliceService {
      serviceConsumer 'Consumer'
      hasPactWith 'Alice Service'
      port 1234
    }
  }

  @Test
  void 'should not define providerState when no given()'() {
    aliceService {
      uponReceiving('a retrieve Mallory request')
      withAttributes(method: 'get', path: '/mallory')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].providerState == None$.empty()
  }

  @Test
  void 'allows matching on paths'() {
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: ~'/mallory/[0-9]+')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].request.path =~ '/mallory/[0-9]+'
    assert aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows using the defined matcher on paths'() {
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: regexp(~'/mallory/[0-9]+', '/mallory/1234567890'))
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].request.path == '/mallory/1234567890'
    assert aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows matching on headers'() {
    aliceService {
      uponReceiving('a request to match a header')
      withAttributes(method: 'get', path: '/headers', headers: [MALLORY: ~'mallory:[0-9]+'])
      willRespondWith(
        status: 200,
        headers: ['Content-Type': regexp('text/.*', 'text/html')],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1

    def firstInteraction = aliceService.interactions[0]
    assert firstInteraction.request.headers.get().apply('MALLORY') =~ 'mallory:[0-9]+'
    assert firstInteraction.request.matchingRules.get().apply('$.headers.MALLORY').apply('regex') == 'mallory:[0-9]+'
    assert firstInteraction.response.headers.get().apply('Content-Type') == 'text/html'
    assert firstInteraction.response.matchingRules.get().apply('$.headers.Content-Type').apply('regex') == 'text/.*'
  }

  @Test
  void 'allow arrays as the root of the body'() {
    aliceService {
      uponReceiving('a request to get a array response')
      withAttributes(method: 'get', path: '/array')
      willRespondWith(status: 200)
      withBody([
        1, 2, 3
      ])
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1

    def firstInteraction = aliceService.interactions[0]
    assert firstInteraction.response.body.get() == '[\n' +
      '    1,\n' +
      '    2,\n' +
      '    3\n' +
      ']'
  }

  @Test
  void 'allow arrays of objects as the root of the body'() {
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
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1

    def firstInteraction = aliceService.interactions[0]
    assert firstInteraction.response.body.get() == '[\n' +
      '    {\n' +
      '        "id": 1,\n' +
      '        "name": "item1"\n' +
      '    },\n' +
      '    {\n' +
      '        "id": 2,\n' +
      '        "name": "item2"\n' +
      '    }\n' +
      ']'
    assert firstInteraction.response.matchingRules.get().keySet().toString() == 'Set($.body[0].id, $.body[1].id)'
  }

  @Test
  void 'allow like matcher as the root of the body'() {
    aliceService {
      uponReceiving('a request to get a like array of objects response')
      withAttributes(method: 'get', path: '/array')
      willRespondWith(status: 200)
      withBody eachLike {
        id identifier(1)
        name 'item1'
      }
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1

    def firstInteraction = aliceService.interactions[0]
    assert firstInteraction.response.body.get() == '[\n' +
      '    {\n' +
      '        "id": 1,\n' +
      '        "name": "item1"\n' +
      '    }\n' +
      ']'
    assert firstInteraction.response.matchingRules.get().keySet().toString() == 'Set($.body, $.body[*].id)'
  }
}
