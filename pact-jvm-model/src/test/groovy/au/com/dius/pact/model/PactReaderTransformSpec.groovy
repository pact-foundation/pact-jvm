package au.com.dius.pact.model

import au.com.dius.pact.support.Json
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactReaderTransformSpec extends Specification {
  private provider
  private consumer
  private jsonMap
  private request
  private Map<String, Serializable> response

  def setup() {
    provider = [
      name: 'Alice Service'
    ]
    consumer = [
      name: 'Consumer'
    ]
    request = [
      method: 'GET',
      path: '/mallory',
      query: 'name=ron&status=good',
      body: [
        'id': '123', 'method': 'create'
      ]
    ]
    response = [
      status: 200,
      headers: [
        'Content-Type': 'text/html'
      ],
      body: '"That is some good Mallory."'
    ]

    jsonMap = new JsonSlurper().parse(this.class.getResourceAsStream('/pact.json'))
  }

  def 'only transforms legacy fields'() {
    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result) == '''{
      |  "provider": {
      |    "name": "Alice Service"
      |  },
      |  "consumer": {
      |    "name": "Consumer"
      |  },
      |  "interactions": [
      |    {
      |      "description": "a retrieve Mallory request",
      |      "request": {
      |        "method": "GET",
      |        "path": "/mallory",
      |        "query": "name=ron&status=good",
      |        "body": {
      |          "id": "123",
      |          "method": "create"
      |        }
      |      },
      |      "response": {
      |        "status": 200,
      |        "headers": {
      |          "Content-Type": "text/html"
      |        },
      |        "body": "\\"That is some good Mallory.\\""
      |      }
      |    }
      |  ]
      |}'''.stripMargin()
  }

  def 'converts provider state to camel case'() {
    given:
    jsonMap.interactions[0].provider_state = 'provider state'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result) == '''{
      |  "provider": {
      |    "name": "Alice Service"
      |  },
      |  "consumer": {
      |    "name": "Consumer"
      |  },
      |  "interactions": [
      |    {
      |      "description": "a retrieve Mallory request",
      |      "request": {
      |        "method": "GET",
      |        "path": "/mallory",
      |        "query": "name=ron&status=good",
      |        "body": {
      |          "id": "123",
      |          "method": "create"
      |        }
      |      },
      |      "response": {
      |        "status": 200,
      |        "headers": {
      |          "Content-Type": "text/html"
      |        },
      |        "body": "\\"That is some good Mallory.\\""
      |      },
      |      "providerState": "provider state"
      |    }
      |  ]
      |}'''.stripMargin()
  }

  def 'handles both a snake and camel case provider state'() {
    given:
    jsonMap.interactions[0].provider_state = 'provider state'
    jsonMap.interactions[0].providerState = 'provider state 2'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result) == '''{
      |  "provider": {
      |    "name": "Alice Service"
      |  },
      |  "consumer": {
      |    "name": "Consumer"
      |  },
      |  "interactions": [
      |    {
      |      "description": "a retrieve Mallory request",
      |      "request": {
      |        "method": "GET",
      |        "path": "/mallory",
      |        "query": "name=ron&status=good",
      |        "body": {
      |          "id": "123",
      |          "method": "create"
      |        }
      |      },
      |      "response": {
      |        "status": 200,
      |        "headers": {
      |          "Content-Type": "text/html"
      |        },
      |        "body": "\\"That is some good Mallory.\\""
      |      },
      |      "providerState": "provider state 2"
      |    }
      |  ]
      |}'''.stripMargin()
  }

  def 'converts request and response matching rules'() {
    given:
    jsonMap.interactions[0].request.requestMatchingRules = [body: ['$': [['match': 'type']]]]
    jsonMap.interactions[0].response.responseMatchingRules = [body: ['$': [['match': 'type']]]]

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result) == '''{
      |  "provider": {
      |    "name": "Alice Service"
      |  },
      |  "consumer": {
      |    "name": "Consumer"
      |  },
      |  "interactions": [
      |    {
      |      "description": "a retrieve Mallory request",
      |      "request": {
      |        "method": "GET",
      |        "path": "/mallory",
      |        "query": "name=ron&status=good",
      |        "body": {
      |          "id": "123",
      |          "method": "create"
      |        },
      |        "matchingRules": {
      |          "body": {
      |            "$": [
      |              {
      |                "match": "type"
      |              }
      |            ]
      |          }
      |        }
      |      },
      |      "response": {
      |        "status": 200,
      |        "headers": {
      |          "Content-Type": "text/html"
      |        },
      |        "body": "\\"That is some good Mallory.\\"",
      |        "matchingRules": {
      |          "body": {
      |            "$": [
      |              {
      |                "match": "type"
      |              }
      |            ]
      |          }
      |        }
      |      }
      |    }
      |  ]
      |}'''.stripMargin()
  }

  def 'converts the http methods to upper case'() {
    given:
    jsonMap.interactions[0].request.method = 'post'

    when:
    def result = PactReader.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result) == '''{
      |  "provider": {
      |    "name": "Alice Service"
      |  },
      |  "consumer": {
      |    "name": "Consumer"
      |  },
      |  "interactions": [
      |    {
      |      "description": "a retrieve Mallory request",
      |      "request": {
      |        "method": "POST",
      |        "path": "/mallory",
      |        "query": "name=ron&status=good",
      |        "body": {
      |          "id": "123",
      |          "method": "create"
      |        }
      |      },
      |      "response": {
      |        "status": 200,
      |        "headers": {
      |          "Content-Type": "text/html"
      |        },
      |        "body": "\\"That is some good Mallory.\\""
      |      }
      |    }
      |  ]
      |}'''.stripMargin()
  }

}
