package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import groovy.json.JsonOutput
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
        .matchHeader('Content-Type', APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
      .willRespondWith()
        .status(200)
        .matchHeader('Content-Type', APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
        .body(JsonOutput.toJson(user()))
      .toPact()
  }

  @Test
  @PactTestFor(pactMethod = 'createFragment1')
  void runTest1(MockServer mockServer) {
    def http = HttpBuilder.configure { request.uri = mockServer.url }

    http.post {
      request.uri.path = '/some-service/users'
      request.body = user()
      request.contentType = 'application/json'

      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 201
        assert fs.headers.find { it.key == 'Location' }?.value?.contains(SOME_SERVICE_USER)
      }
    }

    http.get {
      request.uri.path = SOME_SERVICE_USER + EXPECTED_USER_ID
      request.contentType = 'application/json'
      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 200
      }
    }

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
  @PactTestFor(pactMethod = 'createFragment2')
  void runTest2(MockServer mockServer) {
    assert Request.Put(mockServer.url + '/numbertest')
      .addHeader('Accept', 'application/json')
      .bodyString('{"name": "harry","data": 1234.0 }', ContentType.APPLICATION_JSON)
      .execute().returnContent().asString() == '{"data":1234.0,"name":"harry","responsetest":true}'
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
  @PactTestFor(pactMethod = 'getUsersFragment')
  void runTest3(MockServer mockServer) {
    assert Request.Get(mockServer.url + '/idm/user').execute().returnContent().asString()
  }

  @Test
  @PactTestFor(pactMethod = 'getUsersFragment2')
  void runTest4(MockServer mockServer) {
    assert Request.Get(mockServer.url + '/idm/user').execute().returnContent().asString()
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

}
