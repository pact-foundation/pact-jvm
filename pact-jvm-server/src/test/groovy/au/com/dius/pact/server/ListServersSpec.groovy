package au.com.dius.pact.server

import spock.lang.Specification
import static scala.collection.JavaConverters.mapAsScalaMap

class ListServersSpec extends Specification {
  def 'empty state'() {
    given:
    def state = new ServerState([:])

    when:
    def result = ListServers.apply(state)

    then:
    result.response.status == 200
    result.response.body.valueAsString() == '{"ports": [], "paths": []}'
  }

  def 'with single Mock server'() {
    given:
    def state = new ServerState([
      '1234': Mock(StatefulMockProvider)
    ])

    when:
    def result = ListServers.apply(state)

    then:
    result.response.status == 200
    result.response.body.valueAsString() == '{"ports": [1234], "paths": []}'
  }

  def 'with single Mock server with a path'() {
    given:
    def state = new ServerState([
      '/path': Mock(StatefulMockProvider)
    ])

    when:
    def result = ListServers.apply(state)

    then:
    result.response.status == 200
    result.response.body.valueAsString() == '{"ports": [], "paths": ["/path"]}'
  }

  def 'with multiple Mock servers'() {
    given:
    def state = new ServerState([
      '1234': Mock(StatefulMockProvider),
      '/path': Mock(StatefulMockProvider),
      '8765': Mock(StatefulMockProvider),
      '/other-path': Mock(StatefulMockProvider)
    ])

    when:
    def result = ListServers.apply(state)

    then:
    result.response.status == 200
    result.response.body.valueAsString() == '{"ports": [8765, 1234], "paths": ["/other-path", "/path"]}'
  }
}
