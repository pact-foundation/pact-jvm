package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.HttpStatus
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.StatusCodeMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import kotlin.Pair
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.fromProviderState
import static au.com.dius.pact.consumer.dsl.Matchers.numeric
import static au.com.dius.pact.consumer.dsl.Matchers.regexp

class HttpResponseBuilderSpec extends Specification {

  HttpResponseBuilder builder

  def setup() {
    builder = new HttpResponseBuilder(new HttpResponse())
  }

  def 'with defaults'() {
    expect:
    builder.build() == new HttpResponse()
  }

  def 'allows adding headers to the response'() {
    when:
    def response = builder
      .header('A', 'B')
      .header('B', ['B', 'C', 'D'])
      .header('OPTIONS', 'GET, POST, HEAD')
      .headers([x: 'y', y: ['a', 'b', 'c']])
      .headers('x1', 'y', 'y1', 'a', 'y1', 'b', 'y1', 'c')
      .headers(new Pair('x2', 'y'), new Pair('y2', 'a'), new Pair('y2', 'b'), new Pair('y2', 'c'))
      .build()

    then:
    response.headers == [
      A: ['B'],
      B: ['B', 'C', 'D'],
      OPTIONS: ['GET', 'POST', 'HEAD'],
      x: ['y'],
      y: ['a', 'b', 'c'],
      x1: ['y'],
      y1: ['a', 'b', 'c'],
      x2: ['y'],
      y2: ['a', 'b', 'c']
    ]
  }

  def 'allows using matching rules with headers'() {
    when:
    def response = builder
      .header('A', regexp('\\d+', '111'))
      .header('B', ['B', numeric(100), 'D'])
      .headers([x: regexp('\\d+', '111'), y: ['a', regexp('\\d+', '111'), 'c']])
      .headers(new Pair('x2', regexp('\\d+', '111')), new Pair('y2', 'a'))
      .build()

    then:
    response.headers == [
      'A': ['111'],
      'B': ['B', '100', 'D'],
      'x': ['111'],
      'y': ['a', '111', 'c'],
      x2: ['111'],
      y2: ['a']
    ]
    response.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header',
      [
        A: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        'B[1]': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)]),
        x: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        'y[1]': new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        x2: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')])
      ]
    )
  }

  def 'supports setting header values from provider states'() {
    when:
    def response = builder
      .header('A', fromProviderState('$a', '111'))
      .build()

    then:
    response.headers == [
      'A': ['111']
    ]
    response.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [:])
    response.generators.categoryFor(Category.HEADER) == [A: new ProviderStateGenerator('$a')]
  }

  def 'supports matching set-cookie response headers'() {
    when:
    def response = builder
      .matchSetCookie('A', '\\d+', '100')
      .build()

    then:
    response.headers == [
      'set-cookie': ['A=100']
    ]
    response.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [
      'set-cookie': new MatchingRuleGroup([new RegexMatcher('\\QA=\\E\\d+')], RuleLogic.OR)
    ])
  }

  def 'allows setting the response status'() {
    when:
    def response = builder
      .status(204)
      .build()

    then:
    response.status == 204
  }

  def 'allows setting the response status using common status groups'() {
    when:
    def response
    if (args.empty) {
      response = builder."$method"().build()
    } else {
      response = builder."$method"(args).build()
    }

    then:
    response.status == status
    response.matchingRules.rulesForCategory('status') == new MatchingRuleCategory('status',
      [
        '': new MatchingRuleGroup([matchingRule])
      ]
    )

    where:

    method              | args            | status | matchingRule
    'informationStatus' | []              | 100    | new StatusCodeMatcher(HttpStatus.Information, [])
    'successStatus'     | []              | 200    | new StatusCodeMatcher(HttpStatus.Success, [])
    'redirectStatus'    | []              | 300    | new StatusCodeMatcher(HttpStatus.Redirect, [])
    'clientErrorStatus' | []              | 400    | new StatusCodeMatcher(HttpStatus.ClientError, [])
    'serverErrorStatus' | []              | 500    | new StatusCodeMatcher(HttpStatus.ServerError, [])
    'nonErrorStatus'    | []              | 200    | new StatusCodeMatcher(HttpStatus.NonError, [])
    'errorStatus'       | []              | 400    | new StatusCodeMatcher(HttpStatus.Error, [])
    'statusCodes'       | [200, 201, 204] | 200    | new StatusCodeMatcher(HttpStatus.StatusCodes, [200, 201, 204])
  }

  def 'allows setting the body of the response as a string value'() {
    when:
    def response = builder
      .body('This is some text')
      .build()

    then:
    response.body.valueAsString() == 'This is some text'
    response.body.contentType.toString() == 'text/plain; charset=ISO-8859-1'
    response.headers['content-type'] == ['text/plain; charset=ISO-8859-1']
  }

  def 'allows setting the body of the response as a string value with a given content type'() {
    when:
    def response = builder
      .body('This is some text', 'text/test-special')
      .build()

    then:
    response.body.valueAsString() == 'This is some text'
    response.body.contentType.toString() == 'text/test-special'
    response.headers['content-type'] == ['text/test-special']
  }

  def 'when setting the body, tries to detect the content type from the body contents'() {
    when:
    def response = builder
      .body('{"value": "This is some text"}')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'application/json'
    response.headers['content-type'] == ['application/json']
  }

  def 'when setting the body, uses any existing content type header'() {
    when:
    def response = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'text/plain'
    response.headers['content-type'] == ['text/plain']
  }

  def 'when setting the body, overrides any existing content type header if the content type is given'() {
    when:
    def response = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}', 'application/json')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'application/json'
    response.headers['content-type'] == ['application/json']
  }

  def 'supports setting the body from a DSLPart object'() {
    when:
    def response = builder
      .body(new PactDslJsonBody().stringType('value', 'This is some text'))
      .build()

    then:
    response.body.valueAsString() == '{"value":"This is some text"}'
    response.body.contentType.toString() == 'application/json'
    response.headers['content-type'] == ['application/json']
    response.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$.value': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE])
      ]
    )
  }

  def 'supports setting the body using a body builder'() {
    when:
    def response = builder
      .body(new PactXmlBuilder('test').build {
        it.attributes = [id: regexp('\\d+', '100')]
      })
      .build()

    then:
    response.body.valueAsString() == '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' +
      System.lineSeparator() + '<test id="100"/>' + System.lineSeparator()
    response.body.contentType.toString() == 'application/xml'
    response.headers['content-type'] == ['application/xml']
    response.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$.test[\'@id\']': new MatchingRuleGroup([new RegexMatcher('\\d+', '100')])
      ]
    )
  }

  def 'supports setting up a content type matcher on the body'() {
    when:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]
    def response = builder
      .bodyMatchingContentType('image/gif', gif1px)
      .build()

    then:
    response.body.value == gif1px
    response.body.contentType.toString() == 'image/gif'
    response.headers['content-type'] == ['image/gif']
    response.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$': new MatchingRuleGroup([new ContentTypeMatcher('image/gif')])
      ]
    )
  }

  def 'allows setting the body of the response as a byte array'() {
    given:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def response = builder
      .body(gif1px)
      .build()

    then:
    response.body.unwrap() == gif1px
    response.body.contentType.toString() == 'application/octet-stream'
    response.headers['content-type'] == ['application/octet-stream']
  }

  def 'allows setting the body of the response as a a byte array with a content type'() {
    given:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def response = builder
      .body(gif1px, 'image/gif')
      .build()

    then:
    response.body.unwrap() == gif1px
    response.body.contentType.toString() == 'image/gif'
    response.headers['content-type'] == ['image/gif']
  }
}
