package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.TimestampMatcher
import au.com.dius.pact.model.matchingrules.TypeMatcher
import au.com.dius.pact.model.matchingrules.DateMatcher
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactBodyBuilderSpec extends Specification {

  PactBuilder service

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
    service.buildInteractions()
    def keys = new JsonSlurper().parseText(service.interactions[0].request.body.value).keySet()
    def requestMatchingRules = service.interactions[0].request.matchingRules
    def bodyMatchingRules = requestMatchingRules.rulesForCategory('body').matchingRules
    def responseMatchingRules = service.interactions[0].response.matchingRules

    then:
    service.interactions.size() == 1
    requestMatchingRules.categories == ['body'] as Set
    bodyMatchingRules['$.name'] == [new RegexMatcher('\\w+')]
    bodyMatchingRules['$.surname'] == [new RegexMatcher('\\w+')]
    bodyMatchingRules['$.position'] == [new RegexMatcher('staff|contractor')]
    bodyMatchingRules['$.hexCode'] == [new RegexMatcher('[0-9a-fA-F]+')]
    bodyMatchingRules['$.hexCode2'] == [new RegexMatcher('[0-9a-fA-F]+')]
    bodyMatchingRules['$.id'] == [new TypeMatcher()]
    bodyMatchingRules['$.id2'] == [new TypeMatcher()]
    bodyMatchingRules['$.salary'] == [new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)]
    bodyMatchingRules['$.localAddress'] == [new RegexMatcher('(\\d{1,3}\\.)+\\d{1,3}')]
    bodyMatchingRules['$.localAddress2'] == [new RegexMatcher('(\\d{1,3}\\.)+\\d{1,3}')]
    bodyMatchingRules['$.age2'] == [new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)]
    bodyMatchingRules['$.ts'] == [new TimestampMatcher('yyyy-MM-dd\'T\'HH:mm:ss')]
    bodyMatchingRules['$.timestamp'] == [new TimestampMatcher('yyyy/MM/dd - HH:mm:ss.S')]
    bodyMatchingRules['$.values[3]'] == [new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)]
    bodyMatchingRules['$.role.dob'] == [new DateMatcher('MM/dd/yyyy')]
    bodyMatchingRules['$.role.id'] == [
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')]
    bodyMatchingRules['$.roles[0].id'] == [
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')]
    responseMatchingRules.categories == ['body'] as Set
    responseMatchingRules.rulesForCategory('body').matchingRules == ['$.name': [new RegexMatcher('\\w+')]]

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
    service.buildInteractions()
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.value))

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
        '$.orders': [new MaxTypeMatcher(10)],
        '$.orders[*].id': [new TypeMatcher()],
        '$.orders[*].lineItems': [new MinTypeMatcher(1)],
        '$.orders[*].lineItems[*].id': [new TypeMatcher()],
        '$.orders[*].lineItems[*].amount': [new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)],
        '$.orders[*].lineItems[*].productCodes': [new TypeMatcher()],
        '$.orders[*].lineItems[*].productCodes[*].code': [new TypeMatcher()]
    ]

    keys == [
        'orders', [0, [
                'id', [], 'lineItems', [0, [
                    'amount', [], 'id', [], 'productCodes', [0, [
                        'code', []
                    ]]
                ]]
        ]]
    ]
  }

  @SuppressWarnings('AbcMetric')
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
    service.buildInteractions()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.orders': [new MaxTypeMatcher(10)],
      '$.orders[*].id': [new TypeMatcher()],
      '$.orders[*].lineItems': [new MinTypeMatcher(1)],
      '$.orders[*].lineItems[*].id': [new TypeMatcher()],
      '$.orders[*].lineItems[*].amount': [new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)],
      '$.orders[*].lineItems[*].productCodes': [new TypeMatcher()],
      '$.orders[*].lineItems[*].productCodes[*].code': [new TypeMatcher()]
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
    service.buildInteractions()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.permissions': [new TypeMatcher()],
      '$.permissions2': [new MinTypeMatcher(2)],
      '$.permissions3': [new MaxTypeMatcher(4)]
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
    service.buildInteractions()
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.value)

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.permissions': [new TypeMatcher()],
      '$.permissions[*]': [new RegexMatcher('\\w+')],
      '$.permissions2': [new MinTypeMatcher(2)],
      '$.permissions2[*]': [new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)],
      '$.permissions3': [new MaxTypeMatcher(4)],
      '$.permissions3[*]': [new RegexMatcher('\\d+')]
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
    service.buildInteractions()
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.value))

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      $/$['2']/$: [new MaxTypeMatcher(10)],
      $/$['2'][*].id/$: [new TypeMatcher()],
      $/$['2'][*].lineItems/$: [new MinTypeMatcher(1)],
      $/$['2'][*].lineItems[*].id/$: [new TypeMatcher()],
      $/$['2'][*].lineItems[*]['10k-depreciation-bips']/$: [
        new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)],
      $/$['2'][*].lineItems[*].productCodes/$: [new TypeMatcher()],
      $/$['2'][*].lineItems[*].productCodes[*].code/$: [new TypeMatcher()]
    ]

    keys == [
      '2', [0, [
        'id', [], 'lineItems', [0, [
            '10k-depreciation-bips', [], 'id', [], 'productCodes', [0, [
            'code', []
          ]]
        ]]
      ]]
    ]
  }

  private List walkGraph(def value) {
      def set = []
      if (value instanceof Map) {
          value.keySet().sort().each { k ->
              set << k
              set << walkGraph(value[k])
          }
      } else if (value instanceof List) {
          value.sort().eachWithIndex { v, i ->
              set << i
              set << walkGraph(v)
          }
      }
      set
  }

}
