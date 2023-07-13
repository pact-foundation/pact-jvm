@provider
Feature: Basic HTTP provider
  Supports verifying a basic HTTP provider

  Background:
    Given the following HTTP interactions have been defined:
      | No | method | path | response | response headers | response content | response body    | response matching rules      |
      | 1  | GET    | /one | 200      | 'X-TEST: 1'      | application/json | file: basic.json | regex-matcher-header-v2.json |
      | 2  | GET    | /two | 200      |                  | application/json | file: basic.json | type-matcher-v2.json         |

  Scenario: Supports matching rules for the response headers (positive case)
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | headers        |
      | 'X-TEST: 1000' |
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Supports matching rules for the response headers (negative case)
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | headers          |
      | 'X-TEST: 123ABC' |
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Headers had differences" error

  Scenario: Verifies the response body (positive case)
    Given a provider is started that returns the response from interaction 2, with the following changes:
      | body                             |
      | JSON: { "one": "100", "two": "b" } |
    And a Pact file for interaction 2 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Verifies the response body (negative case)
    Given a provider is started that returns the response from interaction 2, with the following changes:
      | body                             |
      | JSON: { "one": 100, "two": "b" } |
    And a Pact file for interaction 2 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error
