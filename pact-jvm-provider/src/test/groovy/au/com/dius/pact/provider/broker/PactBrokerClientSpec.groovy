package au.com.dius.pact.provider.broker

import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile

  def setup() {
    pactBrokerClient = new PactBrokerClient()
    pactFile = File.createTempFile('pact', '.json')
    pactFile.write '''
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

  def 'returns success when uploading a pact is ok'() {
    given:
    def delegate = [
      uri: [:],
      response: [:]
    ]
    def req = Mock(HttpResponse) {
      getStatusLine() >> new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 200, 'OK')
    }
    GroovySpy(HTTPBuilder, global: true) {
      request(_, _, _) >> { args ->
        def closure = args.last()
        closure.delegate = delegate
        closure.call()
        delegate.response.success.call(req)
      }
    }

    when:
    def result = pactBrokerClient.uploadPactFile(pactFile, '10.0.0')

    then:
    result == 'HTTP/1.1 200 OK'
  }

  def 'returns an error when uploading a pact fails'() {
    given:
    def delegate = [
      uri: [:],
      response: [:]
    ]
    def req = Mock(HttpResponse) {
      getStatusLine() >> new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 500, 'Bang')
    }
    GroovySpy(HTTPBuilder, global: true) {
      request(_, _, _) >> { args ->
        def closure = args.last()
        closure.delegate = delegate
        closure.call()
        delegate.response.failure.call(req, null)
      }
    }

    when:
    def result = pactBrokerClient.uploadPactFile(pactFile, '10.0.0')

    then:
    result == 'FAILED! 500 Bang - Unknown error'
  }

}
