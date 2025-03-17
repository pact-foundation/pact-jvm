package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonOutput
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.extension.ExtendWith

@SuppressWarnings(['PublicInstanceField', 'JUnitPublicNonTestMethod', 'FactoryMethodName'])
@ExtendWith(PactConsumerTestExt)
class MultiTest {

  private static final String EXPECTED_USER_ID = 'abcdefghijklmnop'
  private static final String CONTENT_TYPE = 'Content-Type'
  private static final String APPLICATION_JSON = 'application/json.*'
  private static final String APPLICATION_JSON_CHARSET_UTF_8 = 'application/json; charset=UTF-8'
  private static final String SOME_SERVICE_USER = '/some-service/user/'

  private static user() {
    [
      username: 'bbarke',
      password: '123456',
      firstname: 'Brent',
      lastname: 'Barker',
      booleam: 'true'
    ]
  }

  @Pact(provider = 'multitest_provider', consumer= 'browser_consumer')
  RequestResponsePact createFragment1(PactDslWithProvider builder) {
    builder
      .given('An env')
      .uponReceiving('a new user')
        .path('/some-service/users')
        .method('POST')
        .body(JsonOutput.toJson(user()))
        .matchHeader(CONTENT_TYPE, APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
      .willRespondWith()
        .status(201)
        .matchHeader('Location', 'http(s)?://\\w+:\\d+//some-service/user/\\w{36}$')
      .given("An automation user with id: $EXPECTED_USER_ID")
      .uponReceiving('existing user lookup')
        .path(SOME_SERVICE_USER + EXPECTED_USER_ID)
        .method('GET')
      .willRespondWith()
        .status(200)
        .matchHeader('Content-Type', APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
        .body(JsonOutput.toJson(user()))
      .toPact()
  }

  @Test
  @PactTestFor(pactMethod = 'createFragment1', pactVersion = PactSpecVersion.V3)
  void runTest1(MockServer mockServer) {
    def http = new SimpleHttp(mockServer.url)

    def response = http.post('/some-service/users', JsonOutput.toJson(user()), 'application/json')
    assert response.statusCode == 201
    assert response.headers['location'].first().contains(SOME_SERVICE_USER)

    response = http.get(SOME_SERVICE_USER + EXPECTED_USER_ID)
    assert response.statusCode == 200
  }

  @Pact(provider= 'multitest_provider', consumer= 'test_consumer')
  RequestResponsePact createFragment2(PactDslWithProvider builder) {
    builder
      .given('test state')
      .uponReceiving('A request with double precision number')
        .path('/numbertest')
        .method('PUT')
        .body('{"name": "harry","data": 1234.0 }', 'application/json')
      .willRespondWith()
        .status(200)
        .body('{"responsetest": true, "name": "harry","data": 1234.0 }', 'application/json')
      .toPact()
  }

  @Test
  @PactTestFor(pactMethod = 'createFragment2', pactVersion = PactSpecVersion.V3)
  void runTest2(MockServer mockServer) {
    assert Request.put(mockServer.url + '/numbertest')
      .addHeader('Accept', 'application/json')
      .bodyString('{"name": "harry","data": 1234.0 }', ContentType.APPLICATION_JSON)
      .execute().returnContent().asString() == '{"responsetest": true, "name": "harry","data": 1234.0 }'
  }

  @Pact(provider = 'multitest_provider', consumer = 'test_consumer')
  RequestResponsePact getUsersFragment(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().maxArrayLike(5)
      .uuid('id')
      .stringType('userName')
      .stringType('email')
      .closeObject()
    builder
      .given("a user with an id named 'user' exists")
      .uponReceiving('get all users for max')
        .path('/idm/user')
        .method('GET')
      .willRespondWith()
        .status(200)
        .body(body)
      .toPact()
  }

  @Pact(provider = 'multitest_provider', consumer = 'test_consumer')
  RequestResponsePact getUsersFragment2(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().minArrayLike(5)
      .uuid('id')
      .stringType('userName')
      .stringType('email')
      .closeObject()
    builder
      .given("a user with an id named 'user' exists")
      .uponReceiving('get all users for min')
        .path('/idm/user')
        .method('GET')
      .willRespondWith()
        .status(200)
        .body(body)
      .toPact()
  }

  @Test
  @PactTestFor(pactMethod = 'getUsersFragment', pactVersion = PactSpecVersion.V3)
  void runTest3(MockServer mockServer) {
    assert Request.get(mockServer.url + '/idm/user').execute().returnContent().asString()
  }

  @Test
  @PactTestFor(pactMethod = 'getUsersFragment2', pactVersion = PactSpecVersion.V3)
  void runTest4(MockServer mockServer) {
    assert Request.get(mockServer.url + '/idm/user').execute().returnContent().asString()
  }

  @Pact(provider = 'multitest_provider', consumer = 'test_consumer')
  @Disabled
  RequestResponsePact getUsersFragment3(PactDslWithProvider builder) {
    builder
      .uponReceiving('get all users')
      .path('/idm/user')
      .method('GET')
      .willRespondWith()
      .status(404)
      .toPact()
  }

  @Pact(provider = 'multitest_provider', consumer = 'test_consumer')
  @DisabledIf('shouldBeDisabled')
  RequestResponsePact getAnotherUsersFragment3(PactDslWithProvider builder) {
    builder
      .uponReceiving('get all users')
      .path('/idm/user')
      .method('GET')
      .willRespondWith()
      .status(404)
      .toPact()
  }

  boolean shouldBeDisabled() {
    true
  }
}
