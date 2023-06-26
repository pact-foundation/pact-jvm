@consumer
Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been defined:
      | method | path  | query | headers | body       | matching rules | response | response content | response body |
      | POST   | /path |       |         | basic.json | regex.json     | 200      |                  |               |
      | POST   | /path |       |         | basic.json | type.json      | 200      |                  |               |

  Scenario: Supports a regex matcher
    When the mock server is started with interaction 1
    And request 1 is made to the mock server
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected \"a\" to match '\w{3}\d{3}'"

  Scenario: Supports a regex matcher (positive case)
    When the mock server is started with interaction 1
    And request 1 is made to the mock server with the following changes:
      | body                                  |
      | JSON: { "one": "HHH123", "two": "b" } |
    Then a 200 success response is returned

  Scenario: Supports a type matcher
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | body                             |
      | JSON: { "one": 100, "two": "b" } |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected 100 (Integer) to be the same type as \"a\" (String)"

  Scenario: Supports a type matcher (positive case)
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | body                                  |
      | JSON: { "one": "HHH123", "two": "b" } |
    Then a 200 success response is returned
