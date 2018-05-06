package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.loader.PactFolderLoader
import au.com.dius.pact.provider.junit.loader.PactLoader
import au.com.dius.pact.provider.junit.loader.PactSource
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
class PactVerificationInvocationContextProviderSpec extends Specification {

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  static class TestClassWithAnnotation {
    @TestTarget
    Target target
  }

  @PactSource(TestPactLoader)
  @PactFilter('state 2')
  static class ChildClass extends TestClassWithAnnotation {

  }

  static class InvalidStateChangeTestClass {

    @State('one')
    protected void incorrectStateChangeParameters(int one, String two, Map three) {

    }

  }

  static class InvalidStateChangeTestClass2 extends InvalidStateChangeTestClass {

    @State('two')
    void incorrectStateChangeParameter(List list) {

    }

  }

  static class ValidStateChangeTestClass {

    @State('three')
    void correctStateChange() {

    }

    @State('three')
    void correctStateChange2(Map parameters) {

    }

  }

  static class TestPactLoader implements PactLoader {

    private final Class clazz

    TestPactLoader(Class clazz) {
      this.clazz = clazz
    }

    @Override
    List<Pact> load(String providerName) throws IOException {
      []
    }

    au.com.dius.pact.model.PactSource pactSource = null
  }

  private PactVerificationInvocationContextProvider provider

  def setup() {
    provider = new PactVerificationInvocationContextProvider()
  }

  @Unroll
  def 'only supports tests with a provider annotation'() {
    expect:
    provider.supportsTestTemplate(['getTestClass': { Optional.of(testClass) } ] as ExtensionContext) == isSupported

    where:

    testClass                                     | isSupported
    TestClassWithAnnotation                       | true
    PactVerificationInvocationContextProviderSpec | false
    ChildClass                                    | true
  }

  def 'findPactSources throws an exception if there are no defined pact sources on the test class'() {
    when:
    provider.findPactSources(['getTestClass': {
      Optional.of(PactVerificationInvocationContextProviderSpec)
    } ] as ExtensionContext)

    then:
    def exp = thrown(UnsupportedOperationException)
    exp.message == 'At least one pact source must be present on the test class'
  }

  def 'findPactSources returns a pact loader for each discovered pact source annotation'() {
    when:
    def sources = provider.findPactSources([
      'getTestClass': { Optional.of(TestClassWithAnnotation) } ] as ExtensionContext
    )
    def childSources = provider.findPactSources([
      'getTestClass': { Optional.of(ChildClass) } ] as ExtensionContext
    )

    then:
    sources.size() == 1
    sources.first() instanceof PactFolderLoader
    sources.first().path.toString() == 'pacts'
    childSources.size() == 2
    childSources.first() instanceof PactFolderLoader
    childSources.first().path.toString() == 'pacts'
    childSources[1] instanceof TestPactLoader
    childSources[1].clazz == ChildClass
  }

  def 'returns a junit extension for each interaction in all the discovered pact files'() {
    when:
    def extensions = provider.provideTestTemplateInvocationContexts([
      'getTestClass': { Optional.of(TestClassWithAnnotation) } ] as ExtensionContext
    )

    then:
    extensions.count() == 3
  }

  def 'supports filtering the discovered pact files'() {
    when:
    def extensions = provider.provideTestTemplateInvocationContexts([
      'getTestClass': { Optional.of(ChildClass) } ] as ExtensionContext
    )

    then:
    extensions.count() == 1
  }

  @Unroll
  def 'throws an exception if there are invalid state change methods'() {
    when:
    provider.validateStateChangeMethods(testClass)

    then:
    thrown(UnsupportedOperationException)

    where:

    testClass << [InvalidStateChangeTestClass, InvalidStateChangeTestClass2]
  }

  def 'does not throws an exception if there are valid state change methods'() {
    when:
    provider.validateStateChangeMethods(ValidStateChangeTestClass)

    then:
    notThrown(UnsupportedOperationException)
  }

}
