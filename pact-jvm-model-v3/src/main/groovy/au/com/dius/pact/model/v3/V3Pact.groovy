package au.com.dius.pact.model.v3

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

import java.util.jar.JarInputStream

/**
 * Pact implementing V3 version of the spec
 */
@Slf4j
@ToString
@EqualsAndHashCode(excludes = ['metadata'])
@TupleConstructor
abstract class V3Pact {
    Consumer consumer
    Provider provider
    Map metadata = ['pact-specification': ['version': '3.0.0'], 'pact-jvm': ['version': lookupVersion()]]

    protected static String lookupVersion() {
        V3Pact.protectionDomain.codeSource.location?.withInputStream { stream ->
            try {
                def jarStream = new JarInputStream(stream)
                jarStream.manifest?.mainAttributes?.getValue('Implementation-Version')
            } catch (e) {
                log.warn('Could not load pact-jvm manifest', e)
            }
        } ?: ''
    }

    static Map toMap(object) {
        object?.properties?.findAll { it.key != 'class' }?.collectEntries {
            it.value == null || it.value instanceof Serializable ? [it.key, it.value] : [it.key, toMap(it.value)]
        }
    }

    void write(String pactDir) {
        def pactFile = fileForPact(pactDir)
        pactFile.parentFile.mkdirs()

        def jsonMap = toMap()
        if (pactFile.exists()) {
            jsonMap = mergePacts(jsonMap, pactFile)
        }

        def json = JsonOutput.toJson(jsonMap)
        pactFile.withWriter { writer ->
            writer.print JsonOutput.prettyPrint(json)
        }
    }

    Map mergePacts(Map pact, File pactFile) {
        Map newPact = [:] + pact
        def json = new JsonSlurper().parse(pactFile)
        newPact.messages = (newPact.messages + json.messages).unique { it.description }
        newPact
    }

    protected File fileForPact(String pactDir) {
        new File(pactDir, "${consumer.name()}-${provider.name()}.json")
    }

    abstract Map toMap()
}
