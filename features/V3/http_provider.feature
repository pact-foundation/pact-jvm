@provider
Feature: HTTP provider
  Supports verifying a HTTP provider using V3 features

  Background:
    Given the following HTTP interactions have been defined:
      | No | method | path | response | response headers | response content | response body    | response matching rules      |
      | 1  | GET    | /one | 200      | 'X-TEST: 1'      | application/json | file: basic.json | regex-matcher-header-v2.json |

  Scenario: Verifying an interaction with multiple defined provider states
    Given a provider is started that returns the response from interaction 1
    And a provider state callback is configured
    And a Pact file for interaction 1 is to be verified with the following provider states defined:
      | State Name |
      | State One  |
      | State Two  |
    When the verification is run
    Then the provider state callback will be called before the verification is run
    And the provider state callback will receive a setup call with "State One" as the provider state parameter
    And the provider state callback will receive a setup call with "State Two" as the provider state parameter
    And the provider state callback will be called after the verification is run
    And the provider state callback will receive a teardown call "State One" as the provider state parameter
    And the provider state callback will receive a teardown call "State Two" as the provider state parameter

  Scenario: Verifying an interaction with a provider state with parameters
    Given a provider is started that returns the response from interaction 1
    And a provider state callback is configured
    And a Pact file for interaction 1 is to be verified with the following provider states defined:
      | State Name     | Parameters                   |
      | A user exists  | { "name": "Bob", "age": 22 } |
    When the verification is run
    Then the provider state callback will be called before the verification is run
    And the provider state callback will receive a setup call with "A user exists" and the following parameters:
      | name  | age |
      | "Bob" | 22  |
    And the provider state callback will be called after the verification is run
    And the provider state callback will receive a teardown call "A user exists" and the following parameters:
      | name  | age |
      | "Bob" | 22  |
