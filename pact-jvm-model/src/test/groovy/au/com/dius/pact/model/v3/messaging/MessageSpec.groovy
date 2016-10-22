package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.OptionalBody
import spock.lang.Specification

class MessageSpec extends Specification {

    def 'contentsAsBytes handles contents in string form'() {
        when:
        Message message = new Message(contents: OptionalBody.body('1 2 3 4'))

        then:
        message.contentsAsBytes() == '1 2 3 4'.bytes
    }

    def 'contentsAsBytes handles no contents'() {
        when:
        Message message = new Message(contents: OptionalBody.missing())

        then:
        message.contentsAsBytes() == []
    }

  def 'unique key test'() {
    expect:
    interaction1.uniqueKey() == interaction1.uniqueKey()
    interaction1.uniqueKey() == interaction2.uniqueKey()
    interaction1.uniqueKey() != interaction3.uniqueKey()
    interaction1.uniqueKey() != interaction4.uniqueKey()
    interaction1.uniqueKey() != interaction5.uniqueKey()
    interaction3.uniqueKey() != interaction4.uniqueKey()
    interaction3.uniqueKey() != interaction5.uniqueKey()
    interaction4.uniqueKey() != interaction5.uniqueKey()

    where:
    interaction1 = new Message('description 1+2')
    interaction2 = new Message('description 1+2')
    interaction3 = new Message('description 1+2', 'state 3')
    interaction4 = new Message('description 4')
    interaction5 = new Message('description 4', 'state 5')
  }

  def 'messages do not conflict if they have different states'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', 'state')
    message2 = new Message('description', 'state 2')
  }

  def 'messages do not conflict if they have different descriptions'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', 'state')
    message2 = new Message('description 2', 'state')
  }

  def 'messages do not conflict if they are identical'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', 'state', OptionalBody.body('1 2 3'))
    message2 = new Message('description', 'state', OptionalBody.body('1 2 3'))
  }

  def 'messages do not conflict if they have the same state and description but different bodies'() {
    expect:
    message1.conflictsWith(message2)

    where:
    message1 = new Message('description', 'state', OptionalBody.body('1 2 3'))
    message2 = new Message('description', 'state', OptionalBody.body('1 2 3 4'))
  }

}
