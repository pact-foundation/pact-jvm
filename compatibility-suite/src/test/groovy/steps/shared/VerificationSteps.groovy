package steps.shared

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class VerificationData {
  ProviderInfo providerInfo = null
  ProviderVerifier verifier = null
  List<VerificationResult> verificationResults = []
  Map<String, String> verificationProperties = [:]
  Closure responseFactory = null
  List providerStateParams = []

  // Pico container needs this constructor, otherwise it tries to inject all the fields and blows up
  @SuppressWarnings('UnnecessaryConstructor')
  VerificationData() { }
}

class VerificationSteps {
  VerificationData verificationData

  void providerStateCallback(ProviderState state, String isSetup) {
    verificationData.providerStateParams << [state, isSetup]
  }

  void failingProviderStateCallback(ProviderState state, String isSetup) {
    verificationData.providerStateParams << [state, isSetup]
    throw new RuntimeException('failingProviderStateCallback has failed')
  }

  VerificationSteps(VerificationData verificationData) {
    this.verificationData = verificationData
  }

  @Given('a provider state callback is configured')
  void a_provider_state_callback_is_configured() {
    verificationData.providerInfo.stateChangeRequestFilter = this.&providerStateCallback
  }

  @Given('a provider state callback is configured, but will return a failure')
  void a_provider_state_callback_is_configured_but_will_return_a_failure() {
    verificationData.providerInfo.stateChangeRequestFilter = this.&failingProviderStateCallback
  }

  @Then('the provider state callback will be called before the verification is run')
  void the_provider_state_callback_will_be_called_before_the_verification_is_run() {
    assert !verificationData.providerStateParams.findAll { p -> p[1] == 'setup'  }.empty
  }

  @Then('the provider state callback will receive a setup call with {string} as the provider state parameter')
  void the_provider_state_callback_will_receive_a_setup_call_with_as_the_provider_state_parameter(String state) {
    assert !verificationData.providerStateParams.findAll { p -> p[0].name == state && p[1] == 'setup' }.empty
  }

  @Then('the provider state callback will be called after the verification is run')
  void the_provider_state_callback_will_be_called_after_the_verification_is_run() {
    assert !verificationData.providerStateParams.findAll { p -> p[1] == 'teardown' }.empty
  }

  @Then('the provider state callback will receive a teardown call {string} as the provider state parameter')
  void the_provider_state_callback_will_receive_a_teardown_call_as_the_provider_state_parameter(String providerState) {
    assert !verificationData.providerStateParams.findAll { p -> p[0].name == providerState && p[1] == 'teardown' }.empty
  }

  @Then('the provider state callback will NOT receive a teardown call')
  void the_provider_state_callback_will_not_receive_a_teardown_call() {
    assert verificationData.providerStateParams.findAll { p -> p[1] == 'teardown' }.empty
  }

  @Then('a warning will be displayed that there was no provider state callback configured for provider state {string}')
  void a_warning_will_be_displayed_that_there_was_no_provider_state_callback_configured(String state) {
    assert verificationData.verifier.reporters.first().events.find { it.state == state }
  }

  @When('the verification is run')
  void the_verification_is_run() {
    verificationData.verifier = new ProviderVerifier()
    verificationData.verifier.projectHasProperty = { name -> verificationData.verificationProperties.containsKey(name) }
    verificationData.verifier.projectGetProperty = { name -> verificationData.verificationProperties[name] }
    verificationData.verifier.reporters = [ new StubVerificationReporter() ]

    if (verificationData.responseFactory) {
      verificationData.verifier.responseFactory = verificationData.responseFactory
    }

    verificationData.verificationResults = verificationData.verifier.verifyProvider(verificationData.providerInfo)
  }

  @Then('the verification will be successful')
  void the_verification_will_be_successful() {
    assert verificationData.verificationResults.inject(true) { acc, result ->
      acc && result instanceof VerificationResult.Ok
    }
  }

  @Then('the verification will NOT be successful')
  void the_verification_will_not_be_successful() {
    assert verificationData.verificationResults.any { it instanceof VerificationResult.Failed }
  }

  @Then('the verification results will contain a {string} error')
  void the_verification_results_will_contain_a_error(String error) {
    assert verificationData.verificationResults.any {
      it instanceof VerificationResult.Failed && it.description == error
    }
  }
}
