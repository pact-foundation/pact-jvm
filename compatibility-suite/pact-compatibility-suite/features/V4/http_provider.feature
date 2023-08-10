@provider
Feature: HTTP provider
  Supports verifying a HTTP provider using V4 features

  Background:
    Given the following HTTP interactions have been defined:
      | No | method | path   | query | headers | body | response | response headers | response content | response body    |
      | 1  | GET    | /basic |       |         |      | 200      |                  | application/json | file: basic.json |

  Scenario: Verifying a pending HTTP interaction
    Given a provider is started that returns the response from interaction 1, with the following changes:
      | body              |
      | file: basic2.json |
    And a Pact file for interaction 1 is to be verified, but is marked pending
    When the verification is run
    Then the verification will be successful
    And there will be a pending "Body had differences" error

  Scenario: Verifying a HTTP interaction with comments
    Given a provider is started that returns the response from interaction 1
    And a Pact file for interaction 1 is to be verified with the following comments:
      | comment             | type     |
      | comment one         | text     |
      | comment two         | text     |
      | compatibility-suite | testname |
    When the verification is run
    Then the comment "comment one" will have been printed to the console
    And the comment "comment two" will have been printed to the console
    And the "compatibility-suite" will displayed as the original test name
