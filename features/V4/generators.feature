Feature: V4 era Generators

  Scenario: Supports a Provider State generator
    Given a request configured with the following generators:
      | body             | generators                   |
      | file: basic.json | providerstate-generator.json |
    And the generator test mode is set as "Provider"
    When the request is prepared for use with a "providerState" context:
      | { "id": 1000 } |
    Then the body value for "$.one" will have been replaced with "1000"

  Scenario: Supports a Mock server URL generator
    Given a request configured with the following generators:
      | body             | generators                   |
      | file: basic.json | mockserver-generator.json |
    And the generator test mode is set as "Consumer"
    When the request is prepared for use with a "mockServer" context:
      | { "href": "http://somewhere.world" } |
    Then the body value for "$.one" will have been replaced with "http://somewhere.world/a"

  Scenario: Supports a simple UUID generator
    Given a request configured with the following generators:
      | body             | generators                 |
      | file: basic.json | uuid-generator-simple.json |
    When the request is prepared for use
    Then the body value for "$.one" will have been replaced with a "simple UUID"

  Scenario: Supports a lower-case-hyphenated UUID generator
    Given a request configured with the following generators:
      | body             | generators                                |
      | file: basic.json | uuid-generator-lower-case-hyphenated.json |
    When the request is prepared for use
    Then the body value for "$.one" will have been replaced with a "lower-case-hyphenated UUID"

  Scenario: Supports a upper-case-hyphenated UUID generator
    Given a request configured with the following generators:
      | body             | generators                                |
      | file: basic.json | uuid-generator-upper-case-hyphenated.json |
    When the request is prepared for use
    Then the body value for "$.one" will have been replaced with a "upper-case-hyphenated UUID"

  Scenario: Supports a URN UUID generator
    Given a request configured with the following generators:
      | body             | generators                                |
      | file: basic.json | uuid-generator-urn.json |
    When the request is prepared for use
    Then the body value for "$.one" will have been replaced with a "URN UUID"
