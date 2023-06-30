@provider
Feature: Basic HTTP provider
  Supports verifying a basic HTTP provider

  Background:
    Given the following HTTP interactions have been defined:
      | method | path          | query   | headers                 | body             | response | response headers    | response content | response body    |
      | GET    | /basic        |         |                         |                  | 200      |                     | application/json | file: basic.json |
      | GET    | /with_params  | a=1&b=2 |                         |                  | 200      |                     |                  |                  |
      | GET    | /with_headers |         | 'X-TEST: Compatibility' |                  | 200      |                     |                  |                  |
      | PUT    | /basic        |         |                         | file: basic.json | 200      |                     |                  |                  |
      | GET    | /basic        |         |                         |                  | 200      | 'X-TEST: Something' | application/json | file: basic.json |

  Scenario: Verifying a simple HTTP request
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Verifying multiple Pact files
    Given a provider is started that returns the responses from interactions "1, 2"
    And a Pact file for interaction 1 is to be verified
    And a Pact file for interaction 2 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Incorrect request is made to provider
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 2 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Response status did not match" error

  Scenario: Verifying a simple HTTP request via a Pact broker
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified from a Pact broker
    When the verification is run
    Then the verification will be successful
    And a verification result will NOT be published back

  Scenario: Verifying a simple HTTP request via a Pact broker with publishing results enabled
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will be successful
    And a successful verification result will be published back for interaction {1}

  Scenario: Verifying multiple Pact files via a Pact broker
    Given a provider is started that returns the responses from interactions "1, 2"
    And a Pact file for interaction 1 is to be verified from a Pact broker
    And a Pact file for interaction 2 is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will be successful
    And a successful verification result will be published back for interaction {1}
    And a successful verification result will be published back for interaction {2}

  Scenario: Incorrect request is made to provider via a Pact broker
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 2 is to be verified from a Pact broker
    And publishing of verification results is enabled
    When the verification is run
    Then the verification will NOT be successful
    And a failed verification result will be published back for the interaction {2}

  Scenario: Verifying an interaction with a defined provider state
    Given a provider is started that returns the response from interaction 1
    And a provider state callback is configured
    And a Pact file for interaction 1 is to be verified with a provider state "state one" defined
    When the verification is run
    Then the provider state callback will be called before the verification is run
    And the provider state callback will receive a setup call with "state one" as the provider state parameter
    And the provider state callback will be called after the verification is run
    And the provider state callback will receive a teardown call "state one" as the provider state parameter

  Scenario: Verifying an interaction with no defined provider state
    Given a provider is started that returns the response from interaction 1
    And a provider state callback is configured
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the provider state callback will be called before the verification is run
    And the provider state callback will receive a setup call with "" as the provider state parameter
    And the provider state callback will be called after the verification is run
    And the provider state callback will receive a teardown call "" as the provider state parameter

  Scenario: Verifying an interaction where the provider state callback fails
    Given a provider is started that returns the response from interaction 1
    And a provider state callback is configured, but will return a failure
    And a Pact file for interaction 1 is to be verified with a provider state "state one" defined
    When the verification is run
    Then the provider state callback will be called before the verification is run
    And the verification will NOT be successful
    And the verification results will contain a "State change request failed" error
    And the provider state callback will NOT receive a teardown call

  Scenario: Verifying an interaction where a provider state callback is not configured
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified with a provider state "state one" defined
    When the verification is run
    Then the verification will be successful
    And a warning will be displayed that there was no provider state callback configured for provider state "state one"

  Scenario: Verifying a HTTP request with a request filter configured
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified
    And a request filter is configured to make the following changes:
      | headers |
      | 'A: 1'  |
    When the verification is run
    Then the verification will be successful
    And the request to the provider will contain the header "A: 1"

  Scenario: Verifies the response status code
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | status |
      | 400    |
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Response status did not match" error

  Scenario: Verifies the response headers
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | headers                 |
      | 'X-TEST: Compatibility' |
    And a Pact file for interaction 5 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Headers had differences" error

  Scenario: Verifies the response body
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | body                             |
      | JSON: { "one": 100, "two": "b" } |
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error
