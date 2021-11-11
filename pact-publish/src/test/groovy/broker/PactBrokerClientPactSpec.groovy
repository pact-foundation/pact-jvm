package broker

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.groovy.PactBuilder
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.Latest
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.pactbroker.TestResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import spock.lang.Specification

@SuppressWarnings(['UnnecessaryGetter', 'LineLength', 'NestedBlockDepth', 'AbcMetric', 'MethodSize'])
class PactBrokerClientPactSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents
  private PactBuilder pactBroker, imaginaryBroker

  def setup() {
    pactBrokerClient = new PactBrokerClient('http://localhost:8080', [halClient: [maxPublishRetries: 0]],
      new PactBrokerClientConfig())
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
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:publish-pact' {
            href url('http://localhost:8080', 'pacts/provider/{provider}/consumer/{consumer}/version/{consumerApplicationVersion}')
            title 'Publish a pact'
            templated true
          }
        }
      }
      uponReceiving('a pact publish request')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0/A',
        body: pactContents
      )
      willRespondWith(status: 200)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0/A') instanceof Ok
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'returns success when creating a tag for a pact is ok'() {
    given:
    pactBroker {
        uponReceiving('a HAL navigate request')
        withAttributes(method: 'GET', path: '/', body: '')
        willRespondWith(status: 200)
        withBody(mimetype: 'application/json') {
          _links {
            'pb:pacticipant-version-tag' {
              href url('http://localhost:8080', '/pacticipants/{pacticipant}/versions/{version}/tags/{tag}')
              title 'Create a tag'
              templated true
            }
          }
        }
        uponReceiving('a tag create request')
        withAttributes(method: 'PUT',
                path: '/pacticipants/Foo Consumer/versions/10.0.0/tags/A',
                body: '{}'
        )
        willRespondWith(status: 201)
      }

    when:
      def result = pactBroker.runTest { server, context ->
        assert pactBrokerClient.createVersionTag('Foo Consumer', '10.0.0', 'A') instanceof Ok
      }

    then:
      result instanceof PactVerificationResult.Ok
  }

  def 'returns an error when creating a tag for a pact fails'() {
    given:
    pactBroker {
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/', body: '')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:pacticipant-version-tag' {
            href url('http://localhost:8080', '/pacticipants/{pacticipant}/versions/{version}/tags/{tag}')
            title 'Create a tag'
            templated true
          }
        }
      }
      uponReceiving('a tag create request')
      withAttributes(method: 'PUT',
              path: '/pacticipants/Foo Consumer/versions/10.0.0/tags/A',
              body: '{}'
      )
      willRespondWith(status: 500)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.createVersionTag('Foo Consumer', '10.0.0', 'A') instanceof Err<Exception>
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'returns an error when forbidden to publish the pact'() {
    given:
    pactBroker {
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:publish-pact' {
            href url('http://localhost:8080', 'pacts/provider/{provider}/consumer/{consumer}/version/{consumerApplicationVersion}')
            title 'Publish a pact'
            templated true
          }
        }
      }
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
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') instanceof Err
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  @SuppressWarnings('LineLength')
  def 'returns an error if the pact broker rejects the pact'() {
    given:
    def body = '{"errors":{"consumer_version_number":["Consumer version number \'XXX\' cannot be parsed to a version ' +
      'number. The expected format (unless this configuration has been overridden) is a semantic version. eg. 1.3.0 or 2.0.4.rc1"]}}'
    pactBroker {
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:publish-pact' {
            href url('http://localhost:8080', 'pacts/provider/{provider}/consumer/{consumer}/version/{consumerApplicationVersion}')
            title 'Publish a pact'
            templated true
          }
        }
      }
      given('No pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish request with invalid version')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/XXXX',
        body: pactContents
      )
      willRespondWith(status: 400, headers: ['Content-Type': 'application/json;charset=utf-8'], body: body)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      def result = pactBrokerClient.uploadPactFile(pactFile, 'XXXX')
      assert result instanceof Err
      assert result.error.body == body
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  @SuppressWarnings('LineLength')
  def 'returns an error if the pact broker rejects the pact with a conflict'() {
    given:
    def body = '''
        |This is the first time a pact has been published for "Foo Consumer".
        |The name "Foo Consumer" is very similar to the following existing consumers/providers:
        |Consumer
        |If you meant to specify one of the above names, please correct the pact configuration, and re-publish the pact.
        |If the pact is intended to be for a new consumer or provider, please manually create "Foo Consumer" using the following command, and then re-publish the pact:
        |$ curl -v -XPOST -H "Content-Type: application/json" -d "{\\"name\\": \\"Foo Consumer\\"}" %{create_pacticipant_url}
        '''.stripMargin()
    pactBroker {
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:publish-pact' {
            href url('http://localhost:8080', 'pacts/provider/{provider}/consumer/{consumer}/version/{consumerApplicationVersion}')
            title 'Publish a pact'
            templated true
          }
        }
      }
      given('No pact has been published between the Provider and Foo Consumer and there is a similar consumer')
      uponReceiving('a pact publish request')
      withAttributes(method: 'PUT',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0',
        body: pactContents
      )
      willRespondWith(status: 409, headers: ['Content-Type': 'text/plain'], body: body)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      def result = pactBrokerClient.uploadPactFile(pactFile, '10.0.0')
      assert result instanceof Err
      assert result.error.body == body
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  @SuppressWarnings('LineLength')
  def 'handles non-json failure responses'() {
    given:
    imaginaryBroker {
      uponReceiving('a HAL navigate request')
      withAttributes(method: 'GET', path: '/')
      willRespondWith(status: 200)
      withBody(mimetype: 'application/json') {
        _links {
          'pb:publish-pact' {
            href url('http://localhost:8080', 'pacts/provider/{provider}/consumer/{consumer}/version/{consumerApplicationVersion}')
            title 'Publish a pact'
            templated true
          }
        }
      }
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
      assert pactBrokerClient.uploadPactFile(pactFile, '10.0.0') instanceof Err
    }

    then:
    result instanceof PactVerificationResult.Ok
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
      withBody(contentType: 'application/hal+json') {
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
      withBody(contentType: 'application/hal+json') {
        '_links' {
          'pb:provider' {
            href url('http://localhost:8080', 'pacticipants', regexp('[^\\/]+', 'Activity Service'))
            title string('Activity Service')
          }
          'pb:pacts' eachLike(2) {
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
    result instanceof PactVerificationResult.Ok
  }

  def 'publishing verification results pact test'() {
    given:
    pactBroker {
      given('A pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish verification request')
      withAttributes(method: 'POST',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/pact-version/1234567890/verification-results'
      )
      withBody {
        success true
        providerApplicationVersion '10.0.0'
        verifiedBy {
          implementation 'Pact-JVM'
          version PactBuilder.string('4.1.12')
        }
      }
      willRespondWith(status: 201)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.publishVerificationResults([
        'pb:publish-verification-results': [
          href: 'http://localhost:8080/pacts/provider/Provider/consumer/Foo%20Consumer/pact-version/1234567890' +
            '/verification-results'
        ]
      ], new TestResult.Ok([] as Set), '10.0.0').value
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'publishing verification results pact test with build info'() {
    given:
    pactBroker {
      given('A pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish verification request with build info')
      withAttributes(method: 'POST',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/pact-version/1234567890/verification-results'
      )
      withBody {
        success true
        providerApplicationVersion '10.0.0'
        buildUrl 'http://localhost:8080/build'
        verifiedBy {
          implementation 'Pact-JVM'
          version PactBuilder.string('4.1.12')
        }
      }
      willRespondWith(status: 201)
    }

    when:
    def result = pactBroker.runTest { server, context ->
      assert pactBrokerClient.publishVerificationResults([
        'pb:publish-verification-results': [
          href: 'http://localhost:8080/pacts/provider/Provider/consumer/Foo%20Consumer/pact-version/1234567890' +
            '/verification-results'
        ]
      ], new TestResult.Ok([] as Set), '10.0.0', 'http://localhost:8080/build').value
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'publishing verification results pact test with failure info'() {
    given:
    pactBroker {
      given('A pact has been published between the Provider and Foo Consumer')
      uponReceiving('a pact publish verification request with failure info')
      withAttributes(method: 'POST',
        path: '/pacts/provider/Provider/consumer/Foo Consumer/pact-version/1234567890/verification-results')
      withBody(mimeType: 'application/json') {
        success false
        providerApplicationVersion '10.0.0'
        buildUrl 'http://localhost:8080/build'
        verifiedBy {
          implementation 'Pact-JVM'
          version PactBuilder.string('4.1.12')
        }
        testResults eachLike {
          interactionId string('12345678')
          success false
          exceptions eachLike {
            message string('Boom!')
            exceptionClass string('java.io.IOException')
          }
          mismatches eachLike {
            description string('Expected status code of 400 but got 500')
          }
        }
      }
      willRespondWith(status: 201)
    }
    def failure = new TestResult.Failed([
      [
        message: 'Request to provider method failed with an exception',
        exception: new IOException('Boom!'),
        interactionId: '12345678'
      ],
      [
        description: 'Expected status code of 400 but got 500',
        interactionId: '12345678'
      ]
    ], 'Request to provider method failed with an exception')

    when:
    def result = pactBroker.runTest { server, context ->
      pactBrokerClient.publishVerificationResults([
        'pb:publish-verification-results': [
          href: 'http://localhost:8080/pacts/provider/Provider/consumer/Foo%20Consumer/pact-version/1234567890' +
            '/verification-results'
        ]
      ], failure, '10.0.0', 'http://localhost:8080/build')
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'can-i-deploy call with provider version'() {
    given:
    pactBroker {
      given('the pact for Foo version 1.2.3 has been verified by Bar version 4.5.6 and version 5.6.7')
      uponReceiving('a request for the compatibility matrix where only the version of Foo is specified')
      withAttributes(method: 'GET', path: '/matrix', query: 'q[][pacticipant]=Foo&q[][version]=1.2.3&latestby=cvp&latest=true')
      willRespondWith(status: 200)
      withBody(mimeType: 'application/hal+json') {
        summary {
          deployable true
          reason 'some text'
          unknown 1
        }
        matrix = [
          {
            consumer {
              name 'Foo'
              version {
                number '4'
              }
            }
            provider {
              name 'Bar'
              version {
                number '5'
              }
            }
            verificationResult {
              verifiedAt '2017-10-10T12:49:04+11:00'
              success true
            }
            pact {
              createdAt '2017-10-10T12:49:04+11:00'
            }
          }
        ]
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      pactBrokerClient.canIDeploy('Foo', '1.2.3', new Latest.UseLatest(false), null)
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'can-i-deploy call with provider version and prod tag'() {
    given:
    pactBroker {
      given('the pact for Foo version 1.2.3 has been successfully verified by Bar version 4.5.6 (tagged prod) and version 5.6.7')
      uponReceiving('a request for the compatibility matrix for Foo version 1.2.3 and the latest prod versions of all other pacticipants')
      withAttributes(method: 'GET', path: '/matrix', query: 'q[][pacticipant]=Foo&q[][version]=1.2.3&latestby=cvp&latest=true&tag=prod')
      willRespondWith(status: 200)
      withBody(mimeType: 'application/hal+json') {
        summary {
          deployable true
          reason 'some text'
          unknown 1
        }
        matrix = [
          {
            consumer {
              name 'Foo'
              version {
                number '1.2.3'
              }
            }
            provider {
              name 'Bar'
              version {
                number '4.5.6'
              }
            }
          }
        ]
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      pactBrokerClient.canIDeploy('Foo', '1.2.3', new Latest.UseLatest(false), 'prod')
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'fetch pacts when new pending pacts feature is off'() {
    given:
    pactBroker {
      uponReceiving('a request to the root')
      withAttributes(path: '/')
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_links' {
          'pb:provider-pacts-for-verification' {
            href url('http://localhost:8080', 'pacts', 'provider', '{provider}', 'for-verification')
            title 'Pact versions to be verified for the specified provider'
            templated true
          }
        }
      }
      given('Two consumer pacts exist for the provider', [
        provider: 'Activity Service',
        consumer1: 'Foo Web Client',
        consumer2: 'Foo Web Client 2'
      ])
      given('pact for consumer2 is pending', [
        consumer2: 'Foo Web Client 2'
      ])
      uponReceiving('a request for the provider pacts')
      withAttributes(method: 'POST', path: '/pacts/provider/Activity Service/for-verification')
      withBody(contentType: 'application/hal+json') {
        consumerVersionSelectors([
            {
              tag 'test'
              latest true
            }
        ])
        includePendingStatus false
      }
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_embedded' {
          pacts = [
            {
              shortDescription 'latest'
              'verificationProperties' {
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727 is being verified because it matches the following configured selection criterion: latest pact between a consumer and Activity Service'
                  }
                ]
                noteToDevelopers 'Please print out the text from the \'notices\' rather than using the inclusionReason and the pendingReason fields. These will be removed when this API moves out of beta.'
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727'
                  name 'Pact between Foo Web Client (0.0.0-TEST) and Activity Service'
                }
              }
            },
            {
              shortDescription 'latest'
              verificationProperties {
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/pact-version/21ac89178372169288d3f17fee9f7901d9ed5e8b is being verified because it matches the following configured selection criterion: latest pact between a consumer and Activity Service'
                  }
                ]
                noteToDevelopers 'Please print out the text from the \'notices\' rather than using the inclusionReason and the pendingReason fields. These will be removed when this API moves out of beta.'
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/pact-version/21ac89178372169288d3f17fee9f7901d9ed5e8b'
                  name 'Pact between Foo Web Client 2 (0.0.0-TEST) and Activity Service'
                }
              }
            }
          ]
        }
        '_links' {
          self {
            href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/for-verification'
            title 'Pacts to be verified'
          }
        }
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      def consumerPacts = pactBrokerClient.fetchConsumersWithSelectors('Activity Service', [
          new ConsumerVersionSelector('test', true, null, null)
      ], [], false, '')
      assert consumerPacts instanceof Ok
      assert consumerPacts.value.size == 2
      assert !consumerPacts.value[0].pending
      assert !consumerPacts.value[1].pending
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'fetch pacts when new pending pacts feature is on'() {
    given:
    pactBroker {
      uponReceiving('a request to the root')
      withAttributes(path: '/')
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_links' {
          'pb:provider-pacts-for-verification' {
            href url('http://localhost:8080', 'pacts', 'provider', '{provider}', 'for-verification')
            title 'Pact versions to be verified for the specified provider'
            templated true
          }
        }
      }
      given('Two consumer pacts exist for the provider', [
        provider: 'Activity Service',
        consumer1: 'Foo Web Client',
        consumer2: 'Foo Web Client 2'
      ])
      given('pact for consumer2 is pending', [
        consumer2: 'Foo Web Client 2'
      ])
      uponReceiving('a request for the provider pacts')
      withAttributes(method: 'POST', path: '/pacts/provider/Activity Service/for-verification')
      withBody(contentType: 'application/hal+json') {
        consumerVersionSelectors([
          {
            tag 'test'
            latest true
          }
        ])
        providerVersionTags(['master'])
        includePendingStatus true
      }
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_embedded' {
          pacts = [
            {
              shortDescription 'latest'
              'verificationProperties' {
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727 is being verified because it matches the following configured selection criterion: latest pact between a consumer and Activity Service'
                  }
                ]
                noteToDevelopers 'Please print out the text from the \'notices\' rather than using the inclusionReason and the pendingReason fields. These will be removed when this API moves out of beta.'
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727'
                  name 'Pact between Foo Web Client (0.0.0-TEST) and Activity Service'
                }
              }
            },
            {
              shortDescription 'latest'
              verificationProperties {
                pending true
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact-dev.dius.com.au/pacts/provider/Bar/consumer/Foo/pact-version/dd222221d7d3d915ec6315ca3ebbd76831aab6a3 is being verified because it matches the following configured selection criterion: latest pact between a consumer and Bar'
                  },
                  {
                    when 'before_verification'
                    text 'This pact is in pending state for this version of Bar because a successful verification result for Bar has not yet been published. If this verification fails, it will not cause the overall build to fail. Read more at https://pact.io/pending'
                  },
                  {
                    when 'after_verification:success_true_published_false'
                    text 'This pact is still in pending state for Bar as the successful verification results have not yet been published.'
                  },
                  {
                    when 'after_verification:success_false_published_false'
                    text 'This pact is still in pending state for Bar as a successful verification result has not yet been published'
                  }
                ]
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/pact-version/21ac89178372169288d3f17fee9f7901d9ed5e8b'
                  name 'Pact between Foo Web Client 2 (0.0.0-TEST) and Activity Service'
                }
              }
            }
          ]
        }
        '_links' {
          self {
            href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/for-verification'
            title 'Pacts to be verified'
          }
        }
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      def consumerPacts = pactBrokerClient.fetchConsumersWithSelectors('Activity Service', [
        new ConsumerVersionSelector('test', true, null, null)
      ], ['master'], true, '')
      assert consumerPacts instanceof Ok
      assert consumerPacts.value.size == 2
      assert !consumerPacts.value[0].pending
      assert consumerPacts.value[1].pending
    }

    then:
    result instanceof PactVerificationResult.Ok
  }

  def 'fetch pacts when wip pacts feature is on'() {
    given:
    pactBroker {
      uponReceiving('a request to the root')
      withAttributes(path: '/')
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_links' {
          'pb:provider-pacts-for-verification' {
            href url('http://localhost:8080', 'pacts', 'provider', '{provider}', 'for-verification')
            title 'Pact versions to be verified for the specified provider'
            templated true
          }
        }
      }
      given('Two consumer pacts exist for the provider', [
        provider: 'Activity Service',
        consumer1: 'Foo Web Client',
        consumer2: 'Foo Web Client 2'
      ])
      given('pact for consumer2 is wip', [
        consumer2: 'Foo Web Client 2'
      ])
      uponReceiving('a request for the provider pacts')
      withAttributes(method: 'POST', path: '/pacts/provider/Activity Service/for-verification')
      withBody(contentType: 'application/hal+json') {
        consumerVersionSelectors([
          {
            tag 'test'
            latest true
          }
        ])
        providerVersionTags(['master'])
        includePendingStatus true
        includeWipPactsSince '2020-06-24'
      }
      willRespondWith(status: 200)
      withBody(contentType: 'application/hal+json') {
        '_embedded' {
          pacts = [
            {
              shortDescription 'latest'
              'verificationProperties' {
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727 is being verified because it matches the following configured selection criterion: latest pact between a consumer and Activity Service'
                  }
                ]
                noteToDevelopers 'Please print out the text from the \'notices\' rather than using the inclusionReason and the pendingReason fields. These will be removed when this API moves out of beta.'
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727'
                  name 'Pact between Foo Web Client (0.0.0-TEST) and Activity Service'
                }
              }
            },
            {
              shortDescription 'latest'
              'verificationProperties' {
                pending true
                wip true
                notices = [
                  {
                    when 'before_verification'
                    text 'The pact at https://test.pact-dev.dius.com.au/pacts/provider/Bar/consumer/Foo/pact-version/dd222221d7d3d915ec6315ca3ebbd76831aab6a3  is being verified because it is a \'work in progress\' pact (ie. it is the pact for the latest versions of Foo tagged with \'feature-x\' and is still in pending state). Read more at https//pact.io/wip'
                  },
                  {
                    when 'before_verification'
                    text 'This pact is in pending state for this version of Bar because a successful verification result for Bar has not yet been published. If this verification fails, it will not cause the overall build to fail. Read more at https://pact.io/pending'
                  },
                  {
                    when 'after_verification:success_true_published_false'
                    text 'This pact is still in pending state for Bar as the successful verification results have not yet been published.'
                  },
                  {
                    when 'after_verification:success_false_published_false'
                    text 'This pact is still in pending state for Bar as a successful verification result has not yet been published'
                  },
                  {
                    when 'after_verification:success_true_published_true'
                    text 'This pact is no longer in pending state for Bar, as a successful verification result with this tag has been published. If a verification for a version with  fails in the future, it will fail the build. Read more at https//pact.io/pending'
                  },
                  {
                    when 'after_verification:success_false_published_true'
                    text 'This pact is still in pending state for Bar as the successful verification results have not yet been published.'
                  }
                ]
                noteToDevelopers 'Please print out the text from the \'notices\' rather than using the inclusionReason and the pendingReason fields. These will be removed when this API moves out of beta.'
              }
              '_links' {
                self {
                  href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/pact-version/21ac89178372169288d3f17fee9f7901d9ed5e8b'
                  name 'Pact between Foo Web Client 2 (0.0.0-TEST) and Activity Service'
                }
              }
            }
          ]
        }
        '_links' {
          self {
            href 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/for-verification'
            title 'Pacts to be verified'
          }
        }
      }
    }

    when:
    def result = pactBroker.runTest { server, context ->
      def consumerPacts = pactBrokerClient.fetchConsumersWithSelectors('Activity Service', [
        new ConsumerVersionSelector('test', true, null, null)
      ], ['master'], true, '2020-06-24')
      assert consumerPacts instanceof Ok
      assert consumerPacts.value.size == 2
      assert !consumerPacts.value[0].wip
      assert consumerPacts.value[1].wip
    }

    then:
    result instanceof PactVerificationResult.Ok
  }
}
