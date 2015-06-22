package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.junit.Test
import scala.Option
import scala.collection.JavaConverters

class PactBodyBuilderTest {
    @Test
    void dsl() {
        def service = new PactBuilder()
        service {
            serviceConsumer "Consumer"
            hasPactWith "Provider"
        }
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

              ts(timestamp)
              timestamp = timestamp('yyyy/MM/dd - HH:mm:ss.S')

              values([1, 2, 3, numeric])

              role {
                name('admin')
                id(guid)
                kind {
                  id(100)
                }
                dob date('MM/dd/yyyy')
              }

              roles([
                {
                  name('dev')
                  id(guid)
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
        service.buildInteractions()
        assert service.interactions.size() == 1
        assert asJavaMap(service.interactions[0].request.matchingRules) == [
          '$.body.name': [regex: '\\w+'],
          '$.body.surname': ['regex': '\\w+'],
          '$.body.position': ['regex': 'staff|contractor'],
          '$.body.hexCode': [regex: '[0-9a-fA-F]+'],
          '$.body.hexCode2': [regex: '[0-9a-fA-F]+'],
          '$.body.id': [match: 'type'],
          '$.body.id2': [match: 'type'],
          '$.body.localAddress': [regex: '\\d{1,3}\\.)+\\d{1,3}'],
          '$.body.localAddress2': [regex: '\\d{1,3}\\.)+\\d{1,3}'],
          '$.body.age2': [match: 'integer'],
          '$.body.ts': [timestamp: 'yyyy-MM-dd\'T\'HH:mm:ss'],
          '$.body.timestamp': [timestamp: 'yyyy/MM/dd - HH:mm:ss.S'],
          '$.body.values[3]': [match: 'number'],
          '$.body.role.dob': [date: 'MM/dd/yyyy'],
          '$.body.role.id': [regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'],
          '$.body.roles[0].id': [regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']
        ]
        assert asJavaMap(service.interactions[0].response.matchingRules) == ['$.body.name': [regex: '\\w+']]

        def keys = new JsonSlurper().parseText(service.interactions[0].request.body.get()).keySet()
        assert keys == ['name', 'surname', 'position', 'happy', 'hexCode', 'hexCode2', 'id', 'id2', 'localAddress',
          'localAddress2', 'age', 'age2', 'timestamp', 'ts', 'values', 'role', 'roles'] as Set

        assert service.interactions[0].response.body.get() == new JsonBuilder([name: "harry"]).toPrettyString()
    }

    @Test
    void 'arrays with matching'() {
        def service = new PactBuilder()
        service {
            serviceConsumer "Consumer"
            hasPactWith "Provider"

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
        service.buildInteractions()
        assert service.interactions.size() == 1
        assert asJavaMap(service.interactions[0].request.matchingRules) == [
            '$.body.orders': [max: 10],
            '$.body.orders[*].id': ['match': 'type'],
            '$.body.orders[*].lineItems': ['min': 1],
            '$.body.orders[*].lineItems[*].id': [match: 'type'],
            '$.body.orders[*].lineItems[*].amount': [match: 'number'],
            '$.body.orders[*].lineItems[*].productCodes[*].code': [match: 'type']
        ]

        def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.get()))
        assert keys == [
            'orders', [0, [
                    'id', [], 'lineItems', [0, [
                        'amount', [], 'id', [], 'productCodes', [0, [
                            'code', []
                        ]]
                    ]]
            ]]
        ]

    }

    List walkGraph(def value) {
        def set = []
        if (value instanceof Map) {
            value.each { k, v ->
                set << k
                set << walkGraph(v)
            }
        } else if (value instanceof List) {
            value.eachWithIndex { v, i ->
                set << i
                set << walkGraph(v)
            }
        }
        set
    }

    def asJavaMap(def map) {
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
