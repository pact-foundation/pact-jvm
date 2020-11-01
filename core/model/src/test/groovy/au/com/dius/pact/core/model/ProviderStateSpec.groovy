package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class ProviderStateSpec extends Specification {

  @SuppressWarnings(['PublicInstanceField', 'NonFinalPublicField'])
  static class Pojo {
    public int v = 1
    public String s = 'one'
    public boolean b = false
    public vals = [1, 2, 'three']
  }

  @Unroll
  def 'generates a map of the state'() {
    expect:
    state.toMap() == map

    where:

    state                                              | map
    new ProviderState('test')                          | [name: 'test']
    new ProviderState('test', [:])                     | [name: 'test']
    new ProviderState('test', [a: 'B', b: 1, c: true]) | [name: 'test', params: [a: 'B', b: 1, c: true]]
    new ProviderState('test', [a: [b: ['B', 'C']]])    | [name: 'test', params: [a: [b: ['B', 'C']]]]
    new ProviderState('test', [a: new Pojo()])         | [name: 'test', params: [a: [v: 1, s: 'one', b: false, vals: [1, 2, 'three']]]]
  }
}
