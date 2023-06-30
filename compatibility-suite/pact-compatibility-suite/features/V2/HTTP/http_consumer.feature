@consumer
Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been defined:
      | method | path      | query                | headers        | body             | matching rules               |
      | POST   | /path     |                      |                | file: basic.json | regex-matcher-v2.json        |
      | POST   | /path     |                      |                | file: basic.json | type-matcher-v2.json         |
      | GET    | /aaa/100/ |                      |                |                  | regex-matcher-path-v2.json   |
      | GET    | /path     | a=1&b=2&c=abc&d=true |                |                  | regex-matcher-query-v2.json  |
      | GET    | /path     |                      | 'X-Test: 1000' |                  | regex-matcher-header-v2.json |

  Scenario: Supports a regex matcher (negative case)
    When the mock server is started with interaction 1
    And request 1 is made to the mock server
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected 'a' to match '\w{3}\d{3}'"

  Scenario: Supports a regex matcher (positive case)
    When the mock server is started with interaction 1
    And request 1 is made to the mock server with the following changes:
      | body                                  |
      | JSON: { "one": "HHH123", "two": "b" } |
    Then a 200 success response is returned

  Scenario: Supports a type matcher (negative case)
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | body                             |
      | JSON: { "one": 100, "two": "b" } |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected 100 (Integer) to be the same type as 'a' (String)"

  Scenario: Supports a type matcher (positive case)
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | body                                  |
      | JSON: { "one": "HHH123", "two": "b" } |
    Then a 200 success response is returned

  Scenario: Supports a matcher for request paths
    When the mock server is started with interaction 3
    And request 3 is made to the mock server with the following changes:
      | path     |
      | /XYZ/123 |
    Then a 200 success response is returned

  Scenario: Supports matchers for request query parameters
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | query                  |
      | b=2&c=abc&d=true&a=999 |
    Then a 200 success response is returned

  Scenario: Supports matchers for repeated request query parameters (positive case)
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | query                         |
      | a=123&b=2&c=abc&d=true&a=9999 |
    Then a 200 success response is returned

  Scenario: Supports matchers for repeated request query parameters (negative case)
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | query                          |
      | a=123&b=2&c=abc&d=true&a=9999X |
    Then a 500 error response is returned
    And the mismatches will contain a "query" mismatch with error "Expected '9999X' to match '\d{1,4}'"

  Scenario: Supports matchers for request headers
    When the mock server is started with interaction 5
    And request 5 is made to the mock server with the following changes:
      | headers        |
      | 'X-Test: 1000' |
    Then a 200 success response is returned

  Scenario: Supports matchers for repeated request headers (positive case)
    When the mock server is started with interaction 5
    And request 5 is made to the mock server with the following changes:
      | raw headers                                    |
      | 'X-Test: 1000', 'X-Test: 1234', 'X-Test: 9999' |
    Then a 200 success response is returned

  Scenario: Supports matchers for repeated request headers (negative case)
    When the mock server is started with interaction 5
    And request 5 is made to the mock server with the following changes:
      | raw headers                                       |
      | 'X-Test: 1000', 'X-Test: 1234', 'X-Test: 9999ABC' |
    Then a 500 error response is returned
    And the mismatches will contain a "header" mismatch with error "Expected '9999ABC' to match '\d{1,4}'"

  Scenario: Supports matchers for request bodies
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | body                                  |
      | JSON: { "one": "c", "two": "b" } |
    Then a 200 success response is returned
