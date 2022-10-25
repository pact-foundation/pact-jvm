package au.com.dius.pact.core.model

import spock.lang.Issue
import spock.lang.Specification

class HttpResponseSpec extends Specification {
  @Issue('#1611')
  def 'supports empty bodies'() {
    expect:
    new HttpResponse(200, [:], OptionalBody.empty()).toMap() == [status: 200, body: [content: '']]
  }
}
