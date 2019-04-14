package au.com.dius.pact.model

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import java.util.jar.JarInputStream

/**
 * Base Pact class
 */
@SuppressWarnings(['AbstractClassWithoutAbstractMethod', 'SpaceAroundMapEntryColon'])
@Slf4j
@ToString
@EqualsAndHashCode(excludes = ['metadata', 'source'])
abstract class BasePact<I extends Interaction> implements Pact<I> {
  static final Map DEFAULT_METADATA = Collections.unmodifiableMap([
    'pactSpecification': [version: '3.0.0'],
    'pact-jvm'         : [version: lookupVersion()]
  ])

  Consumer consumer
  Provider provider
  Map metadata = DEFAULT_METADATA
  PactSource source

  protected BasePact(Provider provider, Consumer consumer, Map metadata) {
    this.consumer = consumer
    this.provider = provider
    this.metadata = metadata
  }

  static String lookupVersion() {
    def url = BasePact.protectionDomain?.codeSource?.location
    if (url != null) {
      def openStream = url.openStream()
      try {
        def jarStream = new JarInputStream(openStream)
        jarStream.manifest?.mainAttributes?.getValue('Implementation-Version') ?: ''
      } catch (e) {
        log.warn('Could not load pact-jvm manifest', e)
        ''
      } finally {
        openStream.close()
      }
    } else {
      ''
    }
  }

  static Map objectToMap(def object) {
    if (object?.respondsTo('toMap')) {
      object.toMap()
    } else {
      convertToMap(object)
    }
  }

  static Map convertToMap(def object) {
    if (object == null) {
      null
    } else {
      object.properties.findAll { it.key != 'class' }.collectEntries { k, v ->
        if (v instanceof Map) {
          [k, convertToMap(v)]
        } else if (v instanceof Collection) {
          [k, v.collect { convertToMap(v) } ]
        } else {
          [k, v]
        }
      }
    }
  }

  @SuppressWarnings(['ConfusingMethodName'])
  static Map metaData(PactSpecVersion pactSpecVersion) {
    def pactJvmMetadata = [version: lookupVersion()]
    def updatedToggles = FeatureToggles.updatedToggles()
    if (!updatedToggles.isEmpty()) {
      pactJvmMetadata.features = updatedToggles
    }
    [
      'pactSpecification': [version: pactSpecVersion >= PactSpecVersion.V3 ? '3.0.0' : '2.0.0'],
      'pact-jvm': pactJvmMetadata
    ]
  }

  @CompileStatic
  void write(String pactDir, PactSpecVersion pactSpecVersion) {
    PactWriter.writePact(fileForPact(pactDir), this, pactSpecVersion)
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

  File fileForPact(String pactDir) {
    new File(pactDir, "${consumer.name}-${provider.name}.json")
  }

  boolean compatibleTo(Pact other) {
    provider == other.provider && this.class.isAssignableFrom(other.class)
  }
}
