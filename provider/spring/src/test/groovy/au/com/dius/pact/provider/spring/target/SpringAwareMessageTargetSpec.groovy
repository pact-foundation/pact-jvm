package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.ProviderMethodFinder
import org.junit.runners.model.TestClass
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import spock.lang.Specification

class SpringAwareMessageTargetSpec extends Specification {

  static class MessageProviderBean {
    @PactVerifyProvider('a bean message')
    String message() { '{"from":"bean"}' }
  }

  static class MessageTestClass {
    @PactVerifyProvider('a test class message')
    String message() { '{"from":"test"}' }
  }

  def 'searches the test class and the beans in the application context for provider methods'() {
    given:
    def beanFactory = new DefaultListableBeanFactory()
    beanFactory.registerBeanDefinition('messageProvider', new RootBeanDefinition(MessageProviderBean))
    def target = new SpringAwareMessageTarget()
    target.beanFactory = beanFactory
    def testInstance = new MessageTestClass()
    target.setTestClass(new TestClass(MessageTestClass), testInstance)

    when:
    def classes = target.providerMethodClasses()

    then:
    classes == [MessageTestClass, MessageProviderBean]
    ProviderMethodFinder.findOnClasses(classes, 'a bean message')*.declaringClass == [MessageProviderBean]
    ProviderMethodFinder.findOnClasses(classes, 'a test class message')*.declaringClass == [MessageTestClass]
  }

  def 'only searches the test class if the bean factory can not list the beans'() {
    given:
    def target = new SpringAwareMessageTarget()
    target.beanFactory = Mock(BeanFactory)
    target.setTestClass(new TestClass(MessageTestClass), new MessageTestClass())

    expect:
    target.providerMethodClasses() == [MessageTestClass]
  }
}
