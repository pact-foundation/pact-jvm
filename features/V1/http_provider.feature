@provider
Feature: Basic HTTP provider
  Supports verifying a basic HTTP provider

  Background:
    Given the following HTTP interactions have been defined:
      | No | method | path          | query   | headers                 | body             | response | response headers    | response content | response body            |
      | 1  | GET    | /basic        |         |                         |                  | 200      |                     | application/json | file: basic.json         |
      | 2  | GET    | /with_params  | a=1&b=2 |                         |                  | 200      |                     |                  |                          |
      | 3  | GET    | /with_headers |         | 'X-TEST: Compatibility' |                  | 200      |                     |                  |                          |
      | 4  | PUT    | /basic        |         |                         | file: basic.json | 200      |                     |                  |                          |
      | 5  | GET    | /basic        |         |                         |                  | 200      | 'X-TEST: Something' | application/json | file: basic.json         |
      | 6  | GET    | /plain        |         |                         |                  | 200      |                     |                  | file: text-body.xml      |
      | 7  | GET    | /xml          |         |                         |                  | 200      |                     |                  | file: xml-body.xml       |
      | 8  | GET    | /bin          |         |                         |                  | 200      |                     |                  | file: rat.jpg            |
      | 9  | GET    | /form         |         |                         |                  | 200      |                     |                  | file: form-post-body.xml |
      | 10 | GET    | /multi        |         |                         |                  | 200      |                     |                  | file: multipart-body.xml |

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

  Scenario: Response with plain text body (positive case)
    Given a provider is started that returns the response from interaction 6
    And a Pact file for interaction 6 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with plain text body (negative case)
    Given a provider is started that returns the response from interaction 6, with the following changes:
      | body                       |
      | Hello Compatibility Suite! |
    And a Pact file for interaction 6 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error

  Scenario: Response with JSON body (positive case)
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with JSON body (negative case)
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | body                             |
      | JSON: { "one": 100, "two": "b" } |
    And a Pact file for interaction 1 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error

  Scenario: Response with XML body (positive case)
    Given a provider is started that returns the response from interaction 7
    And a Pact file for interaction 7 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with XML body (negative case)
    Given a provider is started that returns the response from interaction 7, with the following changes:
      | body                                                                      |
      | XML: <?xml version="1.0" encoding="UTF-8" ?><values><one>A</one></values> |
    And a Pact file for interaction 7 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error

  Scenario: Response with binary body (positive case)
    Given a provider is started that returns the response from interaction 8
    And a Pact file for interaction 8 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with binary body (negative case)
    Given a provider is started that returns the response from interaction 8, with the following changes:
      | body             |
      | file: spider.jpg |
    And a Pact file for interaction 8 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error

  Scenario: Response with form post body (positive case)
    Given a provider is started that returns the response from interaction 9
    And a Pact file for interaction 9 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with form post body (negative case)
    Given a provider is started that returns the response from interaction 9, with the following changes:
      | body             |
      | a=1&b=2&c=33&d=4 |
    And a Pact file for interaction 9 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error

  Scenario: Response with multipart body (positive case)
    Given a provider is started that returns the response from interaction 10
    And a Pact file for interaction 10 is to be verified
    When the verification is run
    Then the verification will be successful

  Scenario: Response with multipart body (negative case)
    Given a provider is started that returns the response from interaction 10, with the following changes:
      | body                      |
      | file: multipart2-body.xml |
    And a Pact file for interaction 10 is to be verified
    When the verification is run
    Then the verification will NOT be successful
    And the verification results will contain a "Body had differences" error
