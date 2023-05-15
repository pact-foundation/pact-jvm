Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been setup:
      | method | path   | query | response | response content | response body |
      | GET    | /basic | | 200      | application/json | basic.json    |
      | GET    | /with_params | a=1&b=2 | 200      | application/json | basic.json    |


  Scenario: Simple GET request for a JSON document
    When the mock server is started
    And request {1} is made to the mock server
    Then a 200 success response is returned
    And the payload will contain the {basic} JSON document
    And the content type will be set as {application/json}
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {one} interaction
    And the interaction request will be for a {GET}
    And the interaction response will contain the {basic} document

  Scenario: Request with query parameters
    When the mock server is started
    And request {2} is made to the mock server
    Then a 200 success response is returned
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {one} interaction
    And the interaction request query parameters will be {a=1&b=2}

  Scenario: Request with invalid query parameters
    When the mock server is started
    And request {2} is made to the mock server with the following changes:
      | query   |
      | a=1&c=3 |
    Then a 500 error response is returned
    And the mock server errors will contain {Expected query parameter b}
    And the mock server will NOT write out a Pact file for the interaction when done
