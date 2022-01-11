package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.PactSpecVersion
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
}
