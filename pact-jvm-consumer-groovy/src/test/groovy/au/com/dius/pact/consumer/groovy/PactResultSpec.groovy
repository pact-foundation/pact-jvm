package au.com.dius.pact.consumer.groovy

import groovyx.net.http.RESTClient
import spock.lang.Specification

class PactResultSpec extends Specification {

    def 'case when the test passes and the pact is verified'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'
            port 1234

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
        testService.runTestAndVerify {
            def client = new RESTClient('http://localhost:1234/')
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
            port 1235

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

      when:
        def response
        testService.runTestAndVerify {
          def client = new RESTClient('http://localhost:1235/')
          response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')

          assert response.status == 201
        }

      then:
      def e = thrown(PactFailedException)
      e.message.contains('response.status == 201')
    }

    def 'case when the test fails and the pact has a mismatch'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'
            port 1236

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

      when:
        def response
        testService.runTestAndVerify {
            def client = new RESTClient('http://localhost:1236/')
            response = client.get(path: '/path', query: [status: 'bad', name: 'ron'],
                requestContentType: 'application/json')
            assert response.status == 200
        }

      then:
         def e = thrown(PactFailedException)
         e.message.contains(
            'QueryMismatch(status,good,bad,Some(Expected \'good\' but received \'bad\' for query parameter ' +
              '\'status\'),status)')
    }

    def 'case when the test passes and there is a missing request'() {
      given:
        def testService = new PactBuilder().build  {
            serviceConsumer 'Consumer'
            hasPactWith 'Test Service'
            port 1237

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
        testService.runTestAndVerify {
            def client = new RESTClient('http://localhost:1237/')
            def response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')
            assert response.status == 200
        }

      then:
        def e = thrown(PactFailedException)
        e.message.contains('''|The following requests were not received:
          |Interaction: a valid post request
          |\tin states None
          |request:
          |\tmethod: post
          |\tpath: /path'''.stripMargin())
    }
}
