package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

import static au.com.dius.pact.core.model.V4PactKt.bodyFromJson

class V4PactKtSpec extends Specification {
  def 'bodyFromJson - when body is empty in the Pact file'() {
    expect:
    bodyFromJson('body', new JsonValue.Object(json), [:]) == body

    where:

    json                                                                   | body
    [:]                                                                    | OptionalBody.missing()
    [body: JsonValue.Null.INSTANCE]                                        | OptionalBody.nullBody()
    [body: new JsonValue.StringValue('')]                                  | OptionalBody.empty()
    [body: new JsonValue.Object([content: new JsonValue.StringValue('')])] | OptionalBody.empty()
  }

  @SuppressWarnings('LineLength')
  def 'bodyFromJson - handling different types of encoding'() {
    expect:
    bodyFromJson('body', new JsonValue.Object([
      body: new JsonValue.Object([
        content: content,
        encoded: encoding,
        contentType: new JsonValue.StringValue(contentType)
      ])
    ]), [:]) == body

    where:

    content                                | encoding                            | contentType        | body
    new JsonValue.Object([:])              | JsonValue.False.INSTANCE            | 'application/json' | OptionalBody.body('{}'.bytes, ContentType.JSON)
    new JsonValue.Object([:])              | JsonValue.False.INSTANCE            | ''                 | OptionalBody.body('{}'.bytes, ContentType.JSON)
    new JsonValue.StringValue('ABC')       | JsonValue.False.INSTANCE            | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
    new JsonValue.StringValue('\"ABC\"')   | JsonValue.False.INSTANCE            | 'application/json' | OptionalBody.body('"\\"ABC\\""'.bytes, ContentType.JSON)
    new JsonValue.StringValue('\"ABC\"')   | new JsonValue.StringValue('json')   | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
    new JsonValue.StringValue('\"ABC\"')   | new JsonValue.StringValue('JSON')   | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
    new JsonValue.StringValue('IkFCQyI=')  | JsonValue.True.INSTANCE             | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
    new JsonValue.StringValue('IkFCQyI=')  | new JsonValue.StringValue('base64') | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
    new JsonValue.StringValue('IkFCQyI=')  | new JsonValue.StringValue('BASE64') | 'application/json' | OptionalBody.body('"ABC"'.bytes, ContentType.JSON)
  }
}
