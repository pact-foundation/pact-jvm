package au.com.dius.pact.server

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Request
import scala.collection.JavaConverters
import spock.lang.Issue
import spock.lang.Specification
import unfiltered.request.HttpRequest

class ConversionsSpec extends Specification {

  @Issue('#1008')
  def 'unfilteredRequestToPactRequest - handles the case where there is no content type header'() {
    given:
    def httpRequest = Mock(HttpRequest) {
      headers(_) >> JavaConverters.asScalaIterator([].iterator())
      headerNames() >> JavaConverters.asScalaIterator([].iterator())
      uri() >> '/'
      parameterNames() >> JavaConverters.asScalaIterator([].iterator())
      method() >> 'GET'
      inputStream() >> new ByteArrayInputStream('BOOH!'.bytes)
    }

    when:
    Request request = Conversions$.MODULE$.unfilteredRequestToPactRequest(httpRequest)

    then:
    request.body.contentType == ContentType.TEXT_PLAIN
  }
}
