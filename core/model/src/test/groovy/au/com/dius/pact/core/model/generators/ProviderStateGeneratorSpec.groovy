package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings('GStringExpressionWithinString')
class ProviderStateGeneratorSpec extends Specification {

  private ProviderStateGenerator generator

  def setup() {
    generator = new ProviderStateGenerator('a')
  }

  @Unroll
  def 'uses the provider state map from the context'() {
    expect:
    generator.generate(context)  == value

    where:

    context                       | value
    [:]                           | null
    [providerState: 'test']       | null
    [providerState: [:]]          | null
    [providerState: [a: 'Value']] | 'Value'
  }

  @Unroll
  def 'parsers any expressions from the context'() {
    expect:
    new ProviderStateGenerator(expression).generate([providerState: context]) == value

    where:

    context          | expression   | value
    [a: 'A']         | 'a'          | 'A'
    [a: 100]         | 'a'          | 100
    [a: 'A', b: 100] | '/${a}/${b}' | '/A/100'
    [a: 'A', b: 100] | '/${a}/${c}' | '/A/'
  }

  @Issue('#1031')
  def 'handles encoded values in the expressions'() {
    given:
    def expression = '{\n  "entityName": "${eName}",\n  "xml": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n"\n}'
    def context = [eName: 'Entity-Name']

    when:
    def result = new ProviderStateGenerator(expression).generate([providerState: context])

    then:
    result == '{\n  "entityName": "Entity-Name",\n  "xml": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n"\n}'
  }

  def 'toMap test'() {
    expect:
    new ProviderStateGenerator('/${a}/${b}').toMap(PactSpecVersion.V3) ==
      [type: 'ProviderState', expression: '/${a}/${b}', dataType: 'RAW']
  }

  @RestoreSystemProperties
  def 'toMap restores the expressions if the markers are overridden'() {
    given:
    System.setProperty('pact.expressions.start', '<<')
    System.setProperty('pact.expressions.end', '>>')

    expect:
    new ProviderStateGenerator('/<<a>>/<<b>>').toMap(PactSpecVersion.V3) ==
      [type: 'ProviderState', expression: '/${a}/${b}', dataType: 'RAW']
  }

  def 'fromJson test'() {
    expect:
    ProviderStateGenerator.fromJson(new JsonValue.Object([
      type: new JsonValue.StringValue('ProviderState'),
      expression: new JsonValue.StringValue('/${a}/${b}'),
      dataType: new JsonValue.StringValue('RAW')
    ])) == new ProviderStateGenerator('/${a}/${b}')
  }

  @RestoreSystemProperties
  def 'fromJson updates the expressions if the markers are overridden'() {
    given:
    System.setProperty('pact.expressions.start', '<<')
    System.setProperty('pact.expressions.end', '>>')

    expect:
    ProviderStateGenerator.fromJson(new JsonValue.Object([
      type: new JsonValue.StringValue('ProviderState'),
      expression: new JsonValue.StringValue('/${a}/${b}'),
      dataType: new JsonValue.StringValue('RAW')
    ])) == new ProviderStateGenerator('/<<a>>/<<b>>')
  }
}
