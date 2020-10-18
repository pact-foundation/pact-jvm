package au.com.dius.pact.server

import spock.lang.Specification

class PublishSpec extends Specification {

    def 'invalid broker url in config will not set broker'() {
        given:
        def config = new Config(80, '0.0.0.0', false, 100, 200, false, 3, '', '', 0, 'invalid', 'abc#3')
//        def pact = PublishSpec.getResourceAsStream('/create-pact.json').text

        when:
        def result = Publish.getBrokerUrlFromConfig(config)

        then:
        !result.defined
    }

    def 'valid broker url will set broker'() {
        given:
        def config = new Config(80,
                '0.0.0.0',
                false,
                100,
                200,
                false,
                3,
                '',
                '',
                0,
                'https://valid.broker.com',
                'abc#3'
        )

        when:
        def result = Publish.getBrokerUrlFromConfig(config)

        then:
        result.defined
    }
}
