package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.Pact
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

class PactConsumerTestExtSpec {

  def subject = new PactConsumerTestExt()
  def providerInfo = new ProviderInfo()
  def pact = new RequestResponsePact(new Provider('junit5_provider'), new Consumer('junit5_consumer'), [])

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

  @Test
  @DisplayName('lookupPact throws an exception when pact method is empty and there is no annotated method')
  void test1() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(PactConsumerTestExtSpec) } ] as ExtensionContext
      subject.lookupPact(providerInfo, '', context)
    }
  }

  @Test
  @DisplayName('lookupPact throws an exception when pact method is not empty and there is no annotated method')
  void test2() {
    assertThrows(UnsupportedOperationException) {
      def context = ['getTestClass': { Optional.of(PactConsumerTestExtSpec) } ] as ExtensionContext
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
      'getTestMethod': { Optional.empty() }
    ] as ExtensionContext
    def pact = subject.lookupPact(new ProviderInfo('junit5_provider', 'localhost', '8080', PactSpecVersion.V3),
      'pactMethod', context)
    assertThat(pact, Matchers.is(this.pact))
  }

}
