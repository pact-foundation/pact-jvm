package au.com.dius.pact.provider.junit.descriptions

import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
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
    def pact = new RequestResponsePact(new Provider(), new Consumer('the-consumer-name'), [ interaction ])

    expect:
    def pactSource =  new BrokerUrlSource('url', 'url', [:], [:], tag)
    def generator = new DescriptionGenerator(clazz, pact, pactSource)
    description == generator.generate(interaction).methodName

    where:
    tag      | description
    'master' | 'the-consumer-name [tag:master] - Upon Interaction 1'
    null     | 'the-consumer-name - Upon Interaction 1'
    ''       | 'the-consumer-name - Upon Interaction 1'
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
