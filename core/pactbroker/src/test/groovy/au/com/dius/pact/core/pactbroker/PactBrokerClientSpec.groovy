package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.support.Auth
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
import spock.util.environment.RestoreSystemProperties

import javax.net.ssl.SSLHandshakeException

@SuppressWarnings(['UnnecessaryGetter', 'LineLength'])
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

  def 'fetching consumers with the old auth format'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: [
      'http://pactBrokerUrl', MapsKt.mapOf(new Pair('authentication', ['Basic', '1', '2'])),
      new PactBrokerClientConfig()]) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'fetching consumers with the new auth format'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: [
      'http://pactBrokerUrl', MapsKt.mapOf(new Pair('authentication', new Auth.BasicAuthentication('u', 'p'))),
      new PactBrokerClientConfig()]) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'when fetching consumers for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider', [], [], false, '').value

    then:
    consumers == []
  }

  def 'when fetching consumers, does not decode the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b/100+ab']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider', [], [], false, '').value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a%20b/100+ab'
  }

  def 'fetches consumers with specified tag successfully'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true, null, null) ], [], false, '').value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'fetches consumers with more than one tag successfully'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true, null, null),
              new ConsumerVersionSelector('anotherTag', true, null, null) ], [], false, '').value

    then:
    consumers.size() == 2

    consumers.first()
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
    consumers.first().tag == 'tag'

    consumers.last()
    consumers.last().name == 'bob'
    consumers.last().source == 'http://bob.com/'
    consumers.last().tag == 'anotherTag'
  }

  def 'when fetching consumers with specified tag, does not decode the URLs to the pacts'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> args[1].accept([name: 'bob', href: 'http://bob.com/a%20b/100+ab']) }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
            [ new ConsumerVersionSelector('tag', true, null, null) ], [], false, '').value

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/a%20b/100+ab'
  }

  def 'when fetching consumers with specified tag for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.navigate(_, _) >> halClient
    halClient.forAll(_, _) >> { args -> throw new NotFoundHalResponse() }

    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithSelectors('provider',
      [ new ConsumerVersionSelector('tag', true, null, null) ], [], false, '').value

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
      ['provider': 'Provider', 'consumer': 'Foo Consumer', 'consumerApplicationVersion': '10.0.0'],
      pactContents) >> new Ok(false)
    !result.value
  }

  def 'No need to encode the provider name, consumer name, tags and version when uploading a pact'() {
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
      ['provider': 'Provider/A', 'consumer': 'Foo Consumer/A',
       'consumerApplicationVersion': '10.0.0/B'], pactContents) >> new Ok(true)
    1 * halClient.putJson('pb:pacticipant-version-tag',
            ['pacticipant': 'Foo Consumer/A', 'version': '10.0.0/B', 'tag': 'A/B'], '{}')
  }

  @Issue('#892')
  def 'when uploading a pact with tags, publish the tags first'() {
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
      ['pacticipant': 'Foo Consumer/A', 'version': '10.0.0/B', 'tag': 'A/B'], '{}') >> new Ok(true)

    then:
    1 * halClient.putJson('pb:publish-pact',
      ['provider': 'Provider/A', 'consumer': 'Foo Consumer/A',
      'consumerApplicationVersion': '10.0.0/B'], pactContents) >> new Ok(true)
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
    client.publishVerificationResults(attributes, new TestResult.Ok(), '0', null).class.simpleName == result

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
      a: new JsonValue.StringValue('a'.chars),
      b: new JsonValue.Integer('100'.chars),
      _links: new JsonValue.Object(),
      c: new JsonValue.Array([
        JsonValue.True.INSTANCE, new JsonValue.Decimal('10.2'.chars), new JsonValue.StringValue('test'.chars)
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
    IHalClient halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [ new ConsumerVersionSelector('DEV', true, null, null) ]
    def json = '{"consumerVersionSelectors":[{"latest":true,"tag":"DEV"}],"includePendingStatus":false}'
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
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], false, '')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> 'URL'
    1 * halClient.postJson('pb:provider-pacts-for-verification', [provider: 'provider'], json) >> new Ok(jsonResult)
    result instanceof Ok
    result.value.first() == new PactBrokerResult('Pact between Foo Web Client (1.0.2) and Activity Service',
      'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client/pact-version/384826ff3a2856e28dfae553efab302863dcd727',
      'baseUrl', [], [
        new VerificationNotice('before_verification',
         'The pact at ... is being verified because it matches the following configured selection criterion: latest pact for a consumer version tagged \'DEV\'')
      ],
      false, null, false, true, null
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
    def result = client.fetchConsumersWithSelectors('provider', [], [], false, '')

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
    def result = client.fetchConsumersWithSelectors('provider', [], [], false, '')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> null
    1 * halClient.linkUrl('beta:provider-pacts-for-verification') >> null
    0 * halClient.postJson(_, _, _)
    1 * client.fetchConsumers('provider') >> []
    result instanceof Ok
  }

  def 'fetching pacts with selectors does not include wip pacts when pending parameter is false'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [ new ConsumerVersionSelector('DEV', true, null, null) ]
    def json = '{"consumerVersionSelectors":[{"latest":true,"tag":"DEV"}],"includePendingStatus":false}'
    def jsonResult = JsonParser.INSTANCE.parseString('''
    {
      "_embedded": {
        "pacts": [
        ]
      }
    }
    ''')
    when:
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], false, '2020-24-06')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> 'URL'
    1 * halClient.postJson('pb:provider-pacts-for-verification', [provider: 'provider'], json) >> new Ok(jsonResult)
    result instanceof Ok
  }

  def 'fetching pacts with selectors includes wip pacts when parameter not blank'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [ new ConsumerVersionSelector('DEV', true, null, null) ]
    def json = '{"consumerVersionSelectors":[{"latest":true,"tag":"DEV"}],"includePendingStatus":true,' +
      '"includeWipPactsSince":"2020-24-06","providerVersionTags":[]}'
    def jsonResult = JsonParser.INSTANCE.parseString('''
    {
      "_embedded": {
        "pacts": [
        ]
      }
    }
    ''')
    when:
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], true, '2020-24-06')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> 'URL'
    1 * halClient.postJson('pb:provider-pacts-for-verification', [provider: 'provider'], json) >> new Ok(jsonResult)
    result instanceof Ok
  }

  @Issue('#1227')
  def 'when falling back to the previous implementation, filter out null tag values from the selectors'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def result = client.fetchConsumersWithSelectors('provider',
      [ new ConsumerVersionSelector(null, true, 'consumer', null) ], [], false, '')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> null
    1 * halClient.linkUrl('beta:provider-pacts-for-verification') >> null
    0 * halClient.postJson(_, _, _)
    1 * client.fetchConsumers('provider') >> []
    result instanceof Ok
  }

  @Issue('#1241')
  def 'can i deploy - should retry when there are unknown results'() {
    given:
    def halClient = Mock(IHalClient)
    def config = new PactBrokerClientConfig(10, 0)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl', [:], config]) {
      newHalClient() >> halClient
    }
    def json1 = JsonParser.parseString('''
    |{
    |  "summary": {
    |      "deployable": null,
    |      "reason": "some text",
    |      "unknown": 1
    |  }
    |}'''.stripMargin())
    def json2 = JsonParser.parseString('''
    |{
    |  "summary": {
    |      "deployable": true,
    |      "reason": "some text",
    |      "unknown": 0
    |  }
    |}'''.stripMargin())

    when:
    def result = client.canIDeploy('test', '1.2.3', new Latest.UseLatest(true), '')

    then:
    3 * halClient.getJson(_, _) >> new Ok(json1) >> new Ok(json1) >> new Ok(json2)
    result.ok
  }

  @Issue('#1264')
  def 'fetching pacts with selectors when falling back to the previous implementation, use fallback tags if present'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [
      new ConsumerVersionSelector('DEV', true, null, 'MASTER')
    ]

    when:
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], false, '')

    then:
    1 * halClient.navigate() >> halClient
    1 * halClient.linkUrl('pb:provider-pacts-for-verification') >> null
    1 * halClient.linkUrl('beta:provider-pacts-for-verification') >> null
    0 * halClient.postJson(_, _, _)
    1 * client.fetchConsumersWithTag('provider', 'DEV') >> []
    1 * client.fetchConsumersWithTag('provider', 'MASTER') >> []
    result instanceof Ok
  }

  @Issue('#1322')
  def 'when fetching pacts fails with a certificate error'() {
    given:
    def halClient = Mock(IHalClient)
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def selectors = [
      new ConsumerVersionSelector('DEV', true, null, 'MASTER')
    ]

    when:
    def result = client.fetchConsumersWithSelectors('provider', selectors, [], false, '')

    then:
    1 * halClient.navigate() >> {
      throw new InvalidNavigationRequest('PKIX path building failed',
        new SSLHandshakeException('PKIX path building failed'))
    }
    notThrown(SSLHandshakeException)
    result instanceof Err
    result.component2() instanceof InvalidNavigationRequest
  }

  def 'when publishing provider tags, return an Ok result if it succeeds'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.withDocContext(_) >> halClient
    halClient.navigate(_) >> halClient
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }
    def attributes = [:]
    halClient.putJson('pb:version-tag', _, _) >> new Ok(true)

    expect:
    client.publishProviderTags(attributes, 'provider', ['0'], 'null') == new Ok(true)
  }

  def 'when publishing provider tags, return an error result if any tag fails'() {
    given:
    def halClient = Mock(IHalClient)
    halClient.withDocContext(_) >> halClient
    halClient.navigate(_) >> halClient
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    when:
    def result = client.publishProviderTags([:], 'provider', ['0', '1', '2'], 'null')

    then:
    1 * halClient.putJson('pb:version-tag', [version: 'null', tag: '0'], _) >> new Ok(true)
    1 * halClient.putJson('pb:version-tag', [version: 'null', tag: '1'], _) >> new Err(new RuntimeException('failed'))
    1 * halClient.putJson('pb:version-tag', [version: 'null', tag: '2'], _) >> new Ok(true)
    result == new Err(["Publishing tag '1' failed: failed"])
  }

  @RestoreSystemProperties
  def 'fetches provider pacts for verification based on selectors raw json configuration passed from cli'() {
    given:
    System.setProperty('pactbroker.consumerversionselectors.rawjson', '[{"mainBranch":true}]')
    def halClient = Mock(IHalClient)
    halClient.navigate() >> halClient
    halClient.linkUrl('pb:provider-pacts-for-verification') >> 'pb:provider-pacts-for-verification'
    PactBrokerClient client = Spy(PactBrokerClient, constructorArgs: ['baseUrl']) {
      newHalClient() >> halClient
    }

    def expectedJson = '{"consumerVersionSelectors":[{"mainBranch":true}],"includePendingStatus":false}'

    when:
    client.fetchConsumersWithSelectors('provider', [], [], false, '')

    then:
    1 * halClient.postJson('pb:provider-pacts-for-verification', _, expectedJson)
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'can-i-deploy - matrix query'() {
    expect:
    PactBrokerClient.internalBuildMatrixQuery(pacticipant, pacticipantVersion, (Latest) latest, to, ignore) == result

    where:

    pacticipant  | pacticipantVersion | latest                             | to        | ignore                                                              || result
    'Test'       | ''                 | new Latest.UseLatest(true)         | null      | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true'
    'Test'       | '100'              | new Latest.UseLatest(false)        | null      | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][version]=100&latest=true'
    'Test'       | ''                 | new Latest.UseLatestTag('tst')     | null      | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][tag]=tst&latest=true'
    'Test'       | ''                 | new Latest.UseLatest(true)         | 'tst'     | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true&tag=tst'
    'Test 1 2 3' | ''                 | new Latest.UseLatest(true)         | null      | []                                                                  || 'q[][pacticipant]=Test+1+2+3&latestby=cvp&q[][latest]=true&latest=true'
    'Test'       | '1 0 0'            | new Latest.UseLatest(false)        | null      | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][version]=1+0+0&latest=true'
    'Test'       | ''                 | new Latest.UseLatestTag('tst 3/4') | null      | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][tag]=tst+3%2F4&latest=true'
    'Test'       | ''                 | new Latest.UseLatest(true)         | 'tst 3/4' | []                                                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true&tag=tst+3%2F4'
    'Test'       | ''                 | new Latest.UseLatest(true)         | null      | [new IgnoreSelector('bob', null)]                                   || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true&ignore[][pacticipant]=bob'
    'Test'       | ''                 | new Latest.UseLatest(true)         | null      | [new IgnoreSelector('bob', '100')]                                  || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true&ignore[][pacticipant]=bob&ignore[][version]=100'
    'Test'       | ''                 | new Latest.UseLatest(true)         | null      | [new IgnoreSelector('bob', null), new IgnoreSelector('fred', null)] || 'q[][pacticipant]=Test&latestby=cvp&q[][latest]=true&latest=true&ignore[][pacticipant]=bob&ignore[][pacticipant]=fred'
  }

  @Issue('#1511')
  def 'can-i-deploy - matrix query - encodes + correctly'() {
    expect:
    PactBrokerClient.internalBuildMatrixQuery('test', '0.0.1+4a2a964',
      new Latest.UseLatest(false), null, []) == 'q[][pacticipant]=test&latestby=cvp&q[][version]=0.0.1%2B4a2a964&latest=true'
  }

  def 'publishing pact with new publish endpoint'() {
    given:
    def mockHalClient = Mock(IHalClient)
    def providerName = 'provider'
    def consumerName = 'consumer'
    def config = new PublishConfiguration('1.0.0', [], 'test', 'http://123')
    def pactText = '{}'
    def jsonBody = '{"branch":"test","buildUrl":"http://123","contracts":[{"consumerName":"consumer",' +
      '"content":"e30=","contentType":"application/json","providerName":"provider","specification":"pact"}],' +
      '"pacticipantName":"consumer","pacticipantVersionNumber":"1.0.0","tags":[]}'

    when:
    pactBrokerClient.publishContract(mockHalClient, providerName, consumerName, config, pactText)

    then:
    1 * mockHalClient.postJson(PactBrokerClient.PUBLISH_CONTRACTS_LINK, [:], jsonBody) >> new Ok(new JsonValue.Object([:]))
  }

  @RestoreSystemProperties
  def 'publishing pact with new publish endpoint - with the branch as a system property'() {
    given:
    def mockHalClient = Mock(IHalClient)
    def providerName = 'provider'
    def consumerName = 'consumer'
    def config = new PublishConfiguration('1.0.0')
    def pactText = '{}'
    def jsonBody = '{"branch":"feat/mine","contracts":[{"consumerName":"consumer",' +
      '"content":"e30=","contentType":"application/json","providerName":"provider","specification":"pact"}],' +
      '"pacticipantName":"consumer","pacticipantVersionNumber":"1.0.0","tags":[]}'
    System.setProperty('pact.publish.consumer.branchName', 'feat/mine')

    when:
    pactBrokerClient.publishContract(mockHalClient, providerName, consumerName, config, pactText)

    then:
    1 * mockHalClient.postJson(PactBrokerClient.PUBLISH_CONTRACTS_LINK, [:], jsonBody) >> new Ok(new JsonValue.Object([:]))
  }
}
