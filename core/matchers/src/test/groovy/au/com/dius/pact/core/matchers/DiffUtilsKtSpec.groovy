package au.com.dius.pact.core.matchers

import spock.lang.Specification

class DiffUtilsKtSpec extends Specification {
  def 'generates a diff of JSON'() {
    given:
    def expected = '''
      |[
      |  {
      |    "href": "http://localhost:9000/orders/1234",
      |    "method": "PUT",
      |    "name": "update"
      |  },
      |  {
      |    "href": "http://localhost:9000/orders/1234",
      |    "method": "DELETE",
      |    "name": "delete"
      |  }
      |]
    '''.stripMargin()
    def actual = '''
      |[
      |  {
      |    "href": "http://localhost:8080/orders/6961496522246184783",
      |    "method": "PUT",
      |    "name": "update"
      |  },
      |  {
      |    "href": "http://localhost:8080/orders/6961496522246184783/status",
      |    "method": "PUT",
      |    "name": "changeStatus"
      |  }
      |]
    '''.stripMargin()

    expect:
    DiffUtilsKt.generateDiff(expected, actual).join('\n').trim() == '''[
      |  {
      |-    "href": "http://localhost:9000/orders/1234",
      |+    "href": "http://localhost:8080/orders/6961496522246184783",
      |    "method": "PUT",
      |    "name": "update"
      |  },
      |  {
      |-    "href": "http://localhost:9000/orders/1234",
      |-    "method": "DELETE",
      |-    "name": "delete"
      |+    "href": "http://localhost:8080/orders/6961496522246184783/status",
      |+    "method": "PUT",
      |+    "name": "changeStatus"
      |  }
      |]'''.stripMargin()
  }
}
