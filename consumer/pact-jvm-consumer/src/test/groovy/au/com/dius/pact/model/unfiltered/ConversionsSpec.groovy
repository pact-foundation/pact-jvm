package au.com.dius.pact.model.unfiltered

import scala.collection.JavaConverters
import spock.lang.Specification
import unfiltered.netty.ReceivedMessage
import unfiltered.request.HttpRequest

@SuppressWarnings('LineLength')
class ConversionsSpec extends Specification {

  HttpRequest<ReceivedMessage> request

  def setup() {
    request = Mock {
      headerNames() >> JavaConverters.asScalaIterator(['Accept'].iterator())
      headers(_) >> { args ->
        if (args[0] == 'Accept') {
          JavaConverters.asScalaIterator(['application/json'].iterator())
        } else if (args[0] == 'Content-Encoding') {
          JavaConverters.asScalaIterator([].iterator())
        }
      }
      reader() >> new StringReader('')
    }
  }

  def 'converting an unfiltered request to a pact request - construct the pact request correctly - with a query string'() {
    given:
    request.parameterNames() >> JavaConverters.asScalaIterator(['a', 'b'].iterator())
    request.parameterValues(_) >> { args ->
      if (args[0] == 'a') {
        JavaConverters.asScalaBuffer(['1']).toSeq()
      } else if (args[0] == 'b') {
        JavaConverters.asScalaBuffer(['2']).toSeq()
      }
    }
    request.uri() >> '/path?a=1&b=2'

    when:
    def pactRequest = Conversions.unfilteredRequestToPactRequest(request)

    then:
    pactRequest.path == '/path'
    pactRequest.query == [a: ['1'], b: ['2']]
  }

  def 'converting an unfiltered request to a pact request - construct the pact request correctly - with no query string'() {
    given:
    request.parameterNames() >> JavaConverters.asScalaIterator([].iterator())
    request.uri() >> '/path'

    when:
    def pactRequest = Conversions.unfilteredRequestToPactRequest(request)

    then:
    pactRequest.path == '/path'
    pactRequest.query.isEmpty()
  }

  def 'converting an unfiltered request to a pact request - construct the pact request correctly - with a path ending with a question mark'() {
    given:
    request.parameterNames() >> JavaConverters.asScalaIterator([].iterator())
    request.uri() >> '/path?'

    when:
    def pactRequest = Conversions.unfilteredRequestToPactRequest(request)

    then:
    pactRequest.path == '/path'
    pactRequest.query.isEmpty()
  }

  def 'converting an unfiltered request to a pact request - construct the pact request correctly - with a path with strings in it'() {
    given:
    request.parameterNames() >> JavaConverters.asScalaIterator([].iterator())
    request.uri() >> '/some+path'

    when:
    def pactRequest = Conversions.unfilteredRequestToPactRequest(request)

    then:
    pactRequest.path == '/some+path'
    pactRequest.query.isEmpty()
  }

}
