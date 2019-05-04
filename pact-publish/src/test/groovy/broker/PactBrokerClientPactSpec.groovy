package broker

import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.groovy.PactBuilder
import au.com.dius.pact.provider.broker.PactBrokerClient
import spock.lang.Specification

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientPactSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents
  private PactBuilder pactBroker, imaginaryBroker

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

    imaginaryBroker = new PactBuilder()
    imaginaryBroker {
      serviceConsumer 'JVM Pact Broker Client'
      hasPactWith 'Imaginary Pact Broker'
      port 8080
    }
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
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'HTTP/1.1 200 OK'
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

  def 'returns an error when forbidden to publish the pact'() {
    given:
    pactBroker {
      uponReceiving('a pact publish request which will be forbidden')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0',
        body: pactContents
      )
      willRespondWith(status: 401, headers: [
        'Content-Type': 'application/json'
      ])
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'FAILED! 401 Unauthorized'
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
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
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, 'XXXX') == 'FAILED! 400 Bad Request - ' +
        'consumer_version_number: Consumer version number \'XXX\' cannot be parsed to a version number. ' +
        'The expected format (unless this configuration has been overridden) is a semantic version. eg. 1.3.0 or 2.0.4.rc1'
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
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
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0').startsWith('FAILED! 409 Conflict - ')
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

  @SuppressWarnings('LineLength')
  def 'handles non-json failure responses'() {
    given:
    imaginaryBroker {
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
    def result = imaginaryBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') == 'FAILED! 400 Bad Request - Enjoy this bit of text'
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

  def 'pact broker navigation test'() {
    given:
    pactBroker {
      given('Two consumer pacts exist for the provider', [
        provider: 'Activity Service',
        consumer1: 'Foo Web Client',
        consumer2: 'Foo Web Client 2'
      ])
      uponReceiving('a request to the root')
      withAttributes(path: '/')
      willRespondWith(status: 200)
      withBody('application/hal+json') {
        '_links' {
          'pb:latest-provider-pacts' {
            href url('http://localhost:8080', 'pacts', 'provider', '{provider}', 'latest')
            title 'Latest pacts by provider'
            templated true
          }
        }
      }
      uponReceiving('a request for the provider pacts')
      withAttributes(path: '/pacts/provider/Activity Service/latest')
      willRespondWith(status: 200)
      withBody('application/hal+json') {
        '_links' {
          provider {
            href url('http://localhost:8080', 'pacticipants', regexp('[^\\/]+', 'Activity Service'))
            title string('Activity Service')
          }
          pacts eachLike(2) {
            href url('http://localhost:8080', 'pacts', 'provider', regexp('[^\\/]+', 'Activity Service'),
              'consumer', regexp('[^\\/]+', 'Foo Web Client'),
              'version', regexp('\\d+\\.\\d+\\.\\d+', '0.1.380'))
            title string('Pact between Foo Web Client (v0.1.380) and Activity Service')
            name string('Foo Web Client')
          }
        }
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.fetchConsumers('Activity Service').size() == 2
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

  def 'publishing verification results pact test'() {
    given:
    pactBroker {
      given('A pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish verification request')
      withAttributes(method: 'POST',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/pact-version/1234567890/verification-results',
        body: [success: true, providerApplicationVersion: '10.0.0']
      )
      willRespondWith(status: 201)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.publishVerificationResults([
        'pb:publish-verification-results': [href: 'http://localhost:8080/pacts/provider/Provider/consumer/Foo%20Consumer/pact-version/1234567890/verification-results']
      ], true, '10.0.0') instanceof Ok
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

  def 'publishing verification results pact test with build info'() {
    given:
    pactBroker {
      given('A pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish verification request')
      withAttributes(method: 'POST',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/pact-version/1234567890/verification-results',
        body: [success: true, providerApplicationVersion: '10.0.0', buildUrl: 'http://localhost:8080/build']
      )
      willRespondWith(status: 201)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.publishVerificationResults([
        'pb:publish-verification-results': [href: 'http://localhost:8080/pacts/provider/Provider/consumer/Foo%20Consumer/pact-version/1234567890/verification-results']
      ], true, '10.0.0', 'http://localhost:8080/build') instanceof Ok
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }
}
