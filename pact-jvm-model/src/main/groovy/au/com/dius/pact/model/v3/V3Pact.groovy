package au.com.dius.pact.model.v3

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.Provider
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import java.util.jar.JarInputStream

/**
 * Pact implementing V3 version of the spec
 */
@Slf4j
@ToString
@EqualsAndHashCode(excludes = ['metadata'])
abstract class V3Pact extends BasePact {
    private static final Map DEFAULT_METADATA = [
        'pact-specification': ['version': '3.0.0'],
        'pact-jvm': ['version': lookupVersion()]
    ]

    Consumer consumer
    Provider provider
    Map metadata = DEFAULT_METADATA

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

        def jsonMap = toMap()
        if (pactFile.exists()) {
          jsonMap = mergePacts(jsonMap, pactFile)
        } else {
          pactFile.parentFile.mkdirs()
        }

        jsonMap.metadata = jsonMap.metadata ? jsonMap.metadata + DEFAULT_METADATA : DEFAULT_METADATA
        def json = JsonOutput.toJson(jsonMap)
        pactFile.withWriter { writer ->
            writer.print JsonOutput.prettyPrint(json)
        }
    }

    Map mergePacts(Map pact, File pactFile) {
      Map newPact = [:] + pact
      def json = new JsonSlurper().parse(pactFile)

      def pactSpec = 'pact-specification'
      def version = json?.metadata?.get(pactSpec)?.version
      def pactVersion = pact.metadata?.get(pactSpec)?.version
      if (version && version != pactVersion) {
        throw new InvalidPactException("Could not merge pact into '$pactFile': pact specification version is " +
          "$pactVersion, while the file is version $version")
      }

      if (json.interactions != null) {
        throw new InvalidPactException("Could not merge pact into '$pactFile': file is not a message pact " +
          '(it contains request/response interactions)')
      }

      newPact.messages = (newPact.messages + json.messages).unique { it.description }
      newPact
    }

    protected File fileForPact(String pactDir) {
        new File(pactDir, "${consumer.name}-${provider.name}.json")
    }

    abstract Map toMap()

    V3Pact fromMap(Map map) {
        consumer = new Consumer(map.consumer?.name ?: 'consumer')
        provider = new Provider(map.provider?.name ?: 'provider')
        metadata = map.metadata
        this
    }
}
