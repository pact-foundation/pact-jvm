package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

class HttpResponseSpec extends Specification {
  def 'allows configuring the interaction from properties'() {
    given:
    def interaction = new HttpResponse()

    when:
    interaction.updateProperties([
      'status': '201',
      'headers': ['x-a': 'b']
    ])

    then:
    interaction.status == 201
    interaction.headers == ['x-a': ['b']]
  }

  @Unroll
  def 'supports setting up the status'() {
    given:
    def interaction = new HttpResponse()

    when:
    interaction.updateProperties([status: statusValue])

    then:
    interaction.status == status

    where:

    statusValue | status
    204         | 204
    '203'       | 203
  }

  @Unroll
  def 'supports setting up the headers'() {
    given:
    def interaction = new HttpResponse()

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
