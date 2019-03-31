package au.com.dius.pact.consumer.junit.formpost

import au.com.dius.pact.consumer.Pact
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.fluent.Form
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.Rule
import org.junit.Test

@SuppressWarnings(['PublicInstanceField', 'JUnitPublicNonTestMethod'])
class FormPostDefect198Test {

  @Rule
  public final PactProviderRule mockProvider = new PactProviderRule('formpost_provider', this)

  @Pact(provider = 'formpost_provider', consumer = 'formpost_consumer')
  RequestResponsePact customerDoesNotExist(PactDslWithProvider builder) {
    builder
      .given('customer does not exist')
      .uponReceiving('Request to authenticate')
      .method('POST')
      .path('/authentication-service/authenticate')
      .body('username=unknown%40example.com&password=foobar', ContentType.APPLICATION_FORM_URLENCODED.mimeType)
      .willRespondWith()
      .status(404)
      .toPact()
  }

  @Test
  @PactVerification(fragment = 'customerDoesNotExist')
  void customerDoesNotExist() {
    HttpResponse response = authenticateRequestWith(Form.form()
      .add('username', 'unknown@example.com')
      .add('password', 'foobar')
      .build())

    assert response.statusLine.statusCode == 404
  }

  private HttpResponse authenticateRequestWith(List<NameValuePair> formParams) {
    Request
      .Post(mockProvider.url + '/authentication-service/authenticate')
      .bodyForm(formParams, null)
      .execute()
      .returnResponse()
  }

}
