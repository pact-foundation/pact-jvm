package au.com.dius.pact.consumer.junit

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Method

class JUnitTestSupportSpec extends Specification {

  @SuppressWarnings('EmptyMethod')
  void methodWithNoAnnotation() { }

  @Pact(consumer = 'test')
  @SuppressWarnings('EmptyMethod')
  String methodWithIncorrectReturnType() { }

  @Pact(consumer = 'test')
  @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
  RequestResponsePact methodWithIncorrectParameter(String test) { }

  @Pact(consumer = 'test')
  @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
  RequestResponsePact methodWithMoreThanOneParameter(PactDslWithProvider test, PactDslWithProvider test2) { }

  @Pact(consumer = 'test')
  @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
  RequestResponsePact correctMethod(PactDslWithProvider test) { }

  @Pact(consumer = 'test')
  @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
  V4Pact correctMethodV4(PactDslWithProvider test) { }

  @Unroll
  def 'raises an exception when the method does not conform - #desc'() {
    when:
    JUnitTestSupport.conformsToSignature(method, PactSpecVersion.V3)

    then:
    thrown(exception)

    where:

    method                                     | exception                     | desc
    null                                       | NullPointerException          | 'Null Method'
    luMethod('methodWithIncorrectReturnType')  | UnsupportedOperationException | 'Incorrect Return Type'
    luMethod('methodWithIncorrectParameter')   | UnsupportedOperationException | 'Incorrect Parameter Type'
    luMethod('methodWithMoreThanOneParameter') | UnsupportedOperationException | 'More than one Parameter'
    luMethod('correctMethodV4')                | UnsupportedOperationException | 'Incorrect V4 Return Type'
  }

  @Unroll
  def 'does not raise an exception when #desc'() {
    when:
    JUnitTestSupport.conformsToSignature(method, specVersion)

    then:
    noExceptionThrown()

    where:

    method                             | specVersion        | desc
    luMethod('methodWithNoAnnotation') | PactSpecVersion.V3 | 'no @Pact annotation'
    luMethod('correctMethod')          | PactSpecVersion.V3 | 'correct signature'
    luMethod('correctMethodV4')        | PactSpecVersion.V4 | 'correct signature'
  }

  static Method luMethod(String methodName) {
    JUnitTestSupportSpec.methods.find { it.name == methodName }
  }
}
