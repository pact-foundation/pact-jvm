package steps.v4

import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class SharedV4PactData {
  String scenarioId
  PactBuilder pactBuilder = new PactBuilder('V4 consumer', 'V4 provider', PactSpecVersion.V4)
  List<Closure> builderCallbacks = []
  V4Pact pact
  String pactJsonStr
  JsonValue.Object pactJson

  @SuppressWarnings('UnnecessaryConstructor')
  SharedV4PactData() { }
}

@ParameterType('first|second|third')
@SuppressWarnings(['SpaceAfterOpeningBrace'])
static Integer numType(String numType) {
  switch (numType) {
    case 'first' -> yield 0
    case 'second'-> yield 1
    case 'third' -> yield 2
    default -> throw new IllegalArgumentException("$numType is not a valid number type")
  }
}

class SharedV4Steps {
  SharedV4PactData sharedV4PactData

  SharedV4Steps(SharedV4PactData sharedV4PactData) {
    this.sharedV4PactData = sharedV4PactData
  }

  @When('the Pact file for the test is generated')
  void the_pact_file_for_the_test_is_generated() {
    sharedV4PactData.builderCallbacks.forEach {
      sharedV4PactData.pactBuilder.interactions.add(it.call())
    }
    sharedV4PactData.pact = sharedV4PactData.pactBuilder.toPact()
    sharedV4PactData.pactJsonStr = Json.INSTANCE.prettyPrint(sharedV4PactData.pact.toMap(PactSpecVersion.V4))
    sharedV4PactData.pactJson = JsonParser.parseString(sharedV4PactData.pactJsonStr).asObject()
  }

  @Then('the {numType} interaction in the Pact file will have a type of {string}')
  void the_interaction_in_the_pact_file_will_have_a_type_of(Integer index, String type) {
    JsonValue.Array interactions = sharedV4PactData.pactJson['interactions'].asArray()
    assert interactions.get(index)['type'] == type
  }

  @Then('the {numType} interaction in the Pact file will have {string} = {string}')
  void the_first_interaction_in_the_pact_file_will_have(Integer index, String name, String value) {
    JsonValue.Array interactions = sharedV4PactData.pactJson['interactions'].asArray()
    def json = JsonParser.parseString(value)
    assert interactions.get(index)[name] == json
  }

  @Then('there will be an interaction in the Pact file with a type of {string}')
  void there_will_be_an_interaction_in_the_pact_file_with_a_type_of(String type) {
    JsonValue.Array interactions = sharedV4PactData.pactJson['interactions'].asArray()
    assert interactions.values.find { it['type'] == type } != null
  }
}
