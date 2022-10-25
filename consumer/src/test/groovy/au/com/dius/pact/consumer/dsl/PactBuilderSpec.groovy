package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Issue
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
}
