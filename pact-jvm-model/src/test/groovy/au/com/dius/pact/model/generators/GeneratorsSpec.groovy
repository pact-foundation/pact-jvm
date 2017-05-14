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
    mockGenerator = Mock(Generator)
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
    generators.applyGenerator(Category.HEADER) { String key, Generator generator ->
      closureCalls << [key, generator]
    }

    then:
    closureCalls == [['A', mockGenerator], ['B', mockGenerator]]
  }

  def 'handle the case of categories that do not have sub-keys'() {
    given:
    generators.addGenerator(Category.STATUS, mockGenerator)
    generators.addGenerator(Category.METHOD, mockGenerator)
    def closureCalls = []

    when:
    generators.applyGenerator(Category.STATUS) { String key, Generator generator ->
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
    generators.applyBodyGenerators(body, new ContentType(contentType)) == returnedBody

    where:

    body | contentType | returnedBody
    OptionalBody.empty() | 'text/plain' | OptionalBody.empty()
    OptionalBody.missing() | 'text/plain' | OptionalBody.missing()
    OptionalBody.nullBody() | 'text/plain' | OptionalBody.nullBody()
    OptionalBody.body('text') | 'text/plain' | OptionalBody.body('text')
    OptionalBody.body('text') | 'application/json' | OptionalBody.body('JSON')
    OptionalBody.body('text') | 'application/xml' | OptionalBody.body('XML')

  }

}
