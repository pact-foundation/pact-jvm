package au.com.dius.pact.consumer.groovy

import groovyx.net.http.RESTClient
import org.junit.Test

import static org.junit.Assert.fail

class PactResultTest {

    @Test
    void 'case when the test passes and the pact is verified'() {

        def testService = new PactBuilder().build  {
            serviceConsumer "Consumer"
            hasPactWith "Test Service"
            port 1234

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

        testService.runTestAndVerify() {
            def client = new RESTClient('http://localhost:1234/')
            def response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                requestContentType: 'application/json')

            assert response.status == 200
            assert response.data == [status: 'isGood']
        }
    }

    @Test
    void 'case when the test fails and the pact is verified'() {

        def testService = new PactBuilder().build  {
            serviceConsumer "Consumer"
            hasPactWith "Test Service"
            port 1234

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

        try {
            testService.runTestAndVerify() {
                def client = new RESTClient('http://localhost:1234/')
                def response = client.get(path: '/path', query: [status: 'good', name: 'ron'],
                    requestContentType: 'application/json')

                assert response.status == 201
                assert response.data == [status: 'isGood']
            }
            fail('Should have raised an exception')
        } catch (PactFailedException ex) {
            assert ex.message.contains('assert response.status == 201')
        }
    }

    @Test
    void 'case when the test fails and the pact has a mismatch'() {

        def testService = new PactBuilder().build  {
            serviceConsumer "Consumer"
            hasPactWith "Test Service"
            port 1234

            uponReceiving('a valid request')
            withAttributes(method: 'get', path: '/path', query: [status: 'good', name: 'ron'])
            willRespondWith(status: 200)
            withBody {
                status 'isGood'
            }
        }

        try {
            testService.runTestAndVerify() {
                def client = new RESTClient('http://localhost:1234/')
                def response = client.get(path: '/path', query: [status: 'bad', name: 'ron'], requestContentType: 'application/json')
                assert response.status == 200
            }
            fail('Should have raised an exception')
        } catch (PactFailedException ex) {
            assert ex.message.contains('QueryMismatch(status=good&name=ron,name=ron&status=bad)')
        }
    }

    @Test
    void 'case when the test passes and there is a missing request'() {

        def testService = new PactBuilder().build  {
            serviceConsumer "Consumer"
            hasPactWith "Test Service"
            port 1234

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

        try {
            testService.runTestAndVerify() {
                def client = new RESTClient('http://localhost:1234/')
                def response = client.get(path: '/path', query: [status: 'good', name: 'ron'], requestContentType: 'application/json')
                assert response.status == 200
            }
        } catch (PactFailedException ex) {
            assert ex.message.contains('The following requests where not received:\n' +
                'Interaction: a valid post request\n' +
                '\tin state None\n' +
                'request:\n' +
                '\tmethod: post\n' +
                '\tpath: /path')
        }
    }
}
