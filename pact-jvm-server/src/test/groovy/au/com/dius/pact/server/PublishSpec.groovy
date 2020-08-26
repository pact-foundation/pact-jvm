package au.com.dius.pact.server

import au.com.dius.pact.server.Config
import au.com.dius.pact.server.Publish
import spock.lang.Specification

class PublishSpec extends Specification {

    def 'invalid broker url in config will not set broker'() {
        given:
        def config = new Config(80, '0.0.0.0', false, 100, 200, false, 3, '', '', 0, 'invalid', 'abc#3')
//        def pact = PublishSpec.getResourceAsStream('/create-pact.json').text

        when:
        def result = Publish.getBrokerUrlFromConfig(config)

        then:
        !result.isDefined()
    }

    def 'valid broker url will set broker'() {
        given:
        def config = new Config(80, '0.0.0.0', false, 100, 200, false, 3, '', '', 0, 'https://valid.broker.com', 'abc#3')

        when:
        def result = Publish.getBrokerUrlFromConfig(config)

        then:
        result.isDefined()
    }

    def 'successful read on valid file'() {
        given:
        def content = """ {"consumer": "testconsumer", "provider": "testprovider"} """
        def fileName = "test.json"
        def rootDir = System.getProperty("pact.rootDir", "target/pacts")
        def file = new File("${rootDir}/$fileName")
        new File(rootDir).mkdirs()
        file.write(content)

        when:
        def result = Publish.readContract(fileName)

        then:
        content == result

        cleanup:
        new File(System.getProperty("pact.rootDir", "target")).deleteDir()
    }

    def 'unsuccessful read on invalid file'() {
        when:
        Publish.readContract("invalid")

        then:
        thrown(IOException)
    }
}
