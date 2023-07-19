package steps.v3

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class HttpConsumer {
  def builder
  RequestResponsePact pact
  String pactJsonStr
  JsonValue.Object pactJson

  @Given('an integration is being defined for a consumer test')
  void an_integration_is_being_defined_for_a_consumer_test() {
    builder = ConsumerPactBuilder
      .consumer('V3 consumer')
      .hasPactWith('V3 provider')
  }

  @Given('a provider state {string} is specified')
  void a_provider_state_is_specified(String state) {
    builder = builder.given(state)
  }

  @Given('a provider state {string} is specified with the following data:')
  void a_provider_state_is_specified_with_the_following_data(String state, DataTable dataTable) {
    def entry = dataTable.entries()
      .first()
      .collectEntries {
        [it.key, JsonParser.parseString(it.value).unwrap()]
      }
    builder = builder.given(state, entry)
  }

  @When('the Pact file for the test is generated')
  void the_pact_file_for_the_test_is_generated() {
    pact = builder.uponReceiving('some request')
      .path('/path')
      .willRespondWith()
      .toPact()
    pactJsonStr = Json.INSTANCE.prettyPrint(pact.toMap(PactSpecVersion.V3))
    pactJson = JsonParser.parseString(pactJsonStr).asObject()
  }

  @Then('the interaction in the Pact file will contain {int} provider state(s)')
  void the_interaction_in_the_pact_file_will_contain_provider_states(Integer states) {
    JsonValue.Object interaction = pactJson['interactions'].asArray().get(0).asObject()
    JsonValue.Array providerStates = interaction['providerStates'].asArray()
    assert providerStates.size() == states
  }

  @Then('the interaction in the Pact file will contain provider state {string}')
  void the_interaction_in_the_pact_file_will_contain_provider_state(String state) {
    JsonValue.Object interaction = pactJson['interactions'].asArray().get(0).asObject()
    JsonValue.Array providerStates = interaction['providerStates'].asArray()
    assert providerStates.values.find { it.get('name').toString() == state } != null
  }

  @Then('the provider state {string} in the Pact file will contain the following parameters:')
  void the_provider_state_in_the_pact_file_will_contain_the_following_parameters(String state, DataTable dataTable) {
    def entry = dataTable.entries().first()['parameters']
    JsonValue.Object interaction = pactJson['interactions'].asArray().get(0).asObject()
    JsonValue.Array providerStates = interaction['providerStates'].asArray()
    def providerState = providerStates.values.find { it.get('name').toString() == state }
    assert providerState.get('params').toString() == entry
  }
}
