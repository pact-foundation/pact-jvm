package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Class to represent missing, empty, null and present bodies
 */
@Canonical
class OptionalBody {
  static enum State {
    MISSING, EMPTY, NULL, PRESENT
  }
  State state
  String value

  static OptionalBody missing() {
    new OptionalBody(State.MISSING)
  }

  static OptionalBody empty() {
    new OptionalBody(State.EMPTY, '')
  }

  static OptionalBody nullBody() {
    new OptionalBody(State.NULL)
  }

  static OptionalBody body(String body) {
    if (body == null) {
      nullBody()
    } else if (body.empty) {
      empty()
    } else {
      new OptionalBody(State.PRESENT, body)
    }
  }

  boolean isMissing() {
    state == State.MISSING
  }

  boolean isEmpty() {
    state == State.EMPTY
  }

  boolean isNull() {
    state == State.NULL
  }

  boolean isPresent() {
    state == state.PRESENT
  }

  String orElse(String defaultValue) {
    if (state == State.EMPTY || state == State.PRESENT) {
      value
    } else {
      defaultValue
    }
  }

}
