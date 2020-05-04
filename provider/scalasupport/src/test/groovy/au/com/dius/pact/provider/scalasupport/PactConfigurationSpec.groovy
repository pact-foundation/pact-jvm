package au.com.dius.pact.provider.scalasupport

import spock.lang.Specification

class PactConfigurationSpec extends Specification {

  File testData, pactConfig

  def setup() {
    testData = File.createTempDir()
    pactConfig = new File(testData, 'pact-config.json')
  }

  def cleanup() {
    testData.delete()
  }

  def 'loads the pact config correctly'() {
    given:
    def expectedConfig = new PactConfiguration(new Address('localhost', 8888, '', 'http'),
      new Address('localhost', 8888, '/enterState', 'http'))
    pactConfig.text = PactConfigurationSpec.getResourceAsStream('/pact-config.json').text

    when:
    def config = PactConfiguration.loadConfiguration(pactConfig)

    then:
    config == expectedConfig
  }

  def 'handles missing statechange url'() {
    given:
    def expectedConfig = new PactConfiguration(new Address('localhost', 8888, '', 'http'), null)
    pactConfig.text = PactConfigurationSpec.getResourceAsStream('/pact-config-no-statechange-url.json').text

    when:
    def config = PactConfiguration.loadConfiguration(pactConfig)

    then:
    config == expectedConfig
  }

  def 'fails if there is no provider root'() {
    given:
    pactConfig.text = PactConfigurationSpec.getResourceAsStream('/pact-config-invalid.json').text

    when:
    PactConfiguration.loadConfiguration(pactConfig)

    then:
    thrown(InvalidPactConfigurationException)
  }

}
