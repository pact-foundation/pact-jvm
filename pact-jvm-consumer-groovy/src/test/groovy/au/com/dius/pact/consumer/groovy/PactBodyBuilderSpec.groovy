package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import scala.Option
import scala.collection.JavaConverters
import spock.lang.Specification

class PactBodyBuilderSpec extends Specification {

  def service

  def setup() {
    service = new PactBuilder()
    service {
      serviceConsumer 'Consumer'
      hasPactWith 'Provider'
    }
  }

  @SuppressWarnings('AbcMetric')
  void dsl() {
    given:
    service {
        uponReceiving('a request')
        withAttributes(method: 'get', path: '/')
        withBody {
          name(~/\w+/, 'harry')
          surname regexp(~/\w+/, 'larry')
          position regexp(~/staff|contractor/, 'staff')
          happy(true)

          hexCode(hexValue)
          hexCode2 hexValue('01234AB')
          id(identifier)
          id2 identifier('1234567890')
          localAddress(ipAddress)
          localAddress2 ipAddress('192.169.0.2')
          age(100)
          age2(integer)
          salary decimal

          ts(timestamp)
          timestamp = timestamp('yyyy/MM/dd - HH:mm:ss.S')

          values([1, 2, 3, numeric])

          role {
            name('admin')
            id(uuid)
            kind {
              id(100)
            }
            dob date('MM/dd/yyyy')
          }

          roles([
            {
              name('dev')
              id(uuid)
            }
          ])
        }
        willRespondWith(
            status: 200,
            headers: ['Content-Type': 'text/html']
        )
        withBody {
          name(~/\w+/, 'harry')
        }
    }

    when:
    service.fragment()
    def keys = new JsonSlurper().parseText(service.interactions[0].request.body.value).keySet()

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
      '$.body.name': [match: 'regex', regex: '\\w+'],
      '$.body.surname': [match: 'regex', 'regex': '\\w+'],
      '$.body.position': [match: 'regex', 'regex': 'staff|contractor'],
      '$.body.hexCode': [match: 'regex', regex: '[0-9a-fA-F]+'],
      '$.body.hexCode2': [match: 'regex', regex: '[0-9a-fA-F]+'],
      '$.body.id': [match: 'type'],
      '$.body.id2': [match: 'type'],
      '$.body.salary': [match: 'decimal'],
      '$.body.localAddress': [match: 'regex', regex: '(\\d{1,3}\\.)+\\d{1,3}'],
      '$.body.localAddress2': [match: 'regex', regex: '(\\d{1,3}\\.)+\\d{1,3}'],
      '$.body.age2': [match: 'integer'],
      '$.body.ts': [match: 'timestamp', timestamp: 'yyyy-MM-dd\'T\'HH:mm:ss'],
      '$.body.timestamp': [match: 'timestamp', timestamp: 'yyyy/MM/dd - HH:mm:ss.S'],
      '$.body.values[3]': [match: 'number'],
      '$.body.role.dob': [match: 'date', date: 'MM/dd/yyyy'],
      '$.body.role.id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'],
      '$.body.roles[0].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']
    ]
    asJavaMap(service.interactions[0].response.matchingRules) == ['$.body.name': [match: 'regex', regex: '\\w+']]

    keys == ['name', 'surname', 'position', 'happy', 'hexCode', 'hexCode2', 'id', 'id2', 'localAddress',
      'localAddress2', 'age', 'age2', 'salary', 'timestamp', 'ts', 'values', 'role', 'roles'] as Set

    service.interactions[0].response.body.value == new JsonBuilder([name: 'harry']).toPrettyString()
  }

  def 'arrays with matching'() {
    given:
    service {
        uponReceiving('a request with array matching')
        withAttributes(method: 'get', path: '/')
        withBody {
            orders maxLike(10) {
                id identifier
                lineItems minLike(1) {
                    id identifier
                    amount numeric
                    productCodes eachLike { code string('A100') }
                }
            }
        }
        willRespondWith(
            status: 200,
            headers: ['Content-Type': 'text/html']
        )
    }

    when:
    service.fragment()
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.value))

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
        '$.body.orders': [max: 10, 'match': 'type'],
        '$.body.orders[*].id': ['match': 'type'],
        '$.body.orders[*].lineItems': ['min': 1, 'match': 'type'],
        '$.body.orders[*].lineItems[*].id': [match: 'type'],
        '$.body.orders[*].lineItems[*].amount': [match: 'number'],
        '$.body.orders[*].lineItems[*].productCodes': ['match': 'type'],
        '$.body.orders[*].lineItems[*].productCodes[*].code': [match: 'type']
    ]

