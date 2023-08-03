@consumer
Feature: HTTP consumer
  Supports V3 HTTP consumer interactions

  Scenario: Supports specifying multiple provider states
    Given an integration is being defined for a consumer test
    And a provider state "state one" is specified
    And a provider state "state two" is specified
    When the Pact file for the test is generated
    Then the interaction in the Pact file will contain 2 provider states
    And the interaction in the Pact file will contain provider state "state one"
    And the interaction in the Pact file will contain provider state "state two"

  Scenario: Supports data for provider states
    Given an integration is being defined for a consumer test
    And a provider state "a user exists" is specified with the following data:
      | username | name       | age |
      | "Test"   | "Test Guy" | 66  |
    When the Pact file for the test is generated
    Then the interaction in the Pact file will contain 1 provider state
    And the interaction in the Pact file will contain provider state "a user exists"
    And the provider state "a user exists" in the Pact file will contain the following parameters:
      | parameters                                     |
      | {"age":66,"name":"Test Guy","username":"Test"} |

