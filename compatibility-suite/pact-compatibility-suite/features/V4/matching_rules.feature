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
    And the response mismatches will contain a "status" mismatch with error "Expected status code 400 to be a Successful response (200–299)"

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
    And the mismatches will contain a mismatch with error "$.two" -> "Expected [] (Array) to not be empty"

  Scenario: Supports a not empty matcher (negative case 2, types are different)
    Given an expected request configured with the following:
      | body                               | matching rules           |
      | JSON: { "one": "a", "two": ["b"] } | notempty-matcher-v4.json |
    And a request is received with the following:
      | body                             |
      | JSON: { "one": "a", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.two" -> "Type mismatch: Expected 'b' (String) to be the same type as [\"b\"] (Array)"

  Scenario: Supports a not empty matcher with binary data (negative case)
    Given an expected request configured with the following:
      | body          | matching rules            |
      | file: rat.jpg | notempty2-matcher-v4.json |
    And a request is received with the following:
      | content type | body  |
      | image/jpeg   | EMPTY |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$" -> "Expected [] (0 bytes) to not be empty"

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
    And the mismatches will contain a mismatch with error "$.one" -> "'1.0' is not a valid semantic version"
    And the mismatches will contain a mismatch with error "$.two" -> "'1.0abc' is not a valid semantic version"

  Scenario: Supports an EachKey matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules          |
      | file: basic.json | eachkey-matcher-v4.json |
    And a request is received with the following:
      | body                                                        |
      | JSON: { "one": "a", "two": "b", "three": "c", "four": "d" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports an EachKey matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules         |
      | file: basic.json | eachkey-matcher-v4.json |
    And a request is received with the following:
      | body                                                       |
      | JSON: { "one": "a", "two": "b", "three": "c", "100": "d" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$[100]" -> "Expected '100' to match '[a-z]+"

  Scenario: Supports an EachValue matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules            |
      | file: basic.json | eachvalue-matcher-v4.json |
    And a request is received with the following:
      | body                                                         |
      | JSON: { "one": "a", "three": "b", "four": "c", "five": "d" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a EachValue matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules            |
      | file: basic.json | eachvalue-matcher-v4.json |
    And a request is received with the following:
      | body                                                         |
      | JSON: { "one": "", "two": "b", "three": "c", "four": "100" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.four" -> "Expected '100' to match '[a-z]+"

  Scenario: Supports an ArrayContains matcher (positive case)
    Given an expected request configured with the following:
      | content type               | body             | matching rules                |
      | application/vnd.siren+json | file: siren.json | arraycontains-matcher-v4.json |
    And a request is received with the following:
      | content type               | body              |
      | application/vnd.siren+json | file: siren2.json |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a ArrayContains matcher (negative case)
    Given an expected request configured with the following:
      | content type               | body             | matching rules                |
      | application/vnd.siren+json | file: siren.json | arraycontains-matcher-v4.json |
    And a request is received with the following:
      | content type               | body              |
      | application/vnd.siren+json | file: siren3.json |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.actions" -> "Variant at index 1 ({\"href\":\"http://api.x.io/orders/42/items\",\"method\":\"DELETE\",\"name\":\"delete-item\",\"title\":\"Delete Item\"}) was not found in the actual list"
