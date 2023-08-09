Feature: General V4 features
  Supports general V4 features

  Scenario: Supports different types of interactions in the Pact file
    Given an HTTP interaction is being defined for a consumer test
    And a message interaction is being defined for a consumer test
    When the Pact file for the test is generated
    Then the first interaction in the Pact file will have a type of "Synchronous/HTTP"
    Then the second interaction in the Pact file will have a type of "Asynchronous/Messages"
