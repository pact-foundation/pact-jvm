package au.com.dius.pact.model.v3.messaging

import spock.lang.Specification

class MessageSpec extends Specification {

    def 'contentsAsBytes handles contents in string form'() {
        when:
        Message message = new Message(contents: '1 2 3 4')

        then:
        message.contentsAsBytes() == '1 2 3 4'.bytes
    }

    def 'contentsAsBytes handles no contents'() {
        when:
        Message message = new Message(contents: null)

        then:
        message.contentsAsBytes() == []
    }

}
