package steps.v3

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.StringSource
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.provider.ConsumerInfo
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import steps.shared.CompatibilitySuiteWorld
import steps.shared.SharedHttpProvider
import steps.shared.VerificationData

import static au.com.dius.pact.core.support.json.JsonParser.parseString

class HttpProvider {
  CompatibilitySuiteWorld world
  SharedHttpProvider sharedProvider
  VerificationData verificationData

  HttpProvider(CompatibilitySuiteWorld world, SharedHttpProvider sharedProvider, VerificationData verificationData) {
    this.world = world
    this.sharedProvider = sharedProvider
    this.verificationData = verificationData
  }

  @Given('a Pact file for interaction {int} is to be verified with the following provider states defined:')
  void a_pact_file_for_interaction_is_to_be_verified_with_the_following_provider_states_defined(
    Integer num,
    DataTable dataTable
  ) {
    def interaction = world.interactions[num - 1].copy()
    interaction.providerStates.addAll(dataTable.asMaps().collect {
      if (it['Parameters']) {
        new ProviderState(it['State Name'], Json.INSTANCE.fromJson(parseString(it['Parameters'])))
      } else {
        new ProviderState(it['State Name'])
      }
    })
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [interaction])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V3)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Then('the provider state callback will receive a setup call with {string} and the following parameters:')
  void the_provider_state_callback_will_receive_a_setup_call_with_and_the_following_parameters(
    String state,
    DataTable dataTable
  ) {
    def params = dataTable.asMaps().first().collectEntries {
      [it.key, Json.INSTANCE.fromJson(parseString(it.value))]
    }
    assert !verificationData.providerStateParams.findAll { p ->
      p[0].name == state && p[0].params == params && p[1] == 'setup'
    }.empty
  }

  @Then('the provider state callback will receive a teardown call {string} and the following parameters:')
  void the_provider_state_callback_will_receive_a_teardown_call_and_the_following_parameters(
    String state,
    DataTable dataTable
  ) {
    def params = dataTable.asMaps().first().collectEntries {
      [it.key, Json.INSTANCE.fromJson(parseString(it.value))]
    }
    assert !verificationData.providerStateParams.findAll { p ->
      p[0].name == state && p[0].params == params && p[1] == 'teardown'
    }.empty
  }
}
