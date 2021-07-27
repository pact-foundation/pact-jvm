package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.NotFoundHalResponse
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
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
import au.com.dius.pact.provider.junitsupport.loader.PactUrl
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import java.util.stream.Collectors

// TODO: Groovy mocks don't work on JDK 16
@Ignore
@SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter', 'LineLength'])
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

  @Provider('myAwesomeService')
  @Consumer('doesNotExist')
  @PactFolder('pacts')
  @IgnoreNoPactsToVerify(ignoreIoErrors = 'true')
  static class TestClassWithNoPactsWithIgnoreIoErrors {
    @TestTarget
    Target target
  }

  @Provider
  @PactFolder('pacts')
  static class TestClassWithEmptyProvider {
    @TestTarget
    Target target
  }

  @Provider('someone')
  @PactUrl(urls = [ 'http://localhost.dev.somewhere:9765' ])
  static class TestClassWithInvalidUrl {
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

  @IsContractTest
  static class TestClassWithPactSourceOnAnnotation {
    @TestTarget
    Target target
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
    exp.message == 'Did not find any PactSource annotations. At least one pact source must be set'
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
    childSources[0] instanceof TestPactLoader
    childSources[0].clazz == ChildClass
    childSources[1] instanceof PactFolderLoader
    childSources[1].path.toString() == 'pacts'
  }

  def 'findPactSources returns a pact loader for each discovered pact source on any annotations'() {
    when:
    def sources = provider.findPactSources([
      'getTestClass': { Optional.of(TestClassWithPactSourceOnAnnotation) } ] as ExtensionContext
    )
    then:
    sources.size() == 1
    sources.first() instanceof PactFolderLoader
    sources.first().path.toString() == 'pacts'
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

  @Issue('#1104')
  @RestoreSystemProperties
  def 'supports filtering the interactions'() {
    given:
    System.setProperty('pact.filter.description', 'Get data 2')

    when:
    def extensions = provider.provideTestTemplateInvocationContexts([
      'getTestClass': { Optional.of(TestClassWithAnnotation) } ] as ExtensionContext
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

  @Issue('#1160')
  @RestoreSystemProperties
  def 'supports provider name from system properties'() {
    given:
    System.setProperty('pact.provider.name', 'myAwesomeService')

    when:
    def extensions = provider.provideTestTemplateInvocationContexts([
      'getTestClass': { Optional.of(TestClassWithEmptyProvider) } ] as ExtensionContext
    ).collect(Collectors.toList())

    then:
    !extensions.empty
    extensions.every { it.serviceName == 'myAwesomeService' }
  }

  @Issue('#1225')
  def 'provideTestTemplateInvocationContexts throws an exception if load request fails with an exception'() {
    when:
    provider.provideTestTemplateInvocationContexts(['getTestClass': {
      Optional.of(TestClassWithInvalidUrl)
    } ] as ExtensionContext)

    then:
    thrown(UnknownHostException)
  }

  @Issue('#1324')
  def 'handling exceptions test - with no annotation throws the exception'() {
    given:
    def context = Mock(ExtensionContext) {
      getRequiredTestClass() >> TestClassWithAnnotation
    }
    def valueResolver = null

    when:
    provider.handleException(context, valueResolver, new RuntimeException())

    then:
    thrown(RuntimeException)
  }

  @Issue('#1324')
  def 'handling exceptions test - with IgnoreNoPactsToVerify annotation and an IO exception throws the exception'() {
    given:
    def context = Mock(ExtensionContext) {
      getRequiredTestClass() >> TestClassWithNoPactsWithIgnore
    }
    def valueResolver = null

    when:
    provider.handleException(context, valueResolver, new IOException())

    then:
    thrown(IOException)
  }

  @Issue('#1324')
  def 'handling exceptions test - with IgnoreNoPactsToVerify(ignoreIoErrors = "true") annotation and an IO exception does not throw the exception'() {
    given:
    def context = Mock(ExtensionContext) {
      getRequiredTestClass() >> TestClassWithNoPactsWithIgnoreIoErrors
    }
    def valueResolver = null

    when:
    def result = provider.handleException(context, valueResolver, new IOException())

    then:
    notThrown(IOException)
    result.empty
  }

  @Issue('#1324')
  @RestoreSystemProperties
  def 'handling exceptions test - with IgnoreNoPactsToVerify annotation and ignoreIoErrors system property set and an IO exception does not throw the exception'() {
    given:
    def context = Mock(ExtensionContext) {
      getRequiredTestClass() >> TestClassWithNoPactsWithIgnore
    }
    def valueResolver = SystemPropertyResolver.INSTANCE
    System.setProperty('pact.verification.ignoreIoErrors', 'true')

    when:
    def result = provider.handleException(context, valueResolver, new IOException())

    then:
    noExceptionThrown()
    result.empty
  }

  @Issue('#1324')
  def 'handling exceptions test - with IgnoreNoPactsToVerify annotation and NotFoundHalResponse exception does not throw the exception'() {
    given:
    def context = Mock(ExtensionContext) {
      getRequiredTestClass() >> TestClassWithNoPactsWithIgnore
    }
    def valueResolver = null

    when:
    def result = provider.handleException(context, valueResolver, new NotFoundHalResponse())

    then:
    noExceptionThrown()
    result.empty
  }
}
