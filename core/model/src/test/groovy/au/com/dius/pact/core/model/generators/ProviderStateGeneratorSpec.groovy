package au.com.dius.pact.core.model.generators

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
    [a: 'A', b: 100] | '/${a}/${c}' | '/A/null'
  }

}
