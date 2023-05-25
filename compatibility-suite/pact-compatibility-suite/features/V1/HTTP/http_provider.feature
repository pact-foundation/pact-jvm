@provider
Feature: Basic HTTP provider
  Supports verifying a basic HTTP provider

  Background:
    Given the following HTTP interactions have been defined:
      | method | path          | query   | headers                 | body       | response | response content | response body |
      | GET    | /basic        |         |                         |            | 200      | application/json | basic.json    |
      | GET    | /with_params  | a=1&b=2 |                         |            | 200      |                  |               |
      | GET    | /with_headers |         | 'X-TEST: Compatibility' |            | 200      |                  |               |
      | PUT    | /basic        |         |                         | basic.json | 200      |                  |               |

  Scenario: Verifying a simple HTTP request
    Given a provider is started that returns the response from interaction {1}
    And a Pact file for interaction {1} is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Verifying multiple Pact files
    Given a provider is started that returns the responses from interactions "1, 2"
    And a Pact file for interaction {1} is to be verified
    And a Pact file for interaction {2} is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Incorrect request is made to provider
    Given a provider is started that returns the response from interaction {1}
    And a Pact file for interaction {2} is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Response status did not match" error

  Scenario: Verifying a simple HTTP request via a Pact broker
    Given a provider is started that returns the response from interaction {1}
    And a Pact file for interaction {1} is to be verified from a Pact broker
    When the verification is run
    Then the verification will be successful
    And a verification result will NOT be published back

  Scenario: Verifying a simple HTTP request via a Pact broker with publishing results enabled
    Given a provider is started that returns the response from interaction {1}
    And a Pact file for interaction {1} is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will be successful
    And a successful verification result will be published back for interaction {1}

  Scenario: Verifying multiple Pact files via a Pact broker
    Given a provider is started that returns the responses from interactions "1, 2"
    And a Pact file for interaction {1} is to be verified from a Pact broker
    And a Pact file for interaction {2} is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will be successful
    And a successful verification result will be published back for interaction {1}
    And a successful verification result will be published back for interaction {2}

  Scenario: Incorrect request is made to provider via a Pact broker
    Given a provider is started that returns the response from interaction {1}
    And a Pact file for interaction {2} is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will NOT be successful
    And a failed verification result will be published back for the interaction {2}
