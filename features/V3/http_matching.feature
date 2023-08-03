Feature: Matching HTTP parts (request or response)

  Scenario: Comparing content type headers which are equal
    Given an expected request with a "content-type" header of "application/json"
    And a request is received with a "content-type" header of "application/json"
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Comparing content type headers where they have the same charset
    Given an expected request with a "content-type" header of "application/json;charset=UTF-8"
    And a request is received with a "content-type" header of "application/json;charset=utf-8"
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Comparing content type headers where the actual has a charset
    Given an expected request with a "content-type" header of "application/json"
    And a request is received with a "content-type" header of "application/json;charset=UTF-8"
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Comparing content type headers where the actual is missing a charset
    Given an expected request with a "content-type" header of "application/json;charset=UTF-8"
    And a request is received with a "content-type" header of "application/json"
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "content-type" -> "Expected header 'content-type' to have value 'application/json;\s*charset=UTF-8' but was 'application/json'"

  Scenario: Comparing content type headers where the actual has a different charset
    Given an expected request with a "content-type" header of "application/json;charset=UTF-16"
    And a request is received with a "content-type" header of "application/json;charset=UTF-8"
    When the request is compared to the expected one
    Then the comparison should NOT be OK
    And the mismatches will contain a mismatch with error "content-type" -> "Expected header 'content-type' to have value 'application/json;\s*charset=UTF-16' but was 'application/json;\s*charset=UTF-8'"

  Scenario: Comparing accept headers where the actual has additional parameters
    Given an expected request with an "accept" header of "text/html, application/xhtml+xml, application/xml, image/webp, */*"
    And a request is received with an "accept" header of "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8"
    When the request is compared to the expected one
    Then the comparison should be OK

  Scenario: Comparing accept headers where the actual has is missing a value
    Given an expected request with an "accept" header of "text/html, application/xhtml+xml, application/xml, image/webp, */*"
    And a request is received with an "accept" header of "text/html, application/xml;q=0.9, image/webp, */*;q=0.8"
    When the request is compared to the expected one
    Then the comparison should NOT be OK
