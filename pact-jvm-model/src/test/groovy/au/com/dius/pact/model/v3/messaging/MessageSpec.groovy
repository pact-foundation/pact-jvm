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
}
