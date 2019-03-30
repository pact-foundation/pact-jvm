package au.com.dius.pact.consumer.groovy

import groovyx.net.http.RESTClient
import spock.lang.Specification

class PactResultSpec extends Specification {

    def 'case when the test passes and the pact is verified'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

      when:
        def response
        def data
        testService.runTestAndVerify { mockServer, context ->
            def client = new RESTClient(mockServer.url)
            response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')
            data = response.data
        }

      then:
        response.status == 200
        data == [status: 'isGood']
    }

    def 'case when the test fails and the pact is verified'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

      when:
        def response
        testService.runTestAndVerify { mockServer, context ->
          def client = new RESTClient(mockServer.url)
          response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')

          assert response.status == 201
        }

      then:
      def e = thrown(AssertionError)
      e.message.contains('Pact Test function failed with an exception: Condition not satisfied:\n' +
        '\n' +
        'response.status == 201\n' +
        '|        |      |\n' +
        '|        200    false')
    }

    def 'case when the test fails and the pact has a mismatch'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

      when:
        def response
        testService.runTestAndVerify { mockServer, context ->
            def client = new RESTClient(mockServer.url)
            response = client.get(path: '/path', query: [status: 'bad', name: 'ron'],
                requestContentType: 'application/json')
            assert response.status == 200
        }

      then:
         def e = thrown(AssertionError)
         e.message.contains(
            'QueryMismatch(queryParameter=status, expected=good, actual=bad, mismatch=Expected \'good\' ' +
              'but received \'bad\' for query parameter \'status\', path=status)')
    }

    @SuppressWarnings('LineLength')
    def 'case when the test passes and there is a missing request'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }

            uponReceiving('a valid post request')
            withAttributes(method: 'post', path: '/path')
            withBody {
                status 'isGood'
            }
            willRespondWith(status: 200)
        }

      when:
        testService.runTestAndVerify { mockServer, context ->
            def client = new RESTClient(mockServer.url)
            def response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')
            assert response.status == 200
        }

      then:
        def e = thrown(PactFailedException)
        e.message.contains('The following requests were not received:\n' +
          '\tmethod: post\n' +
          '\tpath: /path\n' +
          '\tquery: [:]\n' +
          '\theaders: [Content-Type:[application/json]]\n' +
          '\tmatchers: MatchingRules(rules={body=Category(name=body, matchingRules={}), path=Category(name=path, matchingRules={})})\n' +
          '\tgenerators: Generators(categories={})\n' +
          '\tbody: PRESENT({\n' +
          '    "status": "isGood"\n' +
          '})')
    }
}
