@provider @message
Feature: Message provider
  Supports verifying a async message provider using V4 features

  Scenario: Verifying a pending message interaction
    Given a provider is started that can generate the "basic" message with "file: basic2.json"
    And a Pact file for "basic":"file: basic.json" is to be verified, but is marked pending
    When the verification is run
    Then the verification will be successful
    And there will be a pending "Body had differences" error

  Scenario: Verifying a message interaction with comments
    Given a provider is started that can generate the "basic" message with "file: basic.json"
    And a Pact file for "basic":"file: basic.json" is to be verified with the following comments:
      | comment             | type     |
      | comment one         | text     |
      | comment two         | text     |
      | compatibility-suite | testname |
    When the verification is run
    Then the comment "comment one" will have been printed to the console
    And the comment "comment two" will have been printed to the console
    And the "compatibility-suite" will displayed as the original test name
