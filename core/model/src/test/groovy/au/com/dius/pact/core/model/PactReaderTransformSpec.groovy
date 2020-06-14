package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class PactReaderTransformSpec extends Specification {
  private provider
  private consumer
  private JsonValue.Object jsonMap
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
      JsonParser.INSTANCE.parseReader(it).asObject()
    }
  }

  def 'only transforms legacy fields'() {
    when:
    def result = DefaultPactReader.INSTANCE.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result.toGson()) == '''{
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
    jsonMap.get('interactions').asArray().get(0).asObject().add('provider_state',
      new JsonValue.StringValue('provider state'.chars))

    when:
    def result = DefaultPactReader.INSTANCE.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result.toGson()) == '''{
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
    jsonMap.get('interactions').asArray().get(0).asObject().add('provider_state',
      new JsonValue.StringValue('provider state'.chars))
    jsonMap.get('interactions').asArray().get(0).asObject().add('providerState',
      new JsonValue.StringValue('provider state 2'.chars))

    when:
    def result = DefaultPactReader.INSTANCE.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result.toGson()) == '''{
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
    jsonMap.get('interactions').asArray().get(0).asObject().get('request').asObject()
      .add('requestMatchingRules', Json.INSTANCE.toJson([body: ['$': [['match': 'type']]]]))
    jsonMap.get('interactions').asArray().get(0).asObject().get('response').asObject()
      .add('responseMatchingRules', Json.INSTANCE.toJson([body: ['$': [['match': 'type']]]]))

    when:
    def result = DefaultPactReader.INSTANCE.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result.toGson()) == '''{
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
    jsonMap.get('interactions').asArray().get(0).asObject().get('request').asObject()
      .add('method', new JsonValue.StringValue('post'.chars))

    when:
    def result = DefaultPactReader.INSTANCE.transformJson(jsonMap)

    then:
    Json.INSTANCE.gsonPretty.toJson(result.toGson()) == '''{
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
