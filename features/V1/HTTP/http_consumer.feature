@consumer
Feature: Basic HTTP consumer
  Supports basic HTTP consumer interactions

  Background:
    Given the following HTTP interactions have been defined:
      | method | path          | query   | headers                 | body                     | response | response content | response body    |
      | GET    | /basic        |         |                         |                          | 200      | application/json | file: basic.json |
      | GET    | /with_params  | a=1&b=2 |                         |                          | 200      |                  |                  |
      | GET    | /with_headers |         | 'X-TEST: Compatibility' |                          | 200      |                  |                  |
      | PUT    | /basic        |         |                         | file: basic.json         | 200      |                  |                  |
      | PUT    | /plain        |         |                         | file: text-body.xml      | 200      |                  |                  |
      | PUT    | /xml          |         |                         | file: xml-body.xml       | 200      |                  |                  |
      | PUT    | /bin          |         |                         | file: rat.jpg            | 200      |                  |                  |
      | PUT    | /form         |         |                         | file: form-post-body.xml | 200      |                  |                  |

  Scenario: When all requests are made to the mock server
    When the mock server is started with interaction 1
    And request 1 is made to the mock server
    Then a 200 success response is returned
    And the payload will contain the "basic" JSON document
    And the content type will be set as "application/json"
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request will be for a "GET"
    And the {first} interaction response will contain the "basic.json" document

  Scenario: When not all requests are made to the mock server
    When the mock server is started with interactions "1, 2"
    And request 1 is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interactions when done
    And the mock server status will be an expected but not received error for interaction {2}

  Scenario: When an unexpected request is made to the mock server
    When the mock server is started with interaction 1
    And request 2 is made to the mock server
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interactions when done
    And the mock server status will be an unexpected "GET" request received error for interaction {2}

  Scenario: Request with query parameters
    When the mock server is started with interaction 2
    And request 2 is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request query parameters will be "a=1&b=2"

  Scenario: Request with invalid query parameters
    When the mock server is started with interaction 2
    And request 2 is made to the mock server with the following changes:
      | query   |
      | a=1&c=3 |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server status will be mismatches
    And the mismatches will contain a "query" mismatch with error "Expected query parameter 'b' but was missing"
    And the mismatches will contain a "query" mismatch with error "Unexpected query parameter 'c' received"
    And the mock server will NOT write out a Pact file for the interaction when done

  Scenario: Request with invalid path
    When the mock server is started with interaction 1
    And request 1 is made to the mock server with the following changes:
      | path  |
      | /path |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interaction when done
    And the mock server status will be an unexpected "GET" request received error for path "/path"

  Scenario: Request with invalid method
    When the mock server is started with interaction 1
    And request 1 is made to the mock server with the following changes:
      | method  |
      | HEAD    |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server will NOT write out a Pact file for the interaction when done
    And the mock server status will be an unexpected "HEAD" request received error for path "/basic"

  Scenario: Request with headers
    When the mock server is started with interaction 3
    And request 3 is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request will contain the header "X-TEST" with value "Compatibility"

  Scenario: Request with invalid headers
    When the mock server is started with interaction 3
    And request 3 is made to the mock server with the following changes:
      | headers              |
      | 'X-OTHER: Something' |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server status will be mismatches
    And the mismatches will contain a "header" mismatch with error "Expected a header 'X-TEST' but was missing"
    And the mock server will NOT write out a Pact file for the interaction when done

  Scenario: Request with body
    When the mock server is started with interaction 4
    And request 4 is made to the mock server
    Then a 200 success response is returned
    When the pact test is done
    Then the mock server status will be OK
    And the mock server will write out a Pact file for the interaction when done
    And the pact file will contain {1} interaction
    And the {first} interaction request will be for a "PUT"
    And the {first} interaction request content type will be "application/json"
    And the {first} interaction request will contain the "basic.json" document

  Scenario: Request with invalid body
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | body                           |
      | JSON: {"one": "a", "two": "c"} |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server status will be mismatches
    And the mismatches will contain a "body" mismatch with path "$.two" with error "Expected 'b' (String) but received 'c' (String)"
    And the mock server will NOT write out a Pact file for the interaction when done

  Scenario: Request with the incorrect type of body contents
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | body                                                                         |
      | XML: <?xml version="1.0" encoding="UTF-8"?><alligator name="Mary" feet="4"/> |
    Then a 500 error response is returned
    When the pact test is done
    Then the mock server status will NOT be OK
    And the mock server status will be mismatches
    And the mismatches will contain a "body-content-type" mismatch with error "Expected a body of 'application/json' but the actual content type was 'application/xml'"
    And the mock server will NOT write out a Pact file for the interaction when done

  Scenario: Request with plain text body (positive case)
    When the mock server is started with interaction 5
    And request 5 is made to the mock server
    Then a 200 success response is returned

  Scenario: Request with plain text body (negative case)
    When the mock server is started with interaction 5
    And request 5 is made to the mock server with the following changes:
      | body        |
      | Hello Mars! |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected body 'Hello World!' to match 'Hello Mars!' using equality but did not match"

  Scenario: Request with JSON body (positive case)
    When the mock server is started with interaction 4
    And request 4 is made to the mock server
    Then a 200 success response is returned

  Scenario: Request with JSON body (negative case)
    When the mock server is started with interaction 4
    And request 4 is made to the mock server with the following changes:
      | body               |
      | JSON: {"one": "a"} |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected a Map with keys [one, two] but received one with keys [one]"

  Scenario: Request with XML body (positive case)
    When the mock server is started with interaction 6
    And request 6 is made to the mock server
    Then a 200 success response is returned

  Scenario: Request with XML body (negative case)
    When the mock server is started with interaction 6
    And request 6 is made to the mock server with the following changes:
      | body                                                                      |
      | XML: <?xml version="1.0" encoding="UTF-8" ?><values><one>A</one></values> |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected child <two/> but was missing"

  Scenario: Request with a binary body (positive case)
    When the mock server is started with interaction 7
    And request 7 is made to the mock server
    Then a 200 success response is returned

  Scenario: Request with a binary body (negative case)
    When the mock server is started with interaction 7
    And request 7 is made to the mock server with the following changes:
      | body             |
      | file: spider.jpg |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Actual body [image/jpeg, 30922 bytes, starting with ffd8ffe000104a46494600010101004800480000ffe100ae4578696600004949] is not equal to the expected body [image/jpeg, 28058 bytes, starting with ffd8ffe000104a46494600010101012c012c0000ffe12db64578696600004949]"

  Scenario: Request with a form post body (positive case)
    When the mock server is started with interaction 8
    And request 8 is made to the mock server
    Then a 200 success response is returned

  Scenario: Request with a form post body (negative case)
    When the mock server is started with interaction 8
    And request 8 is made to the mock server with the following changes:
      | body             |
      | a=1&b=2&c=33&d=4 |
    Then a 500 error response is returned
    And the mismatches will contain a "body" mismatch with error "Expected form post parameter 'c' with value '3' but was '33'"

#    "multipart/form-data" to "au.com.dius.pact.core.matchers.MultipartMessageContentMatcher",
#    "multipart/mixed" to "au.com.dius.pact.core.matchers.MultipartMessageContentMatcher",
