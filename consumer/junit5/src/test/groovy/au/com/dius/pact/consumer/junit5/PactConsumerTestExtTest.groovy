package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

class PactConsumerTestExtTest {

  private final subject = new PactConsumerTestExt()
  private providerInfo = new ProviderInfo()
  private pact = new RequestResponsePact(new Provider('junit5_provider'),
    new Consumer('junit5_consumer'), [])
  private ExtensionContext.Store mockStore

  @BeforeEach
  void setup() {
    mockStore = [
      'get': { param ->
        switch (param) {
          case 'executedFragments': []; break
          default: null
        }
      },
      'put': { param, value -> }
    ] as ExtensionContext.Store
  }

  class TestClassInvalidSignature {
    @Pact(provider = 'junit5_provider', consumer = 'junit5_consumer')
    @SuppressWarnings('EmptyMethod')
    def pactMethod() {

    }
  }

  class TestClass {
    @Pact(provider = 'junit5_provider', consumer = 'junit5_consumer')
    @SuppressWarnings('UnusedMethodParameter')
    RequestResponsePact pactMethod(PactDslWithProvider builder) {
      pact
    }
  }

  @PactTestFor(providerName = 'TestClassWithClassLevelAnnotation', pactMethod = 'pactMethod',
    hostInterface = 'localhost', port = '8080', pactVersion = PactSpecVersion.V3)
  class TestClassWithClassLevelAnnotation {
    @SuppressWarnings('UnusedMethodParameter')
    RequestResponsePact pactMethod(PactDslWithProvider builder) {
      pact
    }
  }

  class TestClassWithMethodLevelAnnotation {
    @SuppressWarnings('UnusedMethodParameter')
    @PactTestFor(providerName = 'TestClassWithMethodLevelAnnotation', pactMethod = 'pactMethod',
      hostInterface = 'localhost', port = '8080', pactVersion = PactSpecVersion.V3)
    RequestResponsePact pactMethod(PactDslWithProvider builder) {
      pact
    }
  }

  @PactTestFor(providerName = 'TestClassWithMethodAndClassLevelAnnotation', port = '1234',
    pactVersion = PactSpecVersion.V1_1)
  class TestClassWithMethodAndClassLevelAnnotation {
    @SuppressWarnings('UnusedMethodParameter')
    @PactTestFor(pactMethod = 'pactMethod', hostInterface = 'testServer')
    RequestResponsePact pactMethod(PactDslWithProvider builder) {
      pact
    }
  }

  @PactTestFor(providerName = 'TestClassWithMethodAndClassLevelAnnotation', port = '1234',
    pactVersion = PactSpecVersion.V1_1)
  class TestClassWithMethodAndClassLevelAnnotation2 {
    @SuppressWarnings('UnusedMethodParameter')
    @PactTestFor(pactMethod = 'pactMethod', hostInterface = 'testServer', pactVersion = PactSpecVersion.V3)
    RequestResponsePact pactMethod(PactDslWithProvider builder) {
      pact
    }
  }

