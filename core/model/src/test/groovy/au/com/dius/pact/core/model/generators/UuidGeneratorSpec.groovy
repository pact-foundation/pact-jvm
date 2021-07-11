package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Json
import spock.lang.Specification
import spock.lang.Unroll

class UuidGeneratorSpec extends Specification {

  def 'default format is lowercase hyphenated'() {
    expect:
    new UuidGenerator().generate([:], null)
      ==~ /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/
  }

  def 'simple'() {
    expect:
    new UuidGenerator(UuidFormat.Simple).generate([:], null) ==~ /^[a-f0-9]{32}$/
  }

  def 'lowercase hyphenated'() {
    expect:
    new UuidGenerator(UuidFormat.LowerCaseHyphenated).generate([:], null)
      ==~ /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/
  }

  def 'uppercase hyphenated'() {
    expect:
    new UuidGenerator(UuidFormat.UpperCaseHyphenated).generate([:], null)
      ==~ /^[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}$/
  }

  def 'urn'() {
    expect:
    new UuidGenerator(UuidFormat.Urn).generate([:], null) ==~
      /^urn:uuid:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/
  }

  @Unroll
  def 'from JSON'() {
    expect:
    UuidGenerator.fromJson(Json.INSTANCE.toJson(json).asObject()) == generator

    where:

    json                              | generator
    [:]                               | new UuidGenerator()
    [format: 'simple']                | new UuidGenerator(UuidFormat.Simple)
    [format: 'lower-case-hyphenated'] | new UuidGenerator(UuidFormat.LowerCaseHyphenated)
    [format: 'upper-case-hyphenated'] | new UuidGenerator(UuidFormat.UpperCaseHyphenated)
    [format: 'URN']                   | new UuidGenerator(UuidFormat.Urn)
    [format: 'other']                 | new UuidGenerator()
  }

  @Unroll
  def 'to JSON'() {
    expect:
    generator.toMap(PactSpecVersion.V4) == json

    where:

    generator                                         | json
    new UuidGenerator()                               | [type: 'Uuid']
    new UuidGenerator(UuidFormat.Simple)              | [type: 'Uuid', format: 'simple']
    new UuidGenerator(UuidFormat.LowerCaseHyphenated) | [type: 'Uuid', format: 'lower-case-hyphenated']
    new UuidGenerator(UuidFormat.UpperCaseHyphenated) | [type: 'Uuid', format: 'upper-case-hyphenated']
    new UuidGenerator(UuidFormat.Urn)                 | [type: 'Uuid', format: 'URN']
  }

}
