package au.com.dius.pact.provider.junit.descriptions

import au.com.dius.pact.core.model.*
import org.junit.runners.model.TestClass
import spock.lang.Specification

class DescriptionGeneratorTest extends Specification {

  class DescriptionGeneratorTestClass {
  }

  private clazz
  private interaction
  private pact

  def setup() {
    clazz = new TestClass(DescriptionGeneratorTestClass)
  }

  def 'when BrokerUrlSource tests description includes tag if present'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
            [ new ProviderState('Test State') ], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])

    expect:
    def pactSource =  new BrokerUrlSource('url', 'url', [:], [:], tag)
    def generator = new DescriptionGenerator(clazz, pact, pactSource)
    description == generator.generate(interaction).getMethodName()

    where:
    tag      | description
    "master" | "[tag:master] consumer - Upon Interaction 1"
    null     | "consumer - Upon Interaction 1"
    ""       | "consumer - Upon Interaction 1"
  }

  def 'when non broker pact source tests name are built correctly'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
            [ new ProviderState('Test State') ], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    def file = Mock(File)

    expect:
    def pactSource = new DirectorySource(file, [file: pact])
    def generator = new DescriptionGenerator(clazz, pact, pactSource)
    "consumer - Upon Interaction 1" == generator.generate(interaction).getMethodName()
  }
}
