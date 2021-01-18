package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonSlurper
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test

class ProviderStateWithComplexParametersTest {

    private static final String APPLICATION_JSON = 'application/json'

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final PactProviderRule provider = new PactProviderRule('provider_with_complex_params',
      'localhost', 8113, this)

    @Pact(consumer='test_consumer')
    @SuppressWarnings('JUnitPublicNonTestMethod')
    RequestResponsePact createFragment(PactDslWithProvider builder) {
        builder
            .given('test state', [
              a: 1,
              b: 'two',
              c: [1, 2, 'three']
            ])
            .uponReceiving('A request with double precision number')
                .path('/numbertest')
                .method('PUT')
                .body('{"name": "harry","data": 1234.0 }', APPLICATION_JSON)
            .willRespondWith()
                .status(200)
                .body('{"responsetest": true, "name": "harry","data": 1234.0 }', APPLICATION_JSON)
            .toPact()
    }

    @Test
    @PactVerification
    void runTest() {
      def result = new JsonSlurper().parseText(Request.Put('http://localhost:8113/numbertest')
        .addHeader('Accept', APPLICATION_JSON)
        .bodyString('{"name": "harry","data": 1234.0 }', ContentType.APPLICATION_JSON)
        .execute().returnContent().asString())
      assert result == [data: 1234.0, name: 'harry', responsetest: true]
    }

    @AfterClass
    static void afterTest() {
      def testResources = ProviderStateWithComplexParametersTest.getResource('/').file
      def pacts = new File(testResources + '/../../../pacts')
      def pactForThisTest = new File(pacts, 'test_consumer-provider_with_complex_params.json')
      def json = new JsonSlurper().parse(pactForThisTest)
      assert json.interactions[0].providerStates[0].params == ['a': 1, 'b': 'two', 'c': [1, 2, 'three']]
    }
}
