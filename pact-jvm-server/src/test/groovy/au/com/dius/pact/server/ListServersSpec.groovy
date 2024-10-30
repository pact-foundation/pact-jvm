package au.com.dius.pact.server

import spock.lang.Specification
import static scala.collection.JavaConverters.mapAsScalaMap

class ListServersSpec extends Specification {
  def 'empty state'() {
    given:
    def state = [:]

    when:
    def result = ListServers.apply(mapAsScalaMap(state).toMap())

    then:
    result.response().status == 200
    result.response().body.valueAsString() == '{"ports": [], "paths": []}'
  }

  def 'with single Mock server'() {
    given:
    def state = [
      '1234': Mock(StatefulMockProvider)
    ]

    when:
    def result = ListServers.apply(mapAsScalaMap(state).toMap())

    then:
    result.response().status == 200
    result.response().body.valueAsString() == '{"ports": [1234], "paths": []}'
  }

  def 'with single Mock server with a path'() {
    given:
    def state = [
      '/path': Mock(StatefulMockProvider)
    ]

    when:
    def result = ListServers.apply(mapAsScalaMap(state).toMap())

    then:
    result.response().status == 200
    result.response().body.valueAsString() == '{"ports": [], "paths": ["/path"]}'
  }

  def 'with multiple Mock servers'() {
    given:
    def state = [
      '1234': Mock(StatefulMockProvider),
      '/path': Mock(StatefulMockProvider),
      '8765': Mock(StatefulMockProvider),
      '/other-path': Mock(StatefulMockProvider)
    ]

    when:
    def result = ListServers.apply(mapAsScalaMap(state).toMap())

    then:
    result.response().status == 200
    result.response().body.valueAsString() == '{"ports": [1234, 8765], "paths": ["/path", "/other-path"]}'
  }
}
