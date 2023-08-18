Feature: General V4 features
  Supports general V4 features

  Scenario: Supports different types of interactions in the Pact file
    Given an HTTP interaction is being defined for a consumer test
    And a message interaction is being defined for a consumer test
    When the Pact file for the test is generated
    Then there will be an interaction in the Pact file with a type of "Synchronous/HTTP"
    And there will be an interaction in the Pact file with a type of "Asynchronous/Messages"
