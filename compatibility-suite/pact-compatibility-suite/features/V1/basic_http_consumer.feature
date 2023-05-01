Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been setup:
      | method | path   | response | response content | response body |
      | GET    | /basic | 200      | application/json | basic.json    |


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
