Feature: V3 era Matching Rules

  Scenario: Supports an equality matcher to reset cascading rules
    Given an expected request configured with the following:
      | body               | matching rules                 |
      | file: 3-level.json | equality-matcher-reset-v3.json |
    And a request is received with the following:
      | body                                                                                                        |
      | JSON: { "one": { "a": { "ids": [100], "status": "Lovely" }  }, "two": [ { "ids": [1], "status": "BAD" } ] } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one.a.status" -> "Expected 'Lovely' (String) to be equal to 'OK' (String)"

  Scenario: Supports an include matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules          |
      | file: basic.json | include-matcher-v3.json |
    And a request is received with the following:
      | body                               |
      | JSON: { "one": "cat", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports an include matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules          |
      | file: basic.json | include-matcher-v3.json |
    And a request is received with the following:
      | body                               |
      | JSON: { "one": "dog", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected 'dog' to include 'a'"

  Scenario: Supports a minmax type matcher (positive case)
    Given an expected request configured with the following:
      | body               | matching rules              |
      | file: 3-level.json | minmax-type-matcher-v3.json |
    And a request is received with the following:
      | body                                                                                                        |
      | JSON: { "one": { "a": { "ids": [100], "status": "OK" }  }, "two": [ { "ids": [1,2,3], "status": "BAD" } ] } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a minmax type matcher (negative case)
    Given an expected request configured with the following:
      | body               | matching rules              |
      | file: 3-level.json | minmax-type-matcher-v3.json |
    And a request is received with the following:
      | body                                                                                                         |
      | JSON: { "one": { "a": { "ids": [], "status": "OK" }  }, "two": [ { "ids": [1,2,3,4,5], "status": "BAD" } ] } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one.a.ids" -> "Expected [] (size 0) to have minimum size of 1"
    And the mismatches will contain a mismatch with error "$.two[0].ids" -> "Expected [1, 2, 3, 4, 5] (size 5) to have maximum size of 4"

  Scenario: Supports a number type matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules              |
      | file: basic.json | number-type-matcher-v3.json |
    And the following requests are received:
      | body                                  | desc                            |
      | JSON: { "one": 100, "two": "b" }      | Integer number                  |
      | JSON: { "one": 100.01, "two": "b" }   | floating point number           |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a number type matcher where it is acceptable to coerce values from string form
    Given an expected request configured with the following:
      | query  | headers     | matching rules              |
      | a=1234 | 'X-A: 1234' | number-type-matcher-v3.json |
    And the following requests are received:
      | query   | headers      | desc                  |
      | a=100   | 'X-A: 100'   | Integer number        |
      | a=100.2 | 'X-A: 100.4' | Floating point number |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a number type matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules              |
      | file: basic.json | number-type-matcher-v3.json |
    And the following requests are received:
      | body                                  | desc                                              |
      | JSON: { "one": true, "two": "b" }     | Boolean                                           |
      | JSON: { "one": "100X01", "two": "b" } | String                                            |
      | JSON: { "one": "100", "two": "b" }    | Number in string form is not acceptable in bodies |
    When the requests are compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected true (Boolean) to be a number"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100X01' (String) to be a number"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100' (String) to be a number"

  Scenario: Supports an integer type matcher, no digits after the decimal point (positive case)
    Given an expected request configured with the following:
      | body             | matching rules               |
      | file: basic.json | integer-type-matcher-v3.json |
    And the following requests are received:
      | body                                  | desc                                |
      | JSON: { "one": 100, "two": "b" }      | Integer number                      |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports an integer type matcher where it is acceptable to coerce values from string form
    Given an expected request configured with the following:
      | query  | headers     | matching rules              |
      | a=1234 | 'X-A: 1234' | number-type-matcher-v3.json |
    And the following requests are received:
      | query   | headers      | desc                  |
      | a=100   | 'X-A: 100'   | Integer number        |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a integer type matcher, no digits after the decimal point (negative case)
    Given an expected request configured with the following:
      | body             | matching rules               |
      | file: basic.json | integer-type-matcher-v3.json |
    And the following requests are received:
      | body                                  | desc                                                            |
      | JSON: { "one": [], "two": "b" }       | Array                                                           |
      | JSON: { "one": 100.1, "two": "b" }    | Floating point number                                           |
      | JSON: { "one": "100X01", "two": "b" } | String                                                          |
      | JSON: { "one": "100", "two": "b" }    | String representation of an integer is not acceptable in bodies |
    When the requests are compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected [] (Array) to be an integer"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected 100.1 (Decimal) to be an integer"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100X01' (String) to be an integer"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100' (String) to be an integer"

  Scenario: Supports an decimal type matcher, must have significant digits after the decimal point (positive case)
    Given an expected request configured with the following:
      | body             | matching rules               |
      | file: basic.json | decimal-type-matcher-v3.json |
    And the following requests are received:
      | body                                    | desc                                             |
      | JSON: { "one": 100.1234, "two": "b" }   | Floating point number                            |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a decimal type matcher, must have significant digits after the decimal point (negative case)
    Given an expected request configured with the following:
      | body             | matching rules               |
      | file: basic.json | decimal-type-matcher-v3.json |
    And the following requests are received:
      | body                                    | desc                                                                         |
      | JSON: { "one": null, "two": "b" }       | Null                                                                         |
      | JSON: { "one": 100, "two": "b" }        | Integer number                                                               |
      | JSON: { "one": "100X01", "two": "b" }   | String value                                                                 |
      | JSON: { "one": "100.1234", "two": "b" } | String representation of a floating point number is not acceptable in bodies |
    When the requests are compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected null (Null) to be a decimal number"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected 100 (Integer) to be a decimal number"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100X01' (String) to be a decimal number"
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '100.1234' (String) to be a decimal number"

  Scenario: Supports a decimal type matcher where it is acceptable to coerce values from string form
    Given an expected request configured with the following:
      | query    | headers       | matching rules              |
      | a=1234.0 | 'X-A: 1234.0' | number-type-matcher-v3.json |
    And the following requests are received:
      | query   | headers      | desc                  |
      | a=100.2 | 'X-A: 100.4' | Floating point number |
    When the requests are compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a null matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules       |
      | file: basic.json | null-matcher-v3.json |
    And a request is received with the following:
      | body                              |
      | JSON: { "one": null, "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports an null matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules          |
      | file: basic.json | null-matcher-v3.json |
    And a request is received with the following:
      | body                            |
      | JSON: { "one": "", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '' (String) to be a null value"

  Scenario: Supports a Date and Time matcher (positive case)
    Given an expected request configured with the following:
      | body             | matching rules       |
      | file: basic.json | date-matcher-v3.json |
    And a request is received with the following:
      | body                                      |
      | JSON: { "one": "2023-07-19", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a Date and Time matcher (negative case)
    Given an expected request configured with the following:
      | body             | matching rules       |
      | file: basic.json | date-matcher-v3.json |
    And a request is received with the following:
      | body                                    |
      | JSON: { "one": "23/07/19", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '23/07/19' to match a date pattern of 'yyyy-MM-dd'"

  Scenario: Supports a Boolean matcher (positive case)
    Given an expected request configured with the following:
      | body                              | matching rules          |
      | JSON: { "one": true, "two": "b" } | boolean-matcher-v3.json |
    And a request is received with the following:
      | body                               |
      | JSON: { "one": false, "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a Boolean matcher (negative case)
    Given an expected request configured with the following:
      | body                              | matching rules          |
      | JSON: { "one": true, "two": "b" } | boolean-matcher-v3.json |
    And a request is received with the following:
      | body                            |
      | JSON: { "one": "", "two": "b" } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.one" -> "Expected '' (String) to match a boolean"

  Scenario: Supports a ContentType matcher (positive case)
    Given an expected request configured with the following:
      | content type             | body          | matching rules              |
      | application/octet-stream | file: rat.jpg | contenttype-matcher-v3.json |
    And a request is received with the following:
      | content type             | body             |
      | application/octet-stream | file: spider.jpg |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a ContentType matcher (negative case)
    Given an expected request configured with the following:
      | content type             | body          | matching rules              |
      | application/octet-stream | file: rat.jpg | contenttype-matcher-v3.json |
    And a request is received with the following:
      | content type             | body             |
      | application/octet-stream | file: sample.pdf |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$" -> "Expected binary contents to have content type 'image/jpeg' but detected contents was 'application/pdf'"

  Scenario: Supports a Values matcher (positive case, ignores missing and additional keys)
    Given an expected request configured with the following:
      | body             | matching rules         |
      | file: basic.json | values-matcher-v3.json |
    And a request is received with the following:
      | body                                                          |
      | JSON: { "one": "", "three": "b", "four": "c", "five": "100" } |
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Supports a Values matcher (negative case, final type is wrong)
    Given an expected request configured with the following:
      | body             | matching rules         |
      | file: basic.json | values-matcher-v3.json |
    And a request is received with the following:
      | body                                                       |
      | JSON: { "one": "", "two": "b", "three": "c", "four": 100 } |
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "$.four" -> "Expected 100 (Integer) to be the same type as 'a' (String)"
