package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import groovy.json.JsonOutput
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

import java.util.jar.JarInputStream

/**
 * Pact for a sequences of messages
 */
@Canonical
@Slf4j
class MessagePact {
    Consumer consumer
    Provider provider
    List messages = []
    Map metadata = ['pact-specification': ['version': '3.0'], 'pact-jvm': ['version': lookupVersion()]]

    private static String lookupVersion() {
        MessagePact.protectionDomain.codeSource.location?.withInputStream { stream ->
            try {
                def jarStream = new JarInputStream(stream)
                jarStream.manifest?.mainAttributes?.getValue('Implementation-Version')
            } catch (e) {
                log.warn('Could not load pact-jvm manifest', e)
            }
        } ?: ''
    }

    void write(String pactDir) {
        def pactFile = new File(pactDir, "${consumer.name()}-${provider.name()}.json")
        pactFile.parentFile.mkdirs()
        pactFile.withWriter { writer ->
            writer.print JsonOutput.prettyPrint(JsonOutput.toJson(toMap()))
        }
    }

    Map toMap() {
        [
            consumer: [name: consumer.name()],
            provider: [name: provider.name()],
            messages: messages*.toMap(),
            metadata: metadata
        ]
    }

    static Map toMap(object) {
        object?.properties?.findAll { it.key != 'class' }?.collectEntries {
            it.value == null || it.value instanceof Serializable ? [it.key, it.value] : [it.key, toMap(it.value)]
        }
    }
}
