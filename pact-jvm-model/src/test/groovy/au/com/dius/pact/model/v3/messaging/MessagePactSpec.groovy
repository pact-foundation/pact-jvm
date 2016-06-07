package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import spock.lang.Specification

class MessagePactSpec extends Specification {

  def 'fails to convert the message to a Map if the target spec version is < 3'() {
    when:
    new MessagePact(new Provider(), new Consumer(), []).toMap(PactSpecVersion.V1)

    then:
    thrown(InvalidPactException)
  }

}
