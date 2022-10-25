package au.com.dius.pact.core.model.generators

import org.apache.hc.core5.net.WWWFormCodec
import spock.lang.Specification

import java.nio.charset.Charset

class FormUrlEncodedContentTypeHandlerSpec extends Specification {
  def 'applies the generator to the field in the body'() {
    given:
    def body = 'a=A&b=B&c=C'
    def charset = Charset.defaultCharset()
    def queryResult = new FormQueryResult(WWWFormCodec.parse(body, charset), null)
    def key = '$.b'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    FormUrlEncodedContentTypeHandler.INSTANCE.applyKey(queryResult, key, generator, [:])

    then:
    WWWFormCodec.format(queryResult.body, charset) == 'a=A&b=X&c=C'
  }

  def 'does not apply the generator when field is not in the body'() {
    def body = 'a=A&b=B&c=C'
    def charset = Charset.defaultCharset()
    def queryResult = new FormQueryResult(WWWFormCodec.parse(body, charset), null)
    def key = '$.d'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    FormUrlEncodedContentTypeHandler.INSTANCE.applyKey(queryResult, key, generator, [:])

    then:
    WWWFormCodec.format(queryResult.body, charset) == 'a=A&b=B&c=C'
  }

  def 'does not apply the generator to empty body'() {
    given:
    def body = new FormQueryResult([], null)
    def key = '$.d'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    FormUrlEncodedContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    WWWFormCodec.format(body.body, Charset.defaultCharset()) == ''
  }

  def 'applies the generator to all map entries'() {
    given:
    def body = 'a=A&b=B&c=C'
    def charset = Charset.defaultCharset()
    def queryResult = new FormQueryResult(WWWFormCodec.parse(body, charset), null)
    def key = '$.*'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    FormUrlEncodedContentTypeHandler.INSTANCE.applyKey(queryResult, key, generator, [:])

    then:
    WWWFormCodec.format(queryResult.body, charset) == 'a=X&b=X&c=X'
  }
}
