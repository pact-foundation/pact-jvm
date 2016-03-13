package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

class OptionalBodySpec extends Specification {

  @Unroll
  def 'returns the appropriate state for missing'() {
    expect:
    body.missing == value

    where:
    body | value
    OptionalBody.missing() | true
    OptionalBody.empty() | false
    OptionalBody.nullBody() | false
    OptionalBody.body('a') | false
  }

  @Unroll
  def 'returns the appropriate state for empty'() {
    expect:
    body.empty == value

    where:
    body | value
    OptionalBody.missing() | false
    OptionalBody.empty() | true
    OptionalBody.body('') | true
    OptionalBody.nullBody() | false
    OptionalBody.body('a') | false
  }

  @Unroll
  def 'returns the appropriate state for nullBody'() {
    expect:
    body.null == value

    where:
    body | value
    OptionalBody.missing() | false
    OptionalBody.empty() | false
    OptionalBody.nullBody() | true
    OptionalBody.body(null) | true
    OptionalBody.body('a') | false
  }

  @Unroll
  def 'returns the appropriate state for present'() {
    expect:
    body.present == value

    where:
    body | value
    OptionalBody.missing() | false
    OptionalBody.empty() | false
    OptionalBody.nullBody() | false
    OptionalBody.body('') | false
    OptionalBody.body(null) | false
    OptionalBody.body('a') | true
  }

  @Unroll
  def 'returns the appropriate value for orElse'() {
    expect:
    body.orElse('default') == value

    where:
    body | value
    OptionalBody.missing() | 'default'
    OptionalBody.empty() | ''
    OptionalBody.nullBody() | 'default'
    OptionalBody.body('') | ''
    OptionalBody.body(null) | 'default'
    OptionalBody.body('a') | 'a'
  }

}