  @Test
  @DisplayName('lookupPact throws an exception when pact method is empty and there is no annotated method')
  void test1() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(PactConsumerTestExtTest) } ] as ExtensionContext
      subject.lookupPact(providerInfo, '', context)
    }
  }

  @Test
  @DisplayName('lookupPact throws an exception when pact method is not empty and there is no annotated method')
  void test2() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(PactConsumerTestExtTest) } ] as ExtensionContext
      subject.lookupPact(providerInfo, 'test', context)
    }
  }

  @Test
  @DisplayName('lookupPact throws an exception when pact method does not conform to the correct signature')
  void test3() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(TestClassInvalidSignature) } ] as ExtensionContext
      subject.lookupPact(providerInfo, 'pactMethod', context)
    }
  }

  @Test
  @DisplayName('lookupPact throws an exception when there is no pact method for the provider')
  void test4() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(TestClass) } ] as ExtensionContext
      subject.lookupPact(providerInfo, 'pactMethod', context)
    }
  }

  @Test
  @DisplayName('lookupPact returns the pact from the matching method')
  void test5() {
    def context = [
      'getTestClass': { Optional.of(TestClass) },
      'getTestInstance': { Optional.of(new TestClass()) },
      'getTestMethod': { Optional.empty() },
      'getStore': { mockStore }
    ] as ExtensionContext
    def pact = subject.lookupPact(new ProviderInfo('junit5_provider', 'localhost', '8080',
      PactSpecVersion.V3, ProviderType.SYNCH), 'pactMethod', context)
    assertThat(pact, Matchers.is(this.pact))
  }

  @Test
  @DisplayName('lookupProviderInfo returns default info if there is no annotation')
  void lookupProviderInfo1() {
    def instance = new TestClass()
    def context = [
      'getTestClass': { Optional.of(TestClass) },
      'getTestInstance': { Optional.of(instance) },
      'getTestMethod': { Optional.of(TestClass.methods.find { it.name == 'pactMethod' }) },
      'getStore': { mockStore }
    ] as ExtensionContext
    def providerInfo = subject.lookupProviderInfo(context)
    assertThat(providerInfo.first.providerName, Matchers.is(''))
    assertThat(providerInfo.first.hostInterface, Matchers.is(''))
    assertThat(providerInfo.first.port, Matchers.is(''))
    assertThat(providerInfo.first.pactVersion, Matchers.is(Matchers.nullValue()))
    assertThat(providerInfo.second, Matchers.is(''))
  }

  @Test
  @DisplayName('lookupProviderInfo returns the value from the class annotation')
  void lookupProviderInfo2() {
    def instance = new TestClassWithClassLevelAnnotation()
    def context = [
      'getTestClass': { Optional.of(TestClassWithClassLevelAnnotation) },
      'getTestInstance': { Optional.of(instance) },
      'getTestMethod': { Optional.of(TestClassWithClassLevelAnnotation.methods.find { it.name == 'pactMethod' }) },
      'getStore': { mockStore }
    ] as ExtensionContext
    def providerInfo = subject.lookupProviderInfo(context)
    assertThat(providerInfo.first.providerName, Matchers.is('TestClassWithClassLevelAnnotation'))
    assertThat(providerInfo.first.hostInterface, Matchers.is('localhost'))
    assertThat(providerInfo.first.port, Matchers.is('8080'))
    assertThat(providerInfo.first.pactVersion, Matchers.is(PactSpecVersion.V3))
    assertThat(providerInfo.second, Matchers.is('pactMethod'))
  }

  @Test
  @DisplayName('lookupProviderInfo returns the value from the method level annotation')
  void lookupProviderInfo3() {
    def instance = new TestClassWithMethodLevelAnnotation()
    def context = [
      'getTestClass': { Optional.of(TestClassWithMethodLevelAnnotation) },
      'getTestInstance': { Optional.of(instance) },
      'getTestMethod': { Optional.of(TestClassWithMethodLevelAnnotation.methods.find { it.name == 'pactMethod' }) },
      'getStore': { mockStore }
    ] as ExtensionContext
    def providerInfo = subject.lookupProviderInfo(context)
    assertThat(providerInfo.first.providerName, Matchers.is('TestClassWithMethodLevelAnnotation'))
    assertThat(providerInfo.first.hostInterface, Matchers.is('localhost'))
    assertThat(providerInfo.first.port, Matchers.is('8080'))
    assertThat(providerInfo.first.pactVersion, Matchers.is(PactSpecVersion.V3))
    assertThat(providerInfo.second, Matchers.is('pactMethod'))
  }

  @Test
  @DisplayName('lookupProviderInfo returns the value from the method and then class level annotation')
  void lookupProviderInfo4() {
    def instance = new TestClassWithMethodAndClassLevelAnnotation()
    def context = [
      'getTestClass': { Optional.of(TestClassWithMethodAndClassLevelAnnotation) },
      'getTestInstance': { Optional.of(instance) },
      'getTestMethod': {
        Optional.of(TestClassWithMethodAndClassLevelAnnotation.methods.find { it.name == 'pactMethod' })
      },
      'getStore': { mockStore }
    ] as ExtensionContext
    def providerInfo = subject.lookupProviderInfo(context)
    assertThat(providerInfo.first.providerName, Matchers.is('TestClassWithMethodAndClassLevelAnnotation'))
    assertThat(providerInfo.first.hostInterface, Matchers.is('testServer'))
    assertThat(providerInfo.first.port, Matchers.is('1234'))
    assertThat(providerInfo.first.pactVersion, Matchers.is(PactSpecVersion.V1_1))
    assertThat(providerInfo.second, Matchers.is('pactMethod'))
  }

  @Test
  @DisplayName('lookupProviderInfo returns the value from the method and then class level annotation (test 2)')
  void lookupProviderInfo5() {
    def instance = new TestClassWithMethodAndClassLevelAnnotation2()
    def context = [
      'getTestClass': { Optional.of(TestClassWithMethodAndClassLevelAnnotation2) },
      'getTestInstance': { Optional.of(instance) },
      'getTestMethod': {
        Optional.of(TestClassWithMethodAndClassLevelAnnotation2.methods.find { it.name == 'pactMethod' })
      },
      'getStore': { mockStore }
    ] as ExtensionContext
    def providerInfo = subject.lookupProviderInfo(context)
    assertThat(providerInfo.first.pactVersion, Matchers.is(PactSpecVersion.V3))
  }

}