//    keys == [
//        'orders', [0, [
//                'id', [], 'lineItems', [0, [
//                    'amount', [], 'id', [], 'productCodes', [0, [
//                        'code', []
//                    ]]
//                ]]
//        ]]
//    ]
  }

  def 'arrays with matching with extra examples'() {
    given:
    service {
      uponReceiving('a request with array matching with extra examples')
      withAttributes(method: 'get', path: '/')
      withBody {
        orders maxLike(10, 2) {
          id identifier
          lineItems minLike(1, 3) {
            id identifier
            amount numeric
            productCodes eachLike(4) { code string('A100') }
          }
        }
      }
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html']
      )
    }

    when:
    service.fragment()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
      '$.body.orders': [max: 10, 'match': 'type'],
      '$.body.orders[*].id': ['match': 'type'],
      '$.body.orders[*].lineItems': ['min': 1, 'match': 'type'],
      '$.body.orders[*].lineItems[*].id': [match: 'type'],
      '$.body.orders[*].lineItems[*].amount': [match: 'number'],
      '$.body.orders[*].lineItems[*].productCodes': ['match': 'type'],
      '$.body.orders[*].lineItems[*].productCodes[*].code': [match: 'type']
    ]
    body.orders.size == 2
    body.orders.every { it.keySet() == ['id', 'lineItems'] as Set }
    body.orders.first().lineItems.size == 3
    body.orders.first().lineItems.every { it.keySet() == ['id', 'amount', 'productCodes'] as Set }
    body.orders.first().lineItems.first().productCodes.size == 4
    body.orders.first().lineItems.first().productCodes.every { it.keySet() == ['code'] as Set }
  }

  def 'arrays of primitives with extra examples'() {
    given:
    service {
      uponReceiving('a request with array matching with primitives')
      withAttributes(method: 'get', path: '/')
      withBody {
        permissions eachLike(3, 'GRANT')
        permissions2 minLike(2, 3, 100)
        permissions3 maxLike(4, 3, 'GRANT')
      }
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html']
      )
    }

    when:
    service.fragment()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
      '$.body.permissions': [match: 'type'],
      '$.body.permissions2': [match: 'type', min: 2],
      '$.body.permissions3': [match: 'type', max: 4]
    ]
    body.permissions == ['GRANT'] * 3
    body.permissions2 == [100] * 3
    body.permissions3 == ['GRANT'] * 3
  }

  def 'arrays of primitives with extra examples and matchers'() {
    given:
    service {
      uponReceiving('a request with array matching with primitives and matchers')
      withAttributes(method: 'get', path: '/')
      withBody {
        permissions eachLike(3, regexp(~/\w+/))
        permissions2 minLike(2, 3, integer())
        permissions3 maxLike(4, 3, ~/\d+/)
      }
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html']
      )
    }

    when:
    service.fragment()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
      '$.body.permissions': [match: 'type'],
      '$.body.permissions[*]': [match: 'regex', regex: '\\w+'],
      '$.body.permissions2': [match: 'type', min: 2],
      '$.body.permissions2[*]': [match: 'integer'],
      '$.body.permissions3': [match: 'type', max: 4],
      '$.body.permissions3[*]': [match: 'regex', regex: '\\d+']
    ]
    body.permissions.size == 3
    body.permissions2.size == 3
    body.permissions3.size == 3
  }

  def 'pretty prints bodies by default'() {
    given:
    service {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/')
      withBody {
        name(~/\w+/, 'harry')
        surname regexp(~/\w+/, 'larry')
        position regexp(~/staff|contractor/, 'staff')
        happy(true)
      }
      willRespondWith(status: 200)
      withBody {
        name(~/\w+/, 'harry')
      }
    }

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def response = service.interactions.first().response

    then:
    request.body.value == '''|{
                       |    "name": "harry",
                       |    "surname": "larry",
                       |    "position": "staff",
                       |    "happy": true
                       |}'''.stripMargin()
    response.body.value == '''|{
                        |    "name": "harry"
                        |}'''.stripMargin()
  }

  def 'pretty prints bodies if pretty print is set to true'() {
    given:
    service {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/')
      withBody(prettyPrint: true) {
        name(~/\w+/, 'harry')
        surname regexp(~/\w+/, 'larry')
        position regexp(~/staff|contractor/, 'staff')
        happy(true)
      }
      willRespondWith(status: 200)
      withBody(prettyPrint: true) {
        name(~/\w+/, 'harry')
      }
    }

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def response = service.interactions.first().response

    then:
    request.body.value == '''|{
                       |    "name": "harry",
                       |    "surname": "larry",
                       |    "position": "staff",
                       |    "happy": true
                       |}'''.stripMargin()
    response.body.value == '''|{
                        |    "name": "harry"
                        |}'''.stripMargin()
  }

  def 'does not pretty print bodies if pretty print is set to false'() {
    given:
    service {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/')
      withBody(prettyPrint: false) {
        name(~/\w+/, 'harry')
        surname regexp(~/\w+/, 'larry')
        position regexp(~/staff|contractor/, 'staff')
        happy(true)
      }
      willRespondWith(status: 200)
      withBody(prettyPrint: false) {
        name(~/\w+/, 'harry')
      }
    }

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def response = service.interactions.first().response

    then:
    request.body.value == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.value == '{"name":"harry"}'
  }

  def 'does not pretty print bodies if mimetype corresponds to one that requires compact bodies'() {
    given:
    service {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/')
      withBody(mimeType: 'application/x-thrift+json') {
        name(~/\w+/, 'harry')
        surname regexp(~/\w+/, 'larry')
        position regexp(~/staff|contractor/, 'staff')
        happy(true)
      }
      willRespondWith(status: 200)
      withBody(mimeType: 'application/x-thrift+json') {
        name(~/\w+/, 'harry')
      }
    }

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def response = service.interactions.first().response

    then:
    request.body.value == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.value == '{"name":"harry"}'
  }

  def 'Guard Against Field Names That Don\'t Conform To Gatling Fields'() {
    given:
    service {
      uponReceiving('a request with invalid gatling fields')
      withAttributes(method: 'get', path: '/')
      withBody {
        '2' maxLike(10) {
          id identifier
          lineItems minLike(1) {
            id identifier
            '10k-depreciation-bips' integer(-2090)
            productCodes eachLike { code string('A100') }
          }
        }
      }
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html']
      )
    }

    when:
    service.fragment()
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.value))

    then:
    service.interactions.size() == 1
    asJavaMap(service.interactions[0].request.matchingRules) == [
      $/$.body['2']/$: [max: 10, 'match': 'type'],
      $/$.body['2'][*].id/$: ['match': 'type'],
      $/$.body['2'][*].lineItems/$: ['min': 1, 'match': 'type'],
      $/$.body['2'][*].lineItems[*].id/$: [match: 'type'],
      $/$.body['2'][*].lineItems[*]['10k-depreciation-bips']/$: [match: 'integer'],
      $/$.body['2'][*].lineItems[*].productCodes/$: ['match': 'type'],
      $/$.body['2'][*].lineItems[*].productCodes[*].code/$: [match: 'type']
    ]

//    keys == [
//      '2', [0, [
//        'id', [], 'lineItems', [0, [
//          '10k-depreciation-bips', [], 'id', [], 'productCodes', [0, [
//            'code', []
//          ]]
//        ]]
//      ]]
//    ]
  }

  private List walkGraph(def value) {
      def set = []
      if (value instanceof Map) {
          value.keySet().each { k ->
              set << k
              set << walkGraph(value[k])
          }
      } else if (value instanceof List) {
          value.eachWithIndex { v, i ->
              set << i
              set << walkGraph(v)
          }
      }
      set
  }

  private asJavaMap(def map) {
      if (map instanceof Option) {
        if (map.defined) {
          asJavaMap(map.get())
        } else {
          [:]
        }
      } else if (map instanceof scala.collection.Map) {
        JavaConverters.mapAsJavaMapConverter(map).asJava().collectEntries {
          [it.key, asJavaMap(it.value)]
        }
      } else {
        map
      }
  }
}
