package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import spock.lang.Specification

class PactReaderTransformSpec extends Specification {
  private provider
  private consumer
  private JsonObject jsonMap
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

    jsonMap = this.class.getResourceAsStream('/pact.json').withReader {
      new JsonParser().parse(it).asJsonObject
    }
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
      |        "query": "name\\u003dron\\u0026status\\u003dgood",
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
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.addProperty('provider_state', 'provider state')

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
      |        "query": "name\\u003dron\\u0026status\\u003dgood",
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
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.addProperty('provider_state', 'provider state')
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.addProperty('providerState', 'provider state 2')

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
      |        "query": "name\\u003dron\\u0026status\\u003dgood",
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
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.get('request').asJsonObject
      .add('requestMatchingRules', Json.INSTANCE.toJson([body: ['$': [['match': 'type']]]]))
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.get('response').asJsonObject
      .add('responseMatchingRules', Json.INSTANCE.toJson([body: ['$': [['match': 'type']]]]))

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
      |        "query": "name\\u003dron\\u0026status\\u003dgood",
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
    jsonMap.get('interactions').asJsonArray.get(0).asJsonObject.get('request').asJsonObject
      .addProperty('method', 'post')

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
      |        "query": "name\\u003dron\\u0026status\\u003dgood",
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
