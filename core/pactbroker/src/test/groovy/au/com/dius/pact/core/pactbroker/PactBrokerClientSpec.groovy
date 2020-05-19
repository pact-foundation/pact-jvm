package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlin.Pair
import kotlin.collections.MapsKt
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents

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
  }

  def 'when fetching consumers, sets the auth if there is any'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: [
      'http://pactBrokerUrl', MapsKt.mapOf(new Pair('authentication', ['Basic', '1', '2']))]) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider', [], [], false).value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider', [], [], false).value

    then:
    consumers == []
  }

  def 'when fetching consumers, decodes the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b/100+ab']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider', [], [], false).value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a b/100+ab'
  }

  def 'fetches consumers with specified tag successfully'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true) ], [], false).value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'when fetching consumers with specified tag, sets the auth if there is any'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    def client = Spy(PactBrokerClient, constructorArgs: [
      'http://pactBrokerUrl', MapsKt.mapOf(new Pair('authentication', ['Basic', '1', '2']))]) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true) ], [], false).value

    then:
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers with specified tag, decodes the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b/100+ab']) }

    def client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true) ], [], false).value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a b/100+ab'
  }

  def 'when fetching consumers with specified tag for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
      [ new ConsumerVersionSelector('tag', true) ], [], false).value

    then:
    consumers == []
  }

  def 'returns an error when uploading a pact fails'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def result = client.uploadPactFile(pactFile, '10.0.0')

    then:
    1 * halClient.putJson('pb:publish-pact',
      ['provider': 'Provider', 'consumer': 'Foo%20Consumer', 'consumerApplicationVersion': '10.0.0'],
      pactContents) >> new Ok(false)
    !result.value
  }

  def 'encode the provider name, consumer name, tags and version when uploading a pact'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def tag = 'A/B'
    pactContents = '''
      {
          "provider" : {
              "name" : "Provider/A"
          },
          "consumer" : {
              "name" : "Foo Consumer/A"
          },
          "interactions" : []
      }
    '''
    pactFile.write pactContents

    when:
    client.uploadPactFile(pactFile, '10.0.0/B', [tag])

    then:
    1 * halClient.putJson('pb:publish-pact',
      ['provider': 'Provider%2FA', 'consumer': 'Foo%20Consumer%2FA',
       'consumerApplicationVersion': '10.0.0%2FB'], pactContents) >> new Ok(true)
    1 * halClient.putJson('pb:pacticipant-version-tag',
            ['pacticipant': 'Foo%20Consumer%2FA', 'version': '10.0.0%2FB', 'tag': 'A/B'], '{}')
  }

  @Issue('#892')
  def 'when uploading a pact a pact with tags, publish the tags first'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    def client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def tag = 'A/B'
    pactContents = '''
      {
          "provider" : {
              "name" : "Provider/A"
          },
          "consumer" : {
              "name" : "Foo Consumer/A"
          },
          "interactions" : []
      }
    '''
    pactFile.write pactContents

    when:
    client.uploadPactFile(pactFile, '10.0.0/B', [tag])

    then:
    1 * halClient.putJson('pb:pacticipant-version-tag',
      ['pacticipant': 'Foo%20Consumer%2FA', 'version': '10.0.0%2FB', 'tag': 'A/B'], '{}') >> new Ok(true)

    then:
    1 * halClient.putJson('pb:publish-pact',
      ['provider': 'Provider%2FA', 'consumer': 'Foo%20Consumer%2FA',
      'consumerApplicationVersion': '10.0.0%2FB'], pactContents) >> new Ok(true)
  }

  @Unroll
  def 'when publishing verification results, return a #result if #reason'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    halClient.postJson('URL', _) >> new Ok(true)

    expect:
    client.publishVerificationResults(attributes, TestResult.Ok.INSTANCE, '0', null).class.simpleName == result

    where:

    reason                              | attributes                                         | result
    'there is no verification link'     | [:]                                                | Err.simpleName
    'the verification link has no href' | ['pb:publish-verification-results': [:]]           | Err.simpleName
    'the broker client returns success' | ['pb:publish-verification-results': [href: 'URL']] | Ok.simpleName
    'the links have different case'     | ['pb:Publish-Verification-Results': [HREF: 'URL']] | Ok.simpleName
  }

  def 'when fetching a pact, return the results as a Map'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def url = 'https://test.pact.dius.com.au' +
      '/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/version/1.0.2'
    def values = [
      a: new JsonValue.StringValue('a'),
      b: new JsonValue.Integer(100),
      _links: new JsonValue.Object(),
      c: new JsonValue.Array([
        JsonValue.True.INSTANCE, new JsonValue.Decimal(10.2), new JsonValue.StringValue('test')
      ])
    ]
    def json = new JsonValue.Object(values)

    when:
    def result = client.fetchPact(url, true)

    then:
    1 * halClient.fetch(url, _) >> new Ok(json)
    result.pactFile == Json.INSTANCE.toJson([a: 'a', b: 100, _links: [:], c: [true, 10.2, 'test']])
  }

  def 'publishing verification results with an exception should support any type of exception'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def uploadResult = new Ok(true)
    halClient.postJson(_, _) >> uploadResult
    def result = new TestResult.Failed([
      [exception: new AssertionError('boom')]
    ], 'Failed')
    def doc = ['pb:publish-verification-results': [href: '']]

    expect:
    client.publishVerificationResults(doc, result, '0', null) == uploadResult
  }

  @SuppressWarnings('LineLength')
  def 'fetching pacts with selectors uses the provider-pacts-for-verification link and returns a list of results'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [ new ConsumerVersionSelector('DEV', true) ]
    def json = '{"consumerVersionSelectors":[{"tag":"DEV","latest":true}]}'
    def jsonResult = JsonParser.INSTANCE.parseString('''
      {
        "_embedded": {
          "pacts": [
            {
              "shortDescription": "latest DEV",
              "verificationProperties": {
                "notices": [
                  {
                    "when": "before_verification",
                    "text": "The pact at ... is being verified because it matches the following configured selection criterion: latest pact for a consumer version tagged 'DEV'"
                  }
                ]
              },
              "_links": {
                "self": {
                  "href": "https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727",
                  "name": "Pact between Foo Web Client (1.0.2) and Activity Service"
                }
              }
            }
          ]
        }
      }
    ''')

    when:
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], false)

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> 'URL'
    1 * halClient.postJson('pb:provider-pacts-for-verification', [provider: 'provider'], json) >> new Ok(jsonResult)
    result instanceof Ok
    result.value.first() == new PactBrokerResult('Pact between Foo Web Client (1.0.2) and Activity Service',
      'https://test.pact.dius.com.au/pacts/provider/Activity Service/consumer/Foo Web Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727',
      'baseUrl', [], [
        new VerificationNotice('before_verification',
         'The pact at ... is being verified because it matches the following configured selection criterion: latest pact for a consumer version tagged \'DEV\'')
      ],
      false
    )
  }

  def 'fetching pacts with selectors falls back to the beta provider-pacts-for-verification link'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def jsonResult = JsonParser.INSTANCE.parseString('''
    {
      "_embedded": {
        "pacts": [
        ]
      }
    }
    ''')

    when:
    def result = client.fetchConsumersWithSelectors('provider', [], [], false)

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> null
    1 * halClient.linkUrl('beta:provider-pacts-for-verification') >> 'URL'
    1 * halClient.postJson('beta:provider-pacts-for-verification', _, _) >> new Ok(jsonResult)
    result instanceof Ok
  }

  def 'fetching pacts with selectors falls back to the previous implementation if no link is available'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def result = client.fetchConsumersWithSelectors('provider', [], [], false)

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> null
    1 * halClient.linkUrl('beta:provider-pacts-for-verification') >> null
    0 * halClient.postJson(_, _, _)
    1 * client.fetchConsumers('provider') >> []
    result instanceof Ok
  }
}
