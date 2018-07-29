package au.com.dius.pact.model.generators

import au.com.dius.pact.model.ContentType
import au.com.dius.pact.model.OptionalBody
import spock.lang.Specification
import spock.lang.Unroll

class GeneratorsSpec extends Specification {

  private Generators generators
  private Generator mockGenerator

  def setup() {
    GeneratorsKt.contentTypeHandlers.clear()
    generators = new Generators([:])
    mockGenerator = Mock(Generator) {
      correspondsToMode(_) >> true
    }
  }

  def cleanupSpec() {
    GeneratorsKt.setupDefaultContentTypeHandlers()
  }

  def 'generators invoke the provided closure for each key-value pair'() {
    given:
    generators.addGenerator(Category.HEADER, 'A', mockGenerator)
    generators.addGenerator(Category.HEADER, 'B', mockGenerator)
    def closureCalls = []

    when:
    generators.applyGenerator(Category.HEADER, GeneratorTestMode.Provider) { String key, Generator generator ->
      closureCalls << [key, generator]
    }

    then:
    closureCalls == [['A', mockGenerator], ['B', mockGenerator]]
  }

  def "doesn't invoke the provided closure if not in the appropriate mode"() {
    given:
    def mockGenerator2 = Mock(Generator) {
      correspondsToMode(_) >> false
    }
    generators.addGenerator(Category.HEADER, 'A', mockGenerator)
    generators.addGenerator(Category.HEADER, 'B', mockGenerator2)
    def closureCalls = []

    when:
    generators.applyGenerator(Category.HEADER, GeneratorTestMode.Provider) { String key, Generator generator ->
      closureCalls << [key, generator]
    }

    then:
    closureCalls == [['A', mockGenerator]]
  }

  def 'handle the case of categories that do not have sub-keys'() {
    given:
    generators.addGenerator(Category.STATUS, mockGenerator)
    generators.addGenerator(Category.METHOD, mockGenerator)
    def closureCalls = []

    when:
    generators.applyGenerator(Category.STATUS, GeneratorTestMode.Provider) { String key, Generator generator ->
      closureCalls << [key, generator]
    }

    then:
    closureCalls == [['', mockGenerator]]
  }

  @Unroll
  def 'for bodies, the generator is applied based on the content type'() {
    given:
    GeneratorsKt.contentTypeHandlers['application/json'] = Stub(ContentTypeHandler) {
      processBody(_, _) >> OptionalBody.body('JSON')
    }
    GeneratorsKt.contentTypeHandlers['application/xml'] = Stub(ContentTypeHandler) {
      processBody(_, _) >> OptionalBody.body('XML')
    }

    expect:
    generators.applyBodyGenerators(body, new ContentType(contentType), [:], GeneratorTestMode.Provider) == returnedBody

    where:

    body | contentType | returnedBody
    OptionalBody.empty() | 'text/plain' | OptionalBody.empty()
    OptionalBody.missing() | 'text/plain' | OptionalBody.missing()
    OptionalBody.nullBody() | 'text/plain' | OptionalBody.nullBody()
    OptionalBody.body('text') | 'text/plain' | OptionalBody.body('text')
    OptionalBody.body('text') | 'application/json' | OptionalBody.body('JSON')
    OptionalBody.body('text') | 'application/xml' | OptionalBody.body('XML')

  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'load generator from map - #description'() {
    expect:
    Generators.fromMap(map) == generator

    where:

    description | map  | generator
    'null map'  | null | new Generators()
    'empty map' | [:]  | new Generators()
    'invalid map key' | [other: [type: 'RandomInt', min: 1, max: 10]] | new Generators()
    'invalid map entry' | [method: [min: 1, max: 10]] | new Generators()
    'invalid generator class' | [method: [type: 'RandomXXX', min: 1, max: 10]]  | new Generators()
    'method'    | [method: [type: 'RandomInt', min: 1, max: 10]]  | new Generators().addGenerator(Category.METHOD, '', new RandomIntGenerator(1, 10))
    'path'      | [path: [type: 'RandomString', size: 10]]  | new Generators().addGenerator(Category.PATH, '', new RandomStringGenerator(10))
    'header'    | [header: [A: [type: 'RandomString', size: 10]]]  | new Generators().addGenerator(Category.HEADER, 'A', new RandomStringGenerator(10))
    'query'     | [query: [q: [type: 'RandomString', size: 10]]]  | new Generators().addGenerator(Category.QUERY, 'q', new RandomStringGenerator(10))
    'body'      | [body: ['$.a.b.c': [type: 'RandomString', size: 10]]]  | new Generators().addGenerator(Category.BODY, '$.a.b.c', new RandomStringGenerator(10))
    'status'    | [status: [type: 'RandomInt', min: 1, max: 3]]  | new Generators().addGenerator(Category.STATUS, '', new RandomIntGenerator(1, 3))
  }

}
