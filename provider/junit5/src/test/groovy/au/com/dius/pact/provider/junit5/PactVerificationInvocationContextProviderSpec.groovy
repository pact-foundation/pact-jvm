package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.NoPactsFoundException
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import au.com.dius.pact.provider.junitsupport.loader.PactSource
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Issue
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

  @Provider('myAwesomeService')
  @Consumer('doesNotExist')
  @PactFolder('pacts')
  static class TestClassWithNoPacts {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @Consumer('doesNotExist')
  @PactFolder('pacts')
  @IgnoreNoPactsToVerify
  static class TestClassWithNoPactsWithIgnore {
    @TestTarget
    Target target
  }

  static class InvalidStateChangeTestClass {
    @State('one')
    protected void incorrectStateChangeParameters(int one, String two, Map three) { }
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

    au.com.dius.pact.core.model.PactSource pactSource = null
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

  @Issue('#1007')
  def 'provideTestTemplateInvocationContexts throws an exception if there are no pacts to verify'() {
    when:
    provider.provideTestTemplateInvocationContexts(['getTestClass': {
      Optional.of(TestClassWithNoPacts)
    } ] as ExtensionContext)

    then:
    def exp = thrown(NoPactsFoundException)
    exp.message.startsWith('No Pact files were found to verify')
  }

  @Issue('#768')
  def 'returns a dummy test if there are no pacts to verify and IgnoreNoPactsToVerify is present'() {
    when:
    def result = provider.provideTestTemplateInvocationContexts(['getTestClass': {
      Optional.of(TestClassWithNoPactsWithIgnore)
    } ] as ExtensionContext).iterator().toList()

    then:
    result.size() == 1
    result.first() instanceof DummyTestTemplate
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
