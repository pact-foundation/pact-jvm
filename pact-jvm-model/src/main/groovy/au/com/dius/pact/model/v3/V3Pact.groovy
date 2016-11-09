package au.com.dius.pact.model.v3

import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.InvalidPactException
import au.com.dius.pact.model.PactMerge
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import groovy.json.JsonOutput
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

/**
 * Pact implementing V3 version of the spec
 */
@Slf4j
@ToString(includeSuperProperties = true)
@EqualsAndHashCode(callSuper = true)
abstract class V3Pact extends BasePact {

  protected V3Pact(Provider provider, Consumer consumer, Map metadata) {
    super(provider, consumer, metadata)
  }

  void write(String pactDir) {
    def pactFile = fileForPact(pactDir)

    if (pactFile.exists()) {
      def existingPact = PactReader.loadPact(pactFile)
      def result = PactMerge.merge(existingPact, this)
      if (!result.ok) {
        throw new InvalidPactException(result.message)
      }
    } else {
      pactFile.parentFile.mkdirs()
    }

    def jsonMap = toMap(PactSpecVersion.V3)
    jsonMap.metadata = jsonMap.metadata ? jsonMap.metadata + DEFAULT_METADATA : DEFAULT_METADATA
    def json = JsonOutput.toJson(jsonMap)
    pactFile.withWriter { writer ->
      writer.print JsonOutput.prettyPrint(json)
    }
  }

  protected File fileForPact(String pactDir) {
    new File(pactDir, "${consumer.name}-${provider.name}.json")
  }

}
