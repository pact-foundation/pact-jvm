package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.OptionalBody
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

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

  @Ignore('Message conflicts do not work with generated values')
  def 'messages do conflict if they have the same state and description but different bodies'() {
    expect:
    message1.conflictsWith(message2)

    where:
    message1 = new Message('description', 'state', OptionalBody.body('1 2 3'), null, [contentType: 'text/plain'])
    message2 = new Message('description', 'state', OptionalBody.body('1 2 3 4'), null, [contentType: 'text/plain'])
  }

  @Unroll
  def 'message to map handles message content correctly'() {
    expect:
    message.toMap().contents == contents

    where:

    body                               | contentType                | contents
    '{"A": "Value A", "B": "Value B"}' | 'application/json'         | [A: 'Value A', B: 'Value B']
    '1 2 3 4'                          | 'text/plain'               | '1 2 3 4'
    new String([1, 2, 3, 4] as byte[]) | 'application/octet-stream' | 'AQIDBA=='

    message = new Message(contents: OptionalBody.body(body), metaData: [contentType: contentType])
  }

}
