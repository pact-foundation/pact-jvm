package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import spock.lang.Specification

import static groovyx.net.http.ContentTypes.JSON

class GroovyConsumerMatchersPactSpec extends Specification {

  @SuppressWarnings(['MethodSize', 'AbcMetric'])
  def 'example V3 spec test'() {
    given:
    def matcherService = new PactBuilder()
    matcherService {
      serviceConsumer 'MatcherConsumer'
      hasPactWith 'MatcherService'
    }

    matcherService {
      uponReceiving('a request')
      withAttributes(method: 'put', path: '/')
      withBody(mimeType: ContentTypes.JSON[0]) {
        name(~/\w+/, 'harry')
        surname includesStr('larry')
        position regexp(~/staff|contractor/, 'staff')
        happy(true)

        hexCode(hexValue)
        hexCode2 hexValue('01234AB')
        id(identifier)
        id2 identifier(1234567890)
        localAddress(ipAddress)
        localAddress2 ipAddress('192.169.0.2')
        age(100)
        age2(integer)
        salary real

        ts(datetime)
        timestamp = datetime('yyyy/MM/dd - HH:mm:ss.SSS')
        nextReview = dateExpression('today + 1 year')

        values([1, 2, 3, numeric])

        role {
          name('admin')
          id(uuid)
          kind {
            id equalTo(100)
          }
          dob date('MM/dd/yyyy')
        }

        roles eachLike {
          name('dev')
          id(uuid)
        }
      }
      willRespondWith(status: 200)
      withBody(mimeType: ContentTypes.JSON[0]) {
        name(~/\w+/, 'harry')
      }
    }

    when:
    PactVerificationResult result = matcherService.runTest { server, context ->
      def client = HttpBuilder.configure {
        request.uri = server.url
      }
      def resp = client.put(FromServer) {
        request.contentType = JSON[0]
        request.body = JsonOutput.toJson([
                'name': 'harry',
                'surname': 'larry',
                'position': 'staff',
                'happy': true,
                'hexCode': '9d1883afcd',
                'hexCode2': '01234AB',
                'id': 6444667731,
                'id2': 1234567890,
                'localAddress': '127.0.0.1',
                'localAddress2': '192.169.0.2',
                'age': 100,
                'age2': 9817343207,
                'salary': 59458983.55,
                'ts': '2015-12-05T16:24:28',
                'timestamp': '2015/12/05 - 16:24:28.429',
                'values': [
                        1,
                        2,
                        3,
                        9232527554
                ],
                'role': [
                        'name': 'admin',
                        'id': '7a97e929-c5b1-43cf-9b2c-295e9d4fa3cd',
                        'kind': [
                                'id': 100
                        ],
                        'dob': '12/05/2015'
                ],
                'roles': [
                        [
                                'name': 'dev',
                                'id': '3590e5cf-8777-4d30-be4c-dac824765b9b'
                        ]
                ],
                nextReview: '2001-01-01'
        ])
        response.parser(JSON) { config, r ->
          r
        }
      }

      assert resp.statusCode == 200
      assert new JsonSlurper().parse(resp.inputStream) == [name: 'harry']
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'matching on query parameters'() {
    given:
    def matcherService = new PactBuilder()
    matcherService {
      serviceConsumer 'MatcherConsumer2'
      hasPactWith 'MatcherService'
      port 1235
    }

    matcherService {
      uponReceiving('a request to match query parameters')
      withAttributes(method: 'get', path: '/', query: [a: ~/\d+/, b: regexp('[A-Z]', 'X')])
      willRespondWith(status: 200)
    }

    when:
    PactVerificationResult result = matcherService.runTest { server ->
      def client = HttpBuilder.configure {
        request.uri = server.url
      }
      def resp = client.get(FromServer) {
        request.uri.query = [a: '100', b: 'Z']
        response.success { r, v ->
          r
        }
      }

      assert resp.statusCode == 200
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'matching with and and or'() {
    given:
    def matcherService = new PactBuilder()
    matcherService {
      serviceConsumer 'MatcherConsumer2'
      hasPactWith 'MatcherService'
      port 1235
    }

    matcherService {
      uponReceiving('a request to match with and and or')
      withAttributes(method: 'put', path: '/')
      withBody {
        valueA 100
        valueB and('AB', includesStr('A'), includesStr('B'))
        valueC or('2000-01-01', date(), nullValue())
      }
      willRespondWith(status: 200)
    }

    when:
    PactVerificationResult result = matcherService.runTest { server ->
      def client = HttpBuilder.configure {
        request.uri = server.url
      }
      def resp = client.put(FromServer) {
        request.contentType = JSON[0]
        request.body = JsonOutput.toJson([
                valueA: 100, valueB: 'AZB', valueC: null])
        response.success { resp, v ->
          resp
        }
      }

      assert resp.statusCode == 200
    }

    then:
    result instanceof PactVerificationResult.Ok
  }
}
