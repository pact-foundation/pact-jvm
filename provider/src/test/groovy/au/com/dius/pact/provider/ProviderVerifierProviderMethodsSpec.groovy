package au.com.dius.pact.provider

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.provider.methodfinder.FinderChild
import spock.lang.Specification

import java.util.function.Supplier

class ProviderVerifierProviderMethodsSpec extends Specification {

  ProviderVerifier verifier

  def setup() {
    verifier = new ProviderVerifier()
  }

  def 'verifyResponseByInvokingProviderMethods uses the methods on the provider method classes without scanning'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    IConsumerInfo consumer = Stub()
    def classpathCalls = 0
    verifier.providerMethodClasses = { [FinderChild] } as Supplier
    verifier.projectClasspath = { classpathCalls++; [] } as Supplier

    when:
    def result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer,
      finderMessage('child message', 'child'), 'Test', [:], false)

    then:
    result instanceof VerificationResult.Ok
    classpathCalls == 0
  }

  def 'verifyResponseByInvokingProviderMethods scans the packages as well as the provider method classes'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    provider.packagesToScan = ['au.com.dius.pact.provider.methodfinder.sub']
    IConsumerInfo consumer = Stub()
    verifier.providerMethodClasses = { [FinderChild] } as Supplier

    when:
    def result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer,
      finderMessage('sub package message', 'sub'), 'Test', [:], false)

    then:
    result instanceof VerificationResult.Ok
  }

  def 'verifyResponseByInvokingProviderMethods scans the classpath if the method is not on the classes'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    IConsumerInfo consumer = Stub()
    verifier.providerMethodClasses = { [FinderChild] } as Supplier

    when:
    def result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer,
      finderMessage('sub package message', 'sub'), 'Test', [:], false)

    then:
    result instanceof VerificationResult.Ok
  }

  def 'verifyResponseByInvokingProviderMethods fails if no annotated method is found'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    provider.packagesToScan = ['au.com.dius.pact.provider.methodfinder.sub']
    IConsumerInfo consumer = Stub()
    verifier.providerMethodClasses = { [FinderChild] } as Supplier

    when:
    def result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer,
      finderMessage('unknown message', 'none'), 'Test', [:], false)

    then:
    result instanceof VerificationResult.Failed
    result.failures['abc123'][0].e.message.startsWith('No annotated methods were found for interaction')
  }

  private static Message finderMessage(String description, String from) {
    new Message(description, [], OptionalBody.body("{\"from\":\"${from}\"}".bytes, ContentType.JSON),
      new MatchingRulesImpl(), new Generators(), [:], 'abc123')
  }
}
