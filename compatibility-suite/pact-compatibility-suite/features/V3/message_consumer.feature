@consumer
Feature: Message consumer
  Supports V3 message consumer interactions

  Scenario: When all messages are successfully processed
    Given a message integration is being defined for a consumer test
    And the message payload contains the "basic" JSON document
    When the message is successfully processed
    Then the received message payload will contain the "basic" JSON document
    And the received message content type will be "application/json"
    And the consumer test will have passed
    And a Pact file for the message interaction will have been written
    And the pact file will contain 1 message interaction
    And the first message in the pact file will contain the "basic.json" document
    And the first message in the pact file content type will be "application/json"

  Scenario: When not all messages are successfully processed
    Given a message integration is being defined for a consumer test
    And the message payload contains the "basic" JSON document
    When the message is NOT successfully processed with a "Test failed" exception
    Then the consumer test will have failed
    And the consume test error will be "Test failed"
    And a Pact file for the message interaction will NOT have been written

  Scenario: Supports arbitrary message metadata
    Given a message integration is being defined for a consumer test
    And the message payload contains the "basic" JSON document
    And the message contains the following metadata:
      | key     | value                                           |
      | Origin  | Some Text                                       |
      | TagData | JSON: { "ID": "sjhdjkshsdjh", "weight": 100.5 } |
    When the message is successfully processed
    Then the received message metadata will contain "Origin" == "Some Text"
    And the received message metadata will contain "TagData" == "JSON: { \"ID\": \"sjhdjkshsdjh\", \"weight\": 100.5 }"
    And a Pact file for the message interaction will have been written
    And the first message in the pact file will contain the message metadata "Origin" == "Some Text"
    And the first message in the pact file will contain the message metadata "TagData" == "JSON: { \"ID\": \"sjhdjkshsdjh\", \"weight\": 100.5 }"

  Scenario: Supports specifying provider states
    Given a message integration is being defined for a consumer test
    And a provider state "state one" for the message is specified
    And a provider state "state two" for the message is specified
    And a message is defined
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the first message in the pact file will contain 2 provider states
    And the first message in the Pact file will contain provider state "state one"
    And the first message in the Pact file will contain provider state "state two"

  Scenario: Supports data for provider states
    Given a message integration is being defined for a consumer test
    And a provider state "a user exists" for the message is specified with the following data:
      | username | name       | age |
      | "Test"   | "Test Guy" | 66  |
    And a message is defined
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the first message in the pact file will contain 1 provider state
    And the provider state "a user exists" for the message will contain the following parameters:
      | parameters                                     |
      | {"age":66,"name":"Test Guy","username":"Test"} |

  Scenario: Supports the use of generators with the message body
    Given a message integration is being defined for a consumer test
    And the message configured with the following:
      | body             | generators               |
      | file: basic.json | randomint-generator.json |
    When the message is successfully processed
    Then the message contents for "$.one" will have been replaced with an "integer"

  Scenario: Supports the use of generators with message metadata
    Given a message integration is being defined for a consumer test
    And the message configured with the following:
      | generators                                                                      | metadata                                  |
      | JSON: { "metadata": { "ID": { "type": "RandomInt", "min": 0,  "max": 1000 } } } | { "ID": "sjhdjkshsdjh", "weight": 100.5 } |
    When the message is successfully processed
    Then the received message metadata will contain "weight" == "JSON: 100.5"
    And the received message metadata will contain "ID" replaced with an "integer"
