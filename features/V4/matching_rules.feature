Feature: V4 era Matching Rules

  Scenario: Supports a status code matcher (positive case)
    Given an expected response configured with the following:
      | status | matching rules             |
      | 200    | statuscode-matcher-v4.json |
    And a status 299 response is received
    When the response is compared to the expected one
    Then the response comparison should be OK

  Scenario: Supports a status code matcher (negative case)
    Given an expected response configured with the following:
      | status | matching rules             |
      | 200    | statuscode-matcher-v4.json |
    And a status 400 response is received
    When the response is compared to the expected one
    Then the response comparison should NOT be OK
    And the response mismatches will contain a "status" mismatch with error "expected Successful response (200–299) but was 400"

  Scenario: Supports a not empty matcher (positive case)
    Given an expected request configured with the following:
      | body                              | matching rules           |
      | JSON: { "one": "", "two": ["b"] } | notempty-matcher-v4.json |
    And a request is received with the following:
      | body                                   |
      | JSON: { "one": "cat", "two": ["rat"] } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a not empty matcher with binary data (positive case)
    Given an expected request configured with the following:
      | body          | matching rules            |
      | file: rat.jpg | notempty2-matcher-v4.json |
    And a request is received with the following:
      | body             |
      | file: spider.jpg |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a not empty matcher (negative case)
    Given an expected request configured with the following:
      | body                               | matching rules           |
      | JSON: { "one": "a", "two": ["b"] } | notempty-matcher-v4.json |
    And a request is received with the following:
      | body                           |
      | JSON: { "one": "", "two": [] } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '' (String) to not be empty"
    And the mismatches will contain a mismatch with error "$.two" -> "Expected [] (ArrayList) to not be empty"

  Scenario: Supports a not empty matcher (negative case 2, types are different)
    Given an expected request configured with the following:
      | body                               | matching rules           |
      | JSON: { "one": "a", "two": ["b"] } | notempty-matcher-v4.json |
    And a request is received with the following:
      | body                             |
      | JSON: { "one": "a", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.two" -> "Type mismatch: Expected String 'b' to be equal to List [\"b\"]"

  Scenario: Supports a not empty matcher with binary data (negative case)
    Given an expected request configured with the following:
      | body          | matching rules            |
      | file: rat.jpg | notempty2-matcher-v4.json |
    And a request is received with the following:
      | body |
      |      |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$" -> "Expected 0 byte(s) (byte[]) to not be empty"

  Scenario: Supports a semver matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules         |
      | file: basic.json | semver-matcher-v4.json |
    And a request is received with the following:
      | body                                     |
      | JSON: { "one": "1.0.0", "two": "2.0.0" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a semver matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules         |
      | file: basic.json | semver-matcher-v4.json |
    And a request is received with the following:
      | body                           |
      | JSON: { "one": "1.0", "two": "1.0abc" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '1.0' (String) to be a semantic version"
    And the mismatches will contain a mismatch with error "$.two" -> "Expected '1.0abc' (String) to be a semantic version"
