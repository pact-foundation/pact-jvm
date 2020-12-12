package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PactVerificationTaskSpec extends Specification {

  private PactVerificationTask task
  private PactPlugin plugin
  private Project project
  private IProviderVerifier verifier

  def setup() {
    project = ProjectBuilder.builder().build()
    plugin = new PactPlugin()
    plugin.apply(project)

    project.pact {
      serviceProviders {
        'Test Service' {

        }
      }
    }
    project.evaluate()

    task = project.tasks.pactVerify_Test_Service
    verifier = Mock(IProviderVerifier)
    task.verifier = verifier
  }

  def 'raises an exception if the verification fails'() {
    given:
    def provider = new Provider('Test')
    def consumer = new Consumer('Test')

    when:
    task.verifyPact()

    then:
    def ex = thrown(GradleScriptException)
    ex.message == 'There were 1 non-pending pact failures for provider Test Service'
    1 * verifier.verifyProvider(_) >> [new VerificationResult.Failed([], '', '', [
      new VerificationFailureType.MismatchFailure(new StatusMismatch(200, 400),
        new RequestResponseInteraction('Test'), new RequestResponsePact(provider, consumer))
    ], false, null) ]
  }

  def 'does not raise an exception if the verification passed'() {
    when:
    task.verifyPact()

    then:
    noExceptionThrown()
    1 * verifier.verifyProvider(_) >> [ new VerificationResult.Ok(null) ]
  }

  def 'does not raise an exception if the pact is pending'() {
    when:
    task.verifyPact()

    then:
    noExceptionThrown()
    1 * verifier.verifyProvider(_) >> [new VerificationResult.Failed([], '', '', [], true, null) ]
  }
}
