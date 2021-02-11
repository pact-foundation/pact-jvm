package au.com.dius.pact.core.model.generators

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('GStringExpressionWithinString')
class ProviderStateGeneratorSpec extends Specification {

  private ProviderStateGenerator generator

  def setup() {
    generator = new ProviderStateGenerator('a')
  }

  @Unroll
  def 'uses the provider state map from the context'() {
    expect:
    generator.generate(context, null)  == value

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
    new ProviderStateGenerator(expression).generate([providerState: context], null) == value

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
    def result = new ProviderStateGenerator(expression).generate([providerState: context], null)

    then:
    result == '{\n  "entityName": "Entity-Name",\n  "xml": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n"\n}'
  }
}
