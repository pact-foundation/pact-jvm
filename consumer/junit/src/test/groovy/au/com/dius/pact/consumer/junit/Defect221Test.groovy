package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.junit.Rule
import org.junit.Test

class Defect221Test {

    private static final String APPLICATION_JSON = 'application/json'

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final PactProviderRule provider = new PactProviderRule('221_provider', 'localhost', 8112, this)

    @Pact(provider= '221_provider', consumer= 'test_consumer')
    @SuppressWarnings('JUnitPublicNonTestMethod')
    RequestResponsePact createFragment(PactDslWithProvider builder) {
        builder
            .given('test state')
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
    @PactVerification('221_provider')
    void runTest() {
      def result = new JsonSlurper().parseText(Request.put('http://localhost:8112/numbertest')
        .addHeader('Accept', APPLICATION_JSON)
        .bodyString('{"name": "harry","data": 1234.0 }', ContentType.APPLICATION_JSON)
        .execute().returnContent().asString())
      assert result == [data: 1234.0, name: 'harry', responsetest: true]
    }
}
