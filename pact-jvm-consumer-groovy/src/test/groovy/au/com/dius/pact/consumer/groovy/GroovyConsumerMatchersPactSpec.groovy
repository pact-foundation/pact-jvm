package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonOutput
import groovyx.net.http.RESTClient
import spock.lang.Specification

import static groovyx.net.http.ContentType.JSON

class GroovyConsumerMatchersPactSpec extends Specification {

  @SuppressWarnings('MethodSize')
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
      withBody(mimeType: JSON.toString()) {
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

        ts(timestamp)
        timestamp = timestamp('yyyy/MM/dd - HH:mm:ss.S')

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
      withBody(mimeType: JSON.toString()) {
        name(~/\w+/, 'harry')
      }
    }

    when:
    PactVerificationResult result = matcherService.runTest {
      def client = new RESTClient(it.url)
      def response = client.put(requestContentType: JSON, body: [
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
          ]
        ]
      )

      assert response.status == 200
      assert response.data == [name: 'harry']
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
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
    PactVerificationResult result = matcherService.runTest {
      def client = new RESTClient(it.url)
      def response = client.get(query: [a: '100', b: 'Z'])

      assert response.status == 200
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
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
    PactVerificationResult result = matcherService.runTest {
      def client = new RESTClient(it.url)
      def response = client.put(requestContentType: JSON, body: JsonOutput.toJson([
        valueA: 100, valueB: 'AZB', valueC: null]))

      assert response.status == 200
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }
}
