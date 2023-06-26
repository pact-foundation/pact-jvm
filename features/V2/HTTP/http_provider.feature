@provider
Feature: Basic HTTP provider
  Supports verifying a basic HTTP provider

  Background:
    Given the following HTTP interactions have been defined:
      | method | path          | query   | headers                 | body       | response | response content | response body |
      | GET    | /basic        |         |                         |            | 200      | application/json | basic.json    |
      | GET    | /with_params  | a=1&b=2 |                         |            | 200      |                  |               |
      | GET    | /with_headers |         | 'X-TEST: Compatibility' |            | 200      |                  |               |
      | PUT    | /basic        |         |                         | basic.json | 200      |                  |               |

