package au.com.dius.pact.core.model

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class HttpRequestSpec extends Specification {
  @Issue('#1611')
  def 'supports empty bodies'() {
    expect:
    new HttpRequest('GET', '/', [:], [:], OptionalBody.empty()).toMap() ==
      [method: 'GET', path: '/', body: [content: '']]
  }

  def 'allows configuring the interaction from properties'() {
    given:
    def interaction = new HttpRequest()

    when:
    interaction.updateProperties([
      'method': 'PUT',
      'path': '/reports/report002.csv',
      'query': [a: 'b'],
      'headers': ['x-a': 'b']
    ])

    then:
    interaction.method == 'PUT'
    interaction.path == '/reports/report002.csv'
    interaction.headers == ['x-a': ['b']]
    interaction.query == [a: ['b']]
  }

  @Unroll
  def 'supports setting up the query parameters'() {
    given:
    def interaction = new HttpRequest()

    when:
    interaction.updateProperties([query: queryValue])

    then:
    interaction.query == query

    where:

    queryValue | query
    [a: ['b']] | [a: ['b']]
    [a: 'b']   | [a: ['b']]
    'a=b'      | [a: ['b']]
  }

  @Unroll
  def 'supports setting up the headers'() {
    given:
    def interaction = new HttpRequest()

    when:
    interaction.updateProperties([headers: headerValue])

    then:
    interaction.headers == headers

    where:

    headerValue | headers
    [a: ['b']] | [a: ['b']]
    [a: 'b']   | [a: ['b']]
  }
}
