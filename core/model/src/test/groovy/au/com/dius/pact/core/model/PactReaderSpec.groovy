package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.json.JsonParser
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.classic.RedirectExec
import org.apache.hc.client5.http.protocol.RedirectStrategy
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import static au.com.dius.pact.core.model.generators.Category.BODY

@SuppressWarnings('DuplicateMapLiteral')
class PactReaderSpec extends Specification {

  def 'loads a pact with no metadata as V2'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')

      when:
      def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

      then:
      pact instanceof RequestResponsePact
      pact.source instanceof UrlPactSource
      pact.metadata == [pactSpecification: [version: '2.0.0'], 'pact-jvm': [version: '']]
  }

  def 'loads a pact with V1 version using existing loader'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v1-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)
    def interaction = pact.interactions.first()

    then:
    pact instanceof RequestResponsePact
    pact.source instanceof UrlPactSource
    pact.metadata == [pactSpecification: [version: '2.0.0'], 'pact-jvm': [version: '']]

    interaction instanceof RequestResponseInteraction
    interaction.response.headers['Content-Type'] == ['text/html']
    interaction.response.headers['access-control-allow-credentials'] == ['true']
    interaction.response.headers['access-control-allow-headers'] == ['Content-Type', 'Authorization']
    interaction.response.headers['access-control-allow-methods'] == ['POST', 'GET', 'PUT', 'HEAD', 'DELETE', 'OPTIONS',
                                                                     'PATCH']
  }

  def 'loads a pact with V2 version using existing loader'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)
    def interaction = pact.interactions.first()

    then:
    pact instanceof RequestResponsePact
    pact.metadata == [pactSpecification: [version: '2.0.0'], 'pact-jvm': [version: '']]

    interaction instanceof RequestResponseInteraction
    interaction.response.headers['Content-Type'] == ['text/html']
    interaction.response.headers['access-control-allow-credentials'] == ['true']
    interaction.response.headers['access-control-allow-headers'] == ['Content-Type', 'Authorization']
    interaction.response.headers['access-control-allow-methods'] == ['POST', 'GET', 'PUT', 'HEAD', 'DELETE', 'OPTIONS',
                                                                     'PATCH']
  }

  def 'loads a pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact.json')

      when:
      def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

      then:
      pact instanceof RequestResponsePact
      pact.source instanceof UrlPactSource
      pact.metadata == [pactSpecification: [version: '3.0.0'], 'pact-jvm': [version: '']]
  }

  def 'loads a pact with old version format'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact-old-format.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.source instanceof UrlPactSource
    pact.metadata == [pactSpecification: [version: '3.0.0'], 'pact-jvm': [version: '']]
  }

  def 'loads a message pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-message-pact.json')

      when:
      def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

      then:
      pact instanceof MessagePact
      pact.source instanceof UrlPactSource
      pact.metadata.pactSpecification == [version: '3.0.0']
  }

  def 'loads a pact from an inputstream'() {
      given:
      def pactInputStream = PactReaderSpec.classLoader.getResourceAsStream('pact.json')

      when:
      def pact = DefaultPactReader.INSTANCE.loadPact(pactInputStream)

      then:
      pact instanceof RequestResponsePact
      pact.source instanceof InputStreamPactSource
  }

  def 'loads a pact from a json string'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact.json').text

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactString)

    then:
    pact instanceof RequestResponsePact
    pact.source instanceof UnknownPactSource
  }

  def 'throws an exception if it can not load the pact file'() {
    given:
    def pactString = 'this is not a pact file!'

    when:
    DefaultPactReader.INSTANCE.loadPact(pactString)

    then:
    thrown(UnsupportedOperationException)
  }

  def 'handles invalid version metadata'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact-invalid-version.json').text

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactString)

    then:
    pact instanceof RequestResponsePact
    pact.metadata == [pactSpecification: [version: '2.0.0'], 'pact-jvm': [version: '']]
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'if authentication is set, sets up the http client with auth'() {
    given:
    def pactUrl = new UrlSource('http://url.that.requires.auth:8080/')

    when:
    def client = PactReaderKt.newHttpClient(pactUrl.url, [authentication: ['basic', 'user', 'pwd']])
    def creds = client.credentialsProvider.credMap.entrySet().first().getValue()

    then:
    client.credentialsProvider instanceof BasicCredentialsProvider
    creds.principal.username == 'user'
    creds.password == 'pwd'.toCharArray()
  }

  def 'custom retry strategy is added to execution chain of client'() {
    given:
    def pactUrl = new UrlSource('http://some.url/')

    when:
    def client = PactReaderKt.newHttpClient(pactUrl.url, [:])

    then:
    client.execChain.handler instanceof RedirectExec
    client.execChain.handler.redirectStrategy instanceof RedirectStrategy
  }

  def 'correctly loads V2 pact query strings'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2_pact_query.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.query == [q: ['p', 'p2'], r: ['s']]
    pact.interactions[1].request.query == [datetime: ['2011-12-03T10:15:30+01:00'], description: ['hello world!']]
    pact.interactions[2].request.query == [options: ['delete.topic.enable=true'], broker: ['1']]
  }

  def 'Defaults to V3 pact provider states'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('test_pact_v3.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].providerStates == [
      new ProviderState('test state', [name: 'Testy']),
      new ProviderState('test state 2', [name: 'Testy2'])
    ]
  }

  def 'Falls back to the to V2 pact provider state'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('test_pact_v3_old_provider_state.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].providerStates == [ new ProviderState('test state') ]
  }

  def 'correctly load pact file from S3'() {
    given:
    def pactUrl = 'S3://some/bucket/aFile.json'
    AmazonS3 s3ClientMock = Mock(AmazonS3)
    String pactJson = this.class.getResourceAsStream('/v2-pact.json').text
    S3Object object = Mock()
    object.objectContent >> new S3ObjectInputStream(new ByteArrayInputStream(pactJson.bytes), null)
    DefaultPactReader.INSTANCE.s3Client = s3ClientMock

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    1 * s3ClientMock.getObject('some', 'bucket/aFile.json') >> object
    pact instanceof RequestResponsePact
    pact.source instanceof S3PactSource
  }

  def 'reads from classpath inside jar'() {
    given:
    def pactUrl = 'classpath:jar-pacts/test_pact_v3.json'

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].providerStates == [
        new ProviderState('test state', [name: 'Testy']),
        new ProviderState('test state 2', [name: 'Testy2'])
    ]
  }

  def 'throws a meaningful exception when reading from non-existent classpath'() {
    given:
    def pactUrl = 'classpath:no_such_pact.json'

    when:
    DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    def e = thrown(RuntimeException)
    e.message.contains('no_such_pact.json')
  }

  def 'correctly loads V2 pact with string bodies'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('test_pact_with_string_body.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.body.valueAsString() == '"This is a string"'
    pact.interactions[0].response.body.valueAsString() == '"This is a string"'
  }

  def 'loads a pact where the source is a closure'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(new ClosurePactSource({ pactUrl }))

    then:
    pact instanceof RequestResponsePact
    pact.source instanceof UrlPactSource
  }

  def 'when loading a pact with V2 version from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact-broker.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

  def 'when loading a pact with V3 version from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact-broker.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

  def 'when loading a message pact from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('message-pact-broker.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof MessagePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

  @Unroll
  def 'determining pact spec version'() {
    expect:
    DefaultPactReader.INSTANCE.determineSpecVersion(JsonParser.INSTANCE.parseString(json).asObject()) == version

    where:

    json                                                      | version
    '{}'                                                      | '2.0.0'
    '{"metadata":{}}'                                         | '2.0.0'
    '{"metadata":{"pactSpecificationVersion":"1.2.3"}}'       | '1.2.3'
    '{"metadata":{"pactSpecification":"1.2.3"}}'              | '2.0.0'
    '{"metadata":{"pactSpecification":{}}}'                   | '2.0.0'
    '{"metadata":{"pactSpecification":{"version":"1.2.3"}}}'  | '1.2.3'
    '{"metadata":{"pactSpecification":{"version":"3.0"}}}'    | '3.0'
    '{"metadata":{"pact-specification":{"version":"1.2.3"}}}' | '1.2.3'
  }

  @Issue('#1031')
  @SuppressWarnings('GStringExpressionWithinString')
  def 'handle encoded values in the pact file'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('encoded-values-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.body.valueAsString() ==
      '{"entityName":"mock-name","xml":"<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n"}'
    pact.interactions[0].request.generators.categories[BODY]['$'].expression ==
      '{\n  "entityName": "${eName}",\n  "xml": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n"\n}'
  }

  @Issue('#1070')
  def 'loading pact displays warning and does not load rules correctly'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('1070-ApiConsumer-ApiProvider.json')
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('/api/test/\\d{1,8}'), RuleLogic.OR)

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.matchingRules == matchingRules
  }

  @Issue('#1110')
  @SuppressWarnings('LineLength')
  def 'handle multipart form post bodies'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('pact-multipart-form-post.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.determineContentType().baseType == 'multipart/form-data'
    pact.interactions[0].request.body.valueAsString().startsWith('--lk9eSoRxJdPHMNbDpbvOYepMB0gWDyQPWo\r\nContent-Disposition: form-data; name="photo"; filename="ron.jpg"\r\nContent-Type: image/jpeg')
  }
}
