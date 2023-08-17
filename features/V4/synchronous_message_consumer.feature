@message @SynchronousMessage
Feature: Synchronous Message consumer
  Supports V4 synchronous message consumer interactions

  Scenario: Sets the type for the interaction
    Given a synchronous message interaction is being defined for a consumer test
    When the Pact file for the test is generated
    Then the first interaction in the Pact file will have a type of "Synchronous/Messages"

  Scenario: Supports specifying a key for the interaction
    Given a synchronous message interaction is being defined for a consumer test
    And a key of "123ABC" is specified for the synchronous message interaction
    When the Pact file for the test is generated
    Then the first interaction in the Pact file will have "key" = '"123ABC"'

  Scenario: Supports specifying the interaction is pending
    Given a synchronous message interaction is being defined for a consumer test
    And the synchronous message interaction is marked as pending
    When the Pact file for the test is generated
    Then the first interaction in the Pact file will have "pending" = 'true'

  Scenario: Supports adding comments
    Given a synchronous message interaction is being defined for a consumer test
    And a comment "this is a comment" is added to the synchronous message interaction
    When the Pact file for the test is generated
    Then the first interaction in the Pact file will have "comments" = '{"text":["this is a comment"]}'

  Scenario: When all messages are successfully processed
    Given a synchronous message interaction is being defined for a consumer test
    And the message request payload contains the "basic" JSON document
    And the message response payload contains the "file: xml-body.xml" document
    When the message is successfully processed
    Then the received message payload will contain the "file: xml-body.xml" document
    And the received message content type will be "application/xml"
    And the consumer test will have passed
    And a Pact file for the message interaction will have been written
    And the pact file will contain 1 interaction
    And the first interaction in the pact file will contain the "file: basic.json" document as the request
    And the first interaction in the pact file request content type will be "application/json"
    And the first interaction in the pact file will contain the "file: xml-body.xml" document as a response
    And the first interaction in the pact file response content type will be "application/xml"

  Scenario: Supports multiple responses to a request message
    Given a synchronous message interaction is being defined for a consumer test
    And the message response payload contains the "file: basic.json" document
    And the message response payload contains the "file: xml-body.xml" document
    When the Pact file for the test is generated
    Then the first interaction in the pact file will contain 2 response messages
    And the first interaction in the pact file will contain the "file: basic.json" document as the first response message
    And the first interaction in the pact file will contain the "file: xml-body.xml" document as the second response message

  Scenario: Supports arbitrary message metadata
    Given a synchronous message interaction is being defined for a consumer test
    And the message request contains the following metadata:
      | key     | value                                           |
      | Origin  | Some Text                                       |
      | TagData | JSON: { "ID": "sjhdjkshsdjh", "weight": 100.5 } |
    When the message is successfully processed
    Then the received message request metadata will contain "Origin" == "Some Text"
    And the received message request metadata will contain "TagData" == "JSON: { \"ID\": \"sjhdjkshsdjh\", \"weight\": 100.5 }"
    And a Pact file for the message interaction will have been written
    And the first message in the pact file will contain the request message metadata "Origin" == "Some Text"
    And the first message in the pact file will contain the request message metadata "TagData" == "JSON: { \"ID\": \"sjhdjkshsdjh\", \"weight\": 100.5 }"

  Scenario: Supports specifying provider states
    Given a synchronous message interaction is being defined for a consumer test
    And a provider state "state one" for the synchronous message is specified
    And a provider state "state two" for the synchronous message is specified
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the first message in the pact file will contain 2 provider states
    And the first message in the Pact file will contain provider state "state one"
    And the first message in the Pact file will contain provider state "state two"

  Scenario: Supports data for provider states
    Given a synchronous message interaction is being defined for a consumer test
    And a provider state "a user exists" for the synchronous message is specified with the following data:
      | username | name       | age |
      | "Test"   | "Test Guy" | 66  |
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the first message in the pact file will contain 1 provider state
    And the provider state "a user exists" for the message will contain the following parameters:
      | parameters                                     |
      | {"age":66,"name":"Test Guy","username":"Test"} |

  Scenario: Supports the use of generators with the message bodies
    Given a synchronous message interaction is being defined for a consumer test
    And the message request is configured with the following:
      | body             | generators               |
      | file: basic.json | randomint-generator.json |
    And the message response is configured with the following:
      | body             | generators               |
      | file: basic.json | randomint-generator.json |
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the message request contents for "$.one" will have been replaced with an "integer"
    And the message response contents for "$.one" will have been replaced with an "integer"

  Scenario: Supports the use of generators with message metadata
    Given a synchronous message interaction is being defined for a consumer test
    And the message request is configured with the following:
      | generators                                                                      | metadata                                  |
      | JSON: { "metadata": { "ID": { "type": "RandomInt", "min": 0,  "max": 1000 } } } | { "ID": "sjhdjkshsdjh", "weight": 100.5 } |
    And the message response is configured with the following:
      | generators                                                                      | metadata                                  |
      | JSON: { "metadata": { "ID": { "type": "RandomInt", "min": 0,  "max": 1000 } } } | { "ID": "sjhdjkshsdjh", "weight": 100.5 } |
    When the message is successfully processed
    Then a Pact file for the message interaction will have been written
    And the received message request metadata will contain "weight" == "JSON: 100.5"
    And the received message request metadata will contain "ID" replaced with an "integer"
    And the received message response metadata will contain "weight" == "JSON: 100.5"
    And the received message response metadata will contain "ID" replaced with an "integer"
