package au.com.dius.pact.provider.junit.descriptions

import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.DirectorySource
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.runners.model.TestClass
import spock.lang.Specification

class DescriptionGeneratorTest extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class DescriptionGeneratorTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
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
    description == generator.generate(interaction).methodName

    where:
    tag      | description
    'master' | '[tag:master] consumer - Upon Interaction 1'
    null     | 'consumer - Upon Interaction 1'
    ''       | 'consumer - Upon Interaction 1'
  }

  def 'when non broker pact source tests name are built correctly'() {
    def interaction = new RequestResponseInteraction('Interaction 1',
            [ new ProviderState('Test State') ], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    def file = Mock(File)

    expect:
    def pactSource = new DirectorySource(file, [file: pact])
    def generator = new DescriptionGenerator(clazz, pact, pactSource)
    'consumer - Upon Interaction 1' == generator.generate(interaction).methodName
  }
}
