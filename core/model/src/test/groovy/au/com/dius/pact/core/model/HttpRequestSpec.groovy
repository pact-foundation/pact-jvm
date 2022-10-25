package au.com.dius.pact.core.model

import spock.lang.Issue
import spock.lang.Specification

class HttpRequestSpec extends Specification {
  @Issue('#1611')
  def 'supports empty bodies'() {
    expect:
    new HttpRequest('GET', '/', [:], [:], OptionalBody.empty()).toMap() ==
      [method: 'GET', path: '/', body: [content: '']]
  }
}
