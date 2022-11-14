package au.com.dius.pact.consumer.dsl


import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Issue
import au.com.dius.pact.core.model.V4Interaction
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class PactBuilderSpec extends Specification {

  @Unroll
  def 'allows adding additional metadata to Pact file - #ver'() {
    given:
    def builder = new PactBuilder('test', 'test', ver)
      .addMetadataValue('extra', 'value')

    expect:
    builder.toPact().metadata.findAll {
      !['pactSpecification', 'pact-jvm', 'plugins'].contains(it.key)
    } == [extra: 'value']

    where:

    ver << [PactSpecVersion.V3, PactSpecVersion.V4 ]
  }

  @Issue('#1612')
  def 'queryMatchingDatetime creates invalid generator'() {
    given:
    def builder = new PactBuilder()
    def pact = builder.usingLegacyDsl()
      .uponReceiving("a request")
      .path("/api/myrequest")
      .method("POST")
      .queryMatchingDatetime("startDateTime", "yyyy-MM-dd'T'hh:mm:ss'Z'")
      .willRespondWith()
      .status(200)
      .toPact(V4Pact)

    when:
    def request = pact.interactions.first()
    def generators = request.asSynchronousRequestResponse().request.generators

    then:
    generators.toMap(PactSpecVersion.V4) == [
      query: [startDateTime: [type: 'DateTime', format: "yyyy-MM-dd'T'hh:mm:ss'Z'"]]
    ]
  }

  def 'expectsToReceive - defaults to the HTTP interaction if not specified'() {
    given:
    def builder = new PactBuilder('test', 'test', PactSpecVersion.V4)

    when:
    builder.expectsToReceive("test interaction", "")

    then:
    builder.currentInteraction instanceof V4Interaction.SynchronousHttp
  }

  @Ignore
  // This test is currently failing due to dependency linking issues
  def 'supports configuring the HTTP interaction attributes'() {
    given:
    def builder = new PactBuilder('test', 'test', PactSpecVersion.V4)

    when:
    def pact = builder.expectsToReceive("test interaction", "")
      .with([
        'request.method': 'PUT',
        'request.path': '/reports/report002.csv',
        'request.query': [a: 'b'],
        'request.headers': ['x-a': 'b'],
        'request.contents': [
          'pact:content-type': 'application/json',
          'body': 'a'
        ],
        'response.status': '200',
        'response.headers': ['x-b': ['b']],
        'response.contents': [
          'pact:content-type': 'application/json',
          'body': 'b'
        ]
      ]).toPact()
    def http = pact.interactions.first().asSynchronousRequestResponse()

    then:
    http.request == new HttpRequest('PUT', '/reports/report002.csv', [a: ['b']], ['x-a': ['b']],
      OptionalBody.body('"a"', ContentType.JSON))
    http.response == new HttpResponse(205, ['x-b': ['a']], OptionalBody.body('"b"', ContentType.JSON))
  }
}
