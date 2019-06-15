package au.com.dius.pact.core.model

import spock.lang.Specification

class BasePactSpec extends Specification {

  def 'metadata should use the metadata from the pact file as a base'() {
    expect:
    BasePact.metaData([a: 'A'], PactSpecVersion.V3) == [a: 'A', pactSpecification: [version: '3.0.0'], 'pact-jvm':
      [version: '']]
  }

}
