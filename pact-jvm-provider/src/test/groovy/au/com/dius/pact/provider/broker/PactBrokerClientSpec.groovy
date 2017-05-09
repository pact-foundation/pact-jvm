package au.com.dius.pact.provider.broker

@SuppressWarnings('UnusedImport')
import au.com.dius.pact.consumer.PactVerified$
import au.com.dius.pact.consumer.groovy.PactBuilder
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents
  private pactBroker

  def setup() {
    pactBrokerClient = new PactBrokerClient('http://localhost:8080')
    pactFile = File.createTempFile('pact', '.json')
    pactContents = '''
      {
          "provider" : {
              "name" : "Provider"
          },
          "consumer" : {
              "name" : "Foo Consumer"
          },
          "interactions" : []
      }
    '''
    pactFile.write pactContents
    pactBroker = new PactBuilder()
    pactBroker {
      serviceConsumer 'JVM Pact Broker Client'
      hasPactWith 'Pact Broker'
      port 8080
    }
  }

  def 'when fetching consumers, sets the auth if there is any'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().pactFile == new URL('http://bob.com/')
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> throw new NotFoundHalResponse() }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers == []
  }

  def 'fetches consumers with specified tag successfully'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().pactFile == new URL('http://bob.com/')
  }

  def 'when fetching consumers with specified tag, sets the auth if there is any'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers with specified tag for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> throw new NotFoundHalResponse() }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers == []
  }

  def 'returns success when uploading a pact is ok'() {
    given:
    pactBroker {
      uponReceiving('a pact publish request')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0',
        body: pactContents
      )
      willRespondWith(status: 200)
    }

    when:
    def result = pactBroker.run {
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'HTTP/1.1 200 OK'
    }

    then:
    result == PactVerified$.MODULE$
  }

  def 'returns an error when uploading a pact fails'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def result = client.uploadPactFile(pactFile, '10.0.0')

    then:
    1 * halClient.uploadJson('/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0', pactContents, _) >>
      { args -> args[2].call('Failed', 'Error') }
    result == 'FAILED! Error'
  }

  @SuppressWarnings('LineLength')
  def 'returns an error if the pact broker rejects the pact'() {
    given:
    pactBroker {
      given('No pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish request with invalid version')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/XXXX',
        body: pactContents
      )
      willRespondWith(status: 400, headers: ['Content-Type': 'application/json;charset=utf-8'],
        body: '''
        |{
        |  "errors": {
        |    "consumer_version_number": [
        |      "Consumer version number 'XXX' cannot be parsed to a version number. The expected format (unless this configuration has been overridden) is a semantic version. eg. 1.3.0 or 2.0.4.rc1"
        |    ]
        |  }
        |}
        '''.stripMargin()
      )
    }

    when:
    def result = pactBroker.run {
      assert pactBrokerClient.uploadPactFile(pactFile, 'XXXX') == 'FAILED! 400 Bad Request - ' +
        'consumer_version_number: [Consumer version number \'XXX\' cannot be parsed to a version number. ' +
        'The expected format (unless this configuration has been overridden) is a semantic version. eg. 1.3.0 or 2.0.4.rc1]'
    }

    then:
    result == PactVerified$.MODULE$
  }

  @SuppressWarnings('LineLength')
  def 'returns an error if the pact broker rejects the pact with a conflict'() {
    given:
    pactBroker {
      given('No pact has been published between the Provider and Foo Consumer and there is a similar consumer')
      uponReceiving('a pact publish request')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0',
        body: pactContents
      )
      willRespondWith(status: 409, headers: ['Content-Type': 'text/plain'],
        body: '''
        |This is the first time a pact has been published for "Foo Consumer".
        |The name "Foo Consumer" is very similar to the following existing consumers/providers:
        |Consumer
        |If you meant to specify one of the above names, please correct the pact configuration, and re-publish the pact.
        |If the pact is intended to be for a new consumer or provider, please manually create "Foo Consumer" using the following command, and then re-publish the pact:
        |$ curl -v -XPOST -H "Content-Type: application/json" -d "{\\"name\\": \\"Foo Consumer\\"}" %{create_pacticipant_url}
        '''.stripMargin()
      )
    }

    when:
    def result = pactBroker.run {
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'FAILED! 409 Conflict - '
    }

    then:
    result == PactVerified$.MODULE$
  }

  @SuppressWarnings('LineLength')
  def 'handles non-json failure responses'() {
    given:
    pactBroker {
      given('Non-JSON response')
      uponReceiving('a pact publish request')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0',
        body: pactContents
      )
      willRespondWith(status: 400, headers: ['Content-Type': 'text/plain'],
        body: 'Enjoy this bit of text'
      )
    }

    when:
    def result = pactBroker.run {
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'FAILED! 400 Bad Request - Enjoy this bit of text'
    }

    then:
    result == PactVerified$.MODULE$
  }
}
