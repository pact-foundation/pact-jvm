package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import au.com.dius.pact.pactbroker.CustomServiceUnavailableRetryStrategy
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.apache.http.impl.client.BasicCredentialsProvider
import spock.lang.Specification

@SuppressWarnings('DuplicateMapLiteral')
class PactReaderSpec extends Specification {

  def setup() {
    GroovySpy(PactReader, global: true)
  }

  def 'loads a pact with no metadata as V2'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact({ it.url == pactUrl.toString() }, _)
      0 * PactReader.loadV3Pact(_, _)
      pact instanceof RequestResponsePact
      pact.source instanceof UrlPactSource
  }

  def 'loads a pact with V1 version using existing loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v1-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact({ it.url == pactUrl.toString() }, _)
      0 * PactReader.loadV3Pact(_, _)
      pact instanceof RequestResponsePact
      pact.source instanceof UrlPactSource
  }

  def 'loads a pact with V2 version using existing loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact({ it.url == pactUrl.toString() }, _)
      0 * PactReader.loadV3Pact(_, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      0 * PactReader.loadV2Pact(_, _)
      1 * PactReader.loadV3Pact({ it.url == pactUrl.toString() }, _)
      pact instanceof RequestResponsePact
      pact.source instanceof UrlPactSource
  }

  def 'loads a pact with old version format'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact-old-format.json')

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    0 * PactReader.loadV2Pact(_, _)
    1 * PactReader.loadV3Pact({ it.url == pactUrl.toString() }, _)
    pact instanceof RequestResponsePact
    pact.source instanceof UrlPactSource
  }

  def 'loads a message pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-message-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV3Pact({ it.url == pactUrl.toString() }, _)
      0 * PactReader.loadV2Pact(_, _)
      pact instanceof MessagePact
      pact.source instanceof UrlPactSource
  }

  def 'loads a pact from an inputstream'() {
      given:
      def pactInputStream = PactReaderSpec.classLoader.getResourceAsStream('pact.json')

      when:
      def pact = PactReader.loadPact(pactInputStream)

      then:
      1 * PactReader.loadV2Pact(_, _)
      0 * PactReader.loadV3Pact(_, _)
      pact instanceof RequestResponsePact
      pact.source instanceof InputStreamPactSource
  }

  def 'loads a pact from a json string'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact.json').text

    when:
    def pact = PactReader.loadPact(pactString)

    then:
    1 * PactReader.loadV2Pact(_, _)
    0 * PactReader.loadV3Pact(_, _)
    pact instanceof RequestResponsePact
    pact.source instanceof UnknownPactSource
  }

  def 'throws an exception if it can not load the pact file'() {
    given:
    def pactString = 'this is not a pact file!'

    when:
    PactReader.loadPact(pactString)

    then:
    thrown(UnsupportedOperationException)
    0 * PactReader.loadV2Pact(pactString, _)
    0 * PactReader.loadV3Pact(pactString, _)
  }

  def 'handles invalid version metadata'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact-invalid-version.json').text

    when:
    PactReader.loadPact(pactString)

    then:
    1 * PactReader.loadV2Pact(_, _)
    0 * PactReader.loadV3Pact(_, _)
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
    creds.password == 'pwd'
  }

  def 'custom retry strategy is added to execution chain of client'() {
    given:
    def pactUrl = new UrlSource('http://some.url/')

    when:
    def client = PactReaderKt.newHttpClient(pactUrl.url, [:])

    then:
    client.execChain.requestExecutor.retryStrategy instanceof CustomServiceUnavailableRetryStrategy
  }

  def 'correctly loads V2 pact query strings'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2_pact_query.json')

    when:
    def pact = PactReader.loadPact(pactUrl)

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
    def pact = PactReader.loadPact(pactUrl)

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
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].providerStates == [ new ProviderState('test state') ]
  }

  def 'correctly load pact file from S3'() {
    given:
    def pactUrl = 'S3://some/bucket/aFile.json'
    def s3ClientMock = Mock(AmazonS3Client)
    String pactJson = this.class.getResourceAsStream('/v2-pact.json').text
    S3Object object = Mock()
    object.objectContent >> new S3ObjectInputStream(new ByteArrayInputStream(pactJson.bytes), null)
    PactReader.s3Client() >> s3ClientMock

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    1 * s3ClientMock.getObject('some', 'bucket/aFile.json') >> object
    pact instanceof RequestResponsePact
    pact.source instanceof S3PactSource
  }

  def 'reads from classpath inside jar'() {
    given:
    def pactUrl = 'classpath:jar-pacts/test_pact_v3.json'

    when:
    def pact = PactReader.loadPact(pactUrl)

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
    PactReader.loadPact(pactUrl)

    then:
    def e = thrown(RuntimeException)
    e.message.contains('no_such_pact.json')
  }

  def 'correctly loads V2 pact with string bodies'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('test_pact_with_string_body.json')

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.body.valueAsString() == '"This is a string"'
    pact.interactions[0].response.body.valueAsString() == '"This is a string"'
  }

  def 'loads a pact where the source is a closure'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')

    when:
    def pact = PactReader.loadPact(new ClosurePactSource({ pactUrl }))

    then:
    1 * PactReader.loadV2Pact({ it.url == pactUrl.toString() }, _)
    0 * PactReader.loadV3Pact(_, _)
    pact instanceof RequestResponsePact
    pact.source instanceof UrlPactSource
  }

  def 'when loading a pact with V2 version from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact-broker.json')


    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

  def 'when loading a pact with V3 version from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact-broker.json')

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

  def 'when loading a message pact from the broker, it preserves the interaction ids'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('message-pact-broker.json')


    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof MessagePact
    pact.interactions.every { it.interactionId ==~ /^[a-zA-Z0-9]+$/  }
  }

}
