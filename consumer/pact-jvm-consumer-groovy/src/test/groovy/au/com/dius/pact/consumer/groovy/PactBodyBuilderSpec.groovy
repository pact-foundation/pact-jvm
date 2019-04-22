package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification

import static au.com.dius.pact.core.model.generators.Category.BODY
import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.INTEGER
import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.NUMBER

class PactBodyBuilderSpec extends Specification {

  PactBuilder service

  def setup() {
    service = new PactBuilder()
    service {
      serviceConsumer 'Consumer'
      hasPactWith 'Provider'
    }
  }

  @SuppressWarnings(['AbcMetric', 'MethodSize'])
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
    def keys = new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString()).keySet()
    def requestMatchingRules = service.interactions[0].request.matchingRules
    def bodyMatchingRules = requestMatchingRules.rulesForCategory('body').matchingRules
    def responseMatchingRules = service.interactions[0].response.matchingRules
    def requestGenerators = service.interactions[0].request.generators.categories[BODY]

    then:
    service.interactions.size() == 1
    requestMatchingRules.categories == ['body'] as Set
    bodyMatchingRules['$.name'].rules == [new RegexMatcher('\\w+', 'harry')]
    bodyMatchingRules['$.surname'].rules == [new RegexMatcher('\\w+', 'larry')]
    bodyMatchingRules['$.position'].rules == [new RegexMatcher('staff|contractor', 'staff')]
    bodyMatchingRules['$.hexCode'].rules == [new RegexMatcher('[0-9a-fA-F]+')]
    bodyMatchingRules['$.hexCode2'].rules == [new RegexMatcher('[0-9a-fA-F]+')]
    bodyMatchingRules['$.id'].rules == [new NumberTypeMatcher(INTEGER)]
    bodyMatchingRules['$.id2'].rules == [new NumberTypeMatcher(INTEGER)]
    bodyMatchingRules['$.salary'].rules == [new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)]
    bodyMatchingRules['$.localAddress'].rules == [new RegexMatcher('(\\d{1,3}\\.)+\\d{1,3}', '127.0.0.1')]
    bodyMatchingRules['$.localAddress2'].rules == [new RegexMatcher('(\\d{1,3}\\.)+\\d{1,3}', '127.0.0.1')]
    bodyMatchingRules['$.age2'].rules == [new NumberTypeMatcher(INTEGER)]
    bodyMatchingRules['$.ts'].rules == [new TimestampMatcher('yyyy-MM-dd\'T\'HH:mm:ss')]
    bodyMatchingRules['$.timestamp'].rules == [new TimestampMatcher('yyyy/MM/dd - HH:mm:ss.S')]
    bodyMatchingRules['$.values[3]'].rules == [new NumberTypeMatcher(NUMBER)]
    bodyMatchingRules['$.role.dob'].rules == [new DateMatcher('MM/dd/yyyy')]
    bodyMatchingRules['$.role.id'].rules == [
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')]
    bodyMatchingRules['$.roles[0].id'].rules == [
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')]
    responseMatchingRules.categories == ['body'] as Set
    responseMatchingRules.rulesForCategory('body').matchingRules == [
      '$.name': new MatchingRuleGroup([new RegexMatcher('\\w+', 'harry')])]

    keys == ['name', 'surname', 'position', 'happy', 'hexCode', 'hexCode2', 'id', 'id2', 'localAddress',
      'localAddress2', 'age', 'age2', 'salary', 'timestamp', 'ts', 'values', 'role', 'roles'] as Set

    service.interactions[0].response.body.valueAsString() == new JsonBuilder([name: 'harry']).toPrettyString()

    requestGenerators.keySet() == ['$.hexCode', '$.id', '$.age2', '$.salary', '$.ts', '$.timestamp', '$.values[3]',
                                   '$.role.id', '$.role.dob', '$.roles[0].id'] as Set
    requestGenerators['$.hexCode'].class == RandomHexadecimalGenerator
    requestGenerators['$.id'].class == RandomIntGenerator
    requestGenerators['$.age2'].class == RandomIntGenerator
    requestGenerators['$.salary'].class == RandomDecimalGenerator
    requestGenerators['$.values[3]'].class == RandomDecimalGenerator
    requestGenerators['$.role.id'].class == UuidGenerator
    requestGenerators['$.role.dob'].class == DateGenerator
    requestGenerators['$.roles[0].id'].class == UuidGenerator
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
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString()))
    def rules = service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules

    then:
    service.interactions.size() == 1
    rules['$.orders'] == new MatchingRuleGroup([new MaxTypeMatcher(10)])
    rules['$.orders[*].id'] == new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)])
    rules['$.orders[*].lineItems'] == new MatchingRuleGroup([new MinTypeMatcher(1)])
    rules['$.orders[*].lineItems[*].id'] == new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)])
    rules['$.orders[*].lineItems[*].amount'] == new MatchingRuleGroup([new NumberTypeMatcher(NUMBER)])
    rules['$.orders[*].lineItems[*].productCodes'] == new MatchingRuleGroup([TypeMatcher.INSTANCE])
    rules['$.orders[*].lineItems[*].productCodes[*].code'] == new MatchingRuleGroup([TypeMatcher.INSTANCE])

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
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString())

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.orders': new MatchingRuleGroup([new MaxTypeMatcher(10)]),
      '$.orders[*].id': new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)]),
      '$.orders[*].lineItems': new MatchingRuleGroup([new MinTypeMatcher(1)]),
      '$.orders[*].lineItems[*].id': new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)]),
      '$.orders[*].lineItems[*].amount': new MatchingRuleGroup([new NumberTypeMatcher(NUMBER)]),
      '$.orders[*].lineItems[*].productCodes': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.orders[*].lineItems[*].productCodes[*].code': new MatchingRuleGroup([TypeMatcher.INSTANCE])
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
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString())

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.permissions': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.permissions2': new MatchingRuleGroup([new MinTypeMatcher(2)]),
      '$.permissions3': new MatchingRuleGroup([new MaxTypeMatcher(4)])
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
    def body = new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString())

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      '$.permissions': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.permissions[*]': new MatchingRuleGroup([new RegexMatcher('\\w+')]),
      '$.permissions2': new MatchingRuleGroup([new MinTypeMatcher(2)]),
      '$.permissions2[*]': new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)]),
      '$.permissions3': new MatchingRuleGroup([new MaxTypeMatcher(4)]),
      '$.permissions3[*]': new MatchingRuleGroup([new RegexMatcher('\\d+')])
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
    request.body.valueAsString() == '''|{
                       |    "name": "harry",
                       |    "surname": "larry",
                       |    "position": "staff",
                       |    "happy": true
                       |}'''.stripMargin()
    response.body.valueAsString() == '''|{
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
    request.body.valueAsString() == '''|{
                       |    "name": "harry",
                       |    "surname": "larry",
                       |    "position": "staff",
                       |    "happy": true
                       |}'''.stripMargin()
    response.body.valueAsString() == '''|{
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
    request.body.valueAsString() == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.valueAsString() == '{"name":"harry"}'
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
    request.body.valueAsString() == '{"name":"harry","surname":"larry","position":"staff","happy":true}'
    response.body.valueAsString() == '{"name":"harry"}'
  }

  def 'No Special Handling For Field Names Formerly Not Conforming Gatling Fields'() {
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
    def keys = walkGraph(new JsonSlurper().parseText(service.interactions[0].request.body.valueAsString()))

    then:
    service.interactions.size() == 1
    service.interactions[0].request.matchingRules.rulesForCategory('body').matchingRules == [
      $/$.2/$: new MatchingRuleGroup([new MaxTypeMatcher(10)]),
      $/$.2[*].id/$: new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)]),
      $/$.2[*].lineItems/$: new MatchingRuleGroup([new MinTypeMatcher(1)]),
      $/$.2[*].lineItems[*].id/$: new MatchingRuleGroup([new NumberTypeMatcher(INTEGER)]),
      $/$.2[*].lineItems[*].10k-depreciation-bips/$: new MatchingRuleGroup([
        new NumberTypeMatcher(INTEGER)]),
      $/$.2[*].lineItems[*].productCodes/$: new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      $/$.2[*].lineItems[*].productCodes[*].code/$: new MatchingRuleGroup([TypeMatcher.INSTANCE])
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

  def 'supports matching arrays of arrays at root level'() {
    given:
    service {
      uponReceiving('a request with arrays of arrays at the root')
      withAttributes(method: 'get', path: '/')
      withBody PactBodyBuilder.eachLike([ regexp('[0-9a-f]{8}', 'e8cda07e'), regexp(~/\w+/, 'sony') ]),
        prettyPrint: false
      willRespondWith(status: 200)
      withBody PactBodyBuilder.eachLike(regexp('\\w+', 'test')), prettyPrint: false
    }
    def expectedMatchingRules = [
      '$': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$[0]': new MatchingRuleGroup([new RegexMatcher('[0-9a-f]{8}', 'e8cda07e')]),
      '$[1]': new MatchingRuleGroup([new RegexMatcher('\\w+', 'sony')]),
    ]

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def response = service.interactions.first().response

    then:
    request.body.valueAsString() == '[["e8cda07e","sony"]]'
    request.matchingRules.rulesForCategory('body').matchingRules == expectedMatchingRules

    response.body.valueAsString() == '["test"]'
    response.matchingRules.rulesForCategory('body').matchingRules == [
      '$': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$[*]': new MatchingRuleGroup([new RegexMatcher('\\w+', 'test')])
    ]
  }

  @SuppressWarnings('AbcMetric')
  def 'supports matching arrays of arrays'() {
    given:
    service {
      uponReceiving('a request with arrays of arrays')
      withAttributes(method: 'get', path: '/')
      withBody(prettyPrint: false) {
        answers minLike(1) {
          questionId string('books')
          answer eachLike([
            {
              questionId string('title')
              answer string('BBBB')
            },
            {
              questionId string('author')
              answer string('B.B.')
            }
          ])
          answer2 eachLike(2,
            eachLike(2) {
              questionId string('title')
              answer string('BBBB')
            }
          )
          answer3 eachArrayLike {
            questionId string('title')
            answer string('BBBB')
          }
        }
      }
      willRespondWith(status: 200)
    }
    def expectedMatchingRules = [
      '$.answers': new MatchingRuleGroup([new MinTypeMatcher(1)]),
      '$.answers[*].questionId': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer[0].questionId': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer[0].answer': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer[1].questionId': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer[1].answer': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer2': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer2[*]': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer2[*][*].questionId': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer2[*][*].answer': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer3': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer3[*]': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer3[*][*].questionId': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.answers[*].answer3[*][*].answer': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    when:
    service.buildInteractions()
    def request = service.interactions.first().request

    then:
    request.body.valueAsString() ==
      '{"answers":[{"questionId":"books","answer":[[{"questionId":"title","answer":"BBBB"},' +
      '{"questionId":"author","answer":"B.B."}]],"answer2":[[{"questionId":"title","answer":"BBBB"},' +
      '{"questionId":"title","answer":"BBBB"}],[{"questionId":"title","answer":"BBBB"},' +
      '{"questionId":"title","answer":"BBBB"}]],"answer3":[[{"questionId":"title","answer":"BBBB"}]]}]}'
    request.matchingRules.rulesForCategory('body').matchingRules == expectedMatchingRules
  }

  def 'array with min and max'() {
    given:
    service {
      uponReceiving('a request with an array with min and max')
      withAttributes(method: 'get', path: '/')
      withBody {
        items minMaxLike(1, 10) {
          id string('100abc')
        }
      }
      willRespondWith(status: 200)
    }
    def expectedMatchingRules = [
      '$.items': new MatchingRuleGroup([new MinMaxTypeMatcher(1, 10)]),
      '$.items[*].id': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    when:
    service.buildInteractions()
    def request = service.interactions.first().request

    then:
    request.body.valueAsString() == '''|{
                             |    "items": [
                             |        {
                             |            "id": "100abc"
                             |        }
                             |    ]
                             |}'''.stripMargin()
    request.matchingRules.rulesForCategory('body').matchingRules == expectedMatchingRules
  }

  def 'root level array with multiple examples'() {
    given:
    service {
      uponReceiving('a request with a root level array with multiple examples')
      withAttributes(method: 'get', path: '/')
      withBody(eachLike(3) {
        id identifier
        state('COMPLETED')
        type regexp('(A|B)', 'A')
      })
      willRespondWith(status: 200)
    }

    when:
    service.buildInteractions()
    def request = service.interactions.first().request
    def body = new JsonSlurper().parseText(request.body.valueAsString())

    then:
    body instanceof List
    body.size() == 3
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
