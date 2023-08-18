Feature: V3 era Generators applied to HTTP parts

  Scenario: Supports using a generator with the request path
    Given a request configured with the following generators:
      | generators                                                                 |
      | JSON: { "path": { "type": "ProviderState", "expression": "/path/${id}" } } |
    And the generator test mode is set as "Provider"
    When the request is prepared for use with a "providerState" context:
      | { "id": 1000 } |
    Then the request "path" will be set as "/path/1000"

  Scenario: Supports using a generator with the request headers
    Given a request configured with the following generators:
      | generators                                                                     |
      | JSON: { "header": { "X-TEST": { "type": "RandomInt", "min": 1, "max": 10 } } } |
    When the request is prepared for use
    Then the request "header[X-TEST]" will match "\d+"

  Scenario: Supports using a generator with the request query parameters
    Given a request configured with the following generators:
      | generators                                                                |
      | JSON: { "query": { "v1": { "type": "RandomInt", "min": 1, "max": 10 } } } |
    When the request is prepared for use
    Then the request "queryParameter[v1]" will match "\d+"

  Scenario: Supports using a generator with the request body
    Given a request configured with the following generators:
      | body             | generators               |
      | file: basic.json | randomint-generator.json |
    When the request is prepared for use
    Then the body value for "$.one" will have been replaced with an "integer"

  Scenario: Supports using a generator with the response status
    Given a response configured with the following generators:
      | generators                                                           |
      | JSON: { "status": { "type": "RandomInt", "min": 201, "max": 599 } }  |
    When the response is prepared for use
    Then the response "status" will not be "200"
    Then the response "status" will match "\d+"

  Scenario: Supports using a generator with the response headers
    Given a response configured with the following generators:
      | generators                                                                     |
      | JSON: { "header": { "X-TEST": { "type": "RandomInt", "min": 1, "max": 10 } } } |
    When the response is prepared for use
    Then the response "header[X-TEST]" will match "\d+"

  Scenario: Supports using a generator with the response body
    Given a response configured with the following generators:
      | body             | generators               |
      | file: basic.json | randomint-generator.json |
    When the response is prepared for use
    Then the body value for "$.one" will have been replaced with a "integer"
