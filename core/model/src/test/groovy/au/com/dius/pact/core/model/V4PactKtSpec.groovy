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
}
