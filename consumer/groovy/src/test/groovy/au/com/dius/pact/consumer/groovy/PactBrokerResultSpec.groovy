package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactBrokerResultSpec extends Specification {

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
        def resp
        def data
        testService.runTestAndVerify { mockServer, context ->
            def client = new SimpleHttp(mockServer.url)
            resp = client.get('/path', [status: 'good', name: 'ron'])
            data = new JsonSlurper().parse(resp.inputStream)
        }

      then:
        resp.statusCode == 200
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
        def resp
        testService.runTestAndVerify { mockServer, context ->
          def client = new SimpleHttp(mockServer.url)
          resp = client.get('/path', [status: 'good', name: 'ron'])
          assert resp.statusCode == 201
        }

      then:
      def e = thrown(AssertionError)
      e.message.contains('Pact Test function failed with an exception: Condition not satisfied:\n' +
        '\n' +
        'resp.statusCode == 201\n' +
        '|    |          |\n' +
        '|    200        false')
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
            def client = new SimpleHttp(mockServer.url)
            response = client.get('/path', [status: 'bad', name: 'ron'])
            assert response.statusCode == 200
        }

      then:
         def e = thrown(AssertionError)
         e.message.contains(
            'QueryMismatch(queryParameter=status, expected=good, actual=bad, mismatch=Expected \'good\'' +
              ' but received \'bad\' for query parameter \'status\', path=status)')
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
            def client = new SimpleHttp(mockServer.url)
            def resp = client.get('/path', [status: 'good', name: 'ron'])
            assert resp.statusCode == 200
        }

      then:
        def e = thrown(PactFailedException)
        e.message.contains(
          '''|The following requests were not received:
             |\tmethod: post
             |\tpath: /path
             |\tquery: {}
             |\theaders: {Content-Type=[application/json]}
             |\tmatchers: MatchingRules(rules={body=MatchingRuleCategory(name=body, matchingRules={}), path=MatchingRuleCategory(name=path, matchingRules={}), query=MatchingRuleCategory(name=query, matchingRules={}), header=MatchingRuleCategory(name=header, matchingRules={})})
             |\tgenerators: Generators(categories={})
             |\tbody: PRESENT({
             |    "status": "isGood"
             |})'''.stripMargin())
    }
}
