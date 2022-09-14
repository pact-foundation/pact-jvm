package au.com.dius.pact.core.model

import spock.lang.Specification

class SynchronousHttpSpec extends Specification {
  def 'allows configuring the interaction from properties'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('', 'test')

    when:
    interaction.updateProperties([
      'request.method': 'PUT',
      'request.path': '/reports/report002.csv',
      'request.query': [a: 'b'],
      'request.headers': ['x-a': 'b'],
      'response.status': '205',
      'response.headers': ['x-b': ['b']]
    ])

    then:
    interaction.request.method == 'PUT'
    interaction.request.path == '/reports/report002.csv'
    interaction.request.headers == ['x-a': ['b']]
    interaction.request.query == [a: ['b']]
    interaction.response.status == 205
    interaction.response.headers == ['x-b': ['b']]
  }
}
