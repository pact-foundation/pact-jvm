package au.com.dius.pact.server

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import scala.Option
import scala.collection.JavaConverters
import scala.collection.immutable.List
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path

@RestoreSystemProperties
class PublishSpec extends Specification {
  static Path pactDir

  def setupSpec() {
    pactDir = Files.createTempDirectory('pacts')
    System.setProperty('pact.rootDir', pactDir.toString())
  }

  def cleanupSpec() {
    System.properties.remove('pact.rootDir')
    pactDir.toFile().deleteDir()
  }

    def 'invalid broker url in config will not set broker'() {
        given:
        def config = new Config(80, '0.0.0.0', false, 100, 200, false, 3, '', '', 0, 'invalid', 'abc#3')

        when:
        def result = Publish.INSTANCE.getBrokerUrlFromConfig(config)

        then:
        !result
    }

    def 'valid broker url will set broker'() {
        given:
        def config = new Config(80,
                '0.0.0.0',
                false,
                100,
                200,
                false,
                3,
                '',
                '',
                0,
                'https://valid.broker.com',
                'abc#3'
        )

        when:
        def result = Publish.INSTANCE.getBrokerUrlFromConfig(config)

        then:
        result
    }

  def 'getVarFromJson'() {
    expect:
    Publish.INSTANCE.getVarFromJson(variable, json) == result

    where:

    variable | json     | result
    'null'   | null     | null
    'null'   | 'null'   | null
    'null'   | [a: 'b'] | null
    'c'      | [a: 'b'] | null
    'a'      | [a: 'b'] | 'b'
    'a'      | [a: 100] | '100'
  }

  def 'getListFromJson'() {
    expect:
    Publish.INSTANCE.getListFromJson(variable, json) == result

    where:

    variable | json       | result
    'null'   | null       | null
    'null'   | 'null'     | null
    'null'   | [a: 'b']   | null
    'c'      | [a: 'b']   | null
    'a'      | [a: ['b']] | ['b']
  }

  def 'getBrokerUrlFromConfig'() {
    expect:
    Publish.INSTANCE.getBrokerUrlFromConfig(new Config().copyBroker(broker)) == result

    where:

    broker           | result
    ''               | null
    'hgjhkhj'        | null
    'http://hgjhkhj' | 'http://hgjhkhj'
  }

  def 'getVarFromConfig'() {
    expect:
    Publish.INSTANCE.getVarFromConfig(variable) == result

    where:

    variable | result
    ''       | null
    'a'      | 'a'
  }

  def 'getOptions'() {
    expect:
    Publish.INSTANCE.getOptions(authToken) == result

    where:

    authToken | result
    null      | [:]
    'token'   | ['authentication': ['bearer', 'token']]
  }

  def 'apply returns an error if the broker URL is not defined'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{}')
    def state = new ServerState()
    def config = new Config()

    when:
    def result = Publish.apply(request, state, config)

    then:
    result.response.status == 500
  }

  def 'apply returns an error if there is no consumer in the request body'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"consumerVersion":"123","provider":"provider"}')
    def state = new ServerState()
    def config = new Config().copyBroker('http://test-broker')

    when:
    def result = Publish.apply(request, state, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if there is no consumerVersion in the request body'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"consumer":"123","provider":"provider"}')
    def state = new ServerState()
    def config = new Config().copyBroker('http://test-broker')

    when:
    def result = Publish.apply(request, state, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if there is no provider in the request body'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"consumerVersion":"123","consumer":"provider"}')
    def state = new ServerState()
    def config = new Config().copyBroker('http://test-broker')

    when:
    def result = Publish.apply(request, state, config)

    then:
    result.response.status == 400
  }

  def 'publishPact calls uploadPactFile on the broker client'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = null

    when:
    def result = Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', []) >> new au.com.dius.pact.core.support.Result.Ok('OK')
    result.status == 200
    result.body.valueAsString() == 'OK'
  }

  def 'publishPact deletes the Pact file after publishing it'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = null
    def pactFile = new File(pactDir.toFile(), "${consumer}-${provider}.json".toString())
    pactFile.text = '{}'

    when:
    Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', []) >> new au.com.dius.pact.core.support.Result.Ok('OK')
    !pactFile.exists()
  }

  def 'publishPact returns an error if the publish call fails'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = null

    when:
    def result = Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', []) >> new au.com.dius.pact.core.support.Result.Err(new RuntimeException('Boom'))
    result.status == 500
    result.body.valueAsString() == 'Boom'
  }

  def 'publishPact does not delete the Pact file if publishing it fails'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = null
    def pactFile = new File(pactDir.toFile(), "${consumer}-${provider}.json".toString())
    pactFile.text = '{}'

    when:
    Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', []) >> new au.com.dius.pact.core.support.Result.Err(new RuntimeException('Boom'))
    pactFile.exists()
  }

  def 'publishPact passes any tags to the publish call'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = ['a', 'b', 'c']

    when:
    Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', ['a', 'b', 'c']) >> new au.com.dius.pact.core.support.Result.Ok('OK')
  }

  def 'publishPact handles any IO exception'() {
    given:
    String consumer = 'consumer'
    String consumerVersion = 'version'
    String provider = 'provider'
    String broker = 'http://test-broker'
    IPactBrokerClient client = Mock(IPactBrokerClient)
    def tags = null

    when:
    def result = Publish.INSTANCE.publishPact(consumer, consumerVersion, provider, broker, client, tags)

    then:
    1 * client.uploadPactFile(_, 'version', []) >> { throw new IOException("Boom!") }
    result.status == 500
  }
}
