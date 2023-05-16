Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been defined:
      | method | path         | query   | response | response content | response body |
      | GET    | /basic       |         | 200      | application/json | basic.json    |
      | GET    | /with_params | a=1&b=2 | 200      |                  |               |


  Scenario: When all requests are made to the mock server
    When the mock server is started with interaction {1}
    And request {1} is made to the mock server
    Then a 200 success response is returned
    And the payload will contain the "basic" JSON document
    And the content type will be set as "application/json"
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request will be for a "GET"
    And the {first} interaction response will contain the "basic.json" document

  Scenario: When not all requests are made to the mock server
    When the mock server is started with interactions "1, 2"
    And request {1} is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interactions when done
    And the mock server status will be an expected but not received error for interaction {2}

  Scenario: When an unexpected request is made to the mock server
    When the mock server is started with interaction {1}
    And request {2} is made to the mock server
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interactions when done
    And the mock server status will be an unexpected request received error for interaction {2}

  Scenario: Request with query parameters
    When the mock server is started with interaction {2}
    And request {2} is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request query parameters will be "a=1&b=2"

  Scenario: Request with invalid query parameters
    When the mock server is started with interaction {2}
    And request {2} is made to the mock server with the following changes:
      | query   |
      | a=1&c=3 |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server status will be mismatches
    And the mismatches will contain a "query" mismatch with error "Expected query parameter 'b' but was missing"
    And the mismatches will contain a "query" mismatch with error "Unexpected query parameter 'c' received"
    And the mock server will NOT write out a Pact file for the interaction when done
